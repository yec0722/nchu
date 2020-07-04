package com.atguigu.gmall1213.gateway.filter;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall1213.common.result.Result;
import com.atguigu.gmall1213.common.result.ResultCodeEnum;
import com.atguigu.gmall1213.common.util.IpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class AuthGlobalFilter implements GlobalFilter {
    private AntPathMatcher antPathMatcher = new AntPathMatcher();

    @Autowired
    private RedisTemplate redisTemplate;

    @Value("${authUrls.url}")
    private String authUrlsUrl;

    //过滤器
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        //获取用户在浏览器中输入的访问路径  比如 http://localhost:8206/api/product/inner/getSkuInfo/30
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        //request.getURI():  http://item.gmall.com/api/product/auth/hello
        // System.out.println("request.getURI()" + request.getURI());
        //request.getURI().getPath(): /api/product/auth/hello
        //System.out.println("request.getURI().getPath()" + request.getURI().getPath());
        //判断用户输入的访问路径是否包含inner  如果有，说明是内部接口，不允许在浏览器直接访问
        if (antPathMatcher.match("/**/inner/**", path)) {
            //返回一个响应信息，说明没有访问权限
            ServerHttpResponse response = exchange.getResponse();
            //out()  提示信息
            return out(response, ResultCodeEnum.PERMISSION);
        }
        /*
          获取用户登录信息，用户登录成功之后，在缓存中存储了一个userId,如果我们能从缓存中获取到这个userId，
          那么就证明用户已经登录成功了，反之，说明用户没有登录
          用户在登录的时候将token放到了两个地方，一个是cookie中，一个是header中，所以要用request得到cookie或header
         */
        String userId = getUserId(request);
        //获取临时用户id
        String userTempId = getUserTempId(request);
        //判断token是否被盗用   设置ip地址防token被盗用
        if ("-1".equals(userId)) {
            //返回一个响应信息，说明没有访问权限
            ServerHttpResponse response = exchange.getResponse();
            //out()  提示信息
            return out(response, ResultCodeEnum.PERMISSION);
        }

        //用户登录认证
        if (antPathMatcher.match("/api/**/auth/**", path)) {
            //如果用户访问的url中包含/api/**/auth/**这个路径，则必须登录
            if (StringUtils.isEmpty(userId)) {
                //返回一个响应信息，说明未登录
                ServerHttpResponse response = exchange.getResponse();
                //out()  提示信息
                return out(response, ResultCodeEnum.LOGIN_AUTH);
            }
        }

        //验证用户访问web-all时是否带有黑名单中的控制器
        /*authUrls:
              url: trade.html,myOrder.html,list.html # 配置黑名单  即用户访问上述这些控制器时，需要登录。
         */
        for (String authUrl : authUrlsUrl.split(",")) {
            //访问路径中包含上述这些控制器，并且没有登录
            if (path.indexOf(authUrl) != -1 && StringUtils.isEmpty(userId)) {
                //返回一个响应信息，说明未登录
                ServerHttpResponse response = exchange.getResponse();
                //返回一个响应的状态码，重定向获取资源
                response.setStatusCode(HttpStatus.SEE_OTHER);
                //访问登录页面
                response.getHeaders().set(HttpHeaders.LOCATION, "http://www.gmall.com/login.html?originUrl=" + request.getURI());
                //设置返回
                return response.setComplete();
            }
        }

        //用户在访问任何一个微服务的时候，都必须要走网关，在网关中获取到了userId,就可以将userId传递给任何一个微服务
        // 传递用户Id，临时用户Id 到各个微服务！
        if (!StringUtils.isEmpty(userId) || !StringUtils.isEmpty(userTempId)) {
            if (!StringUtils.isEmpty(userId)) {
                // 将用户Id 存储在请求头中
                request.mutate().header("userId", userId).build();
            }
            if (!StringUtils.isEmpty(userTempId)) {
                // 将临时用户Id 存储在请求头中
                request.mutate().header("userTempId", userTempId).build();
            }
            // 固定写法
            return chain.filter(exchange.mutate().request(request).build());
        }
        //else情况下，传过来一个空的exchange
        return chain.filter(exchange);
    }

    //获取用户id
    private String getUserId(ServerHttpRequest request) {
        String token = "";
        //先拿token
        List<String> list = request.getHeaders().get("token");
        if (null != list) {
            //因为key只有一个，所以对应的value数据也只有一条
            token = list.get(0);
        } else {
            //没在header中，就在cookie中 因为key只有一个，所以对应的value数据也只有一条
            MultiValueMap<String, HttpCookie> cookies = request.getCookies();
            //   return $.cookie('token', token, {domain: 'gmall.com', expires: 7, path: '/'})
            HttpCookie cookie = cookies.getFirst("token");
            if (null != cookie) {
                //做个转换   因为token要经过url进行传送
                token = URLDecoder.decode(cookie.getValue());
            }
        }
        if (!StringUtils.isEmpty(token)) {
            //组成userKey
            String userKey = "user:login:" + token;
            //从缓存中获取userId
            String userJson = (String) redisTemplate.opsForValue().get(userKey);
            JSONObject jsonObject = JSONObject.parseObject(userJson);
            //获取当时登录时的ip地址    它是放在缓存中的
            String ip = jsonObject.getString("ip");
            //获取现在使用的电脑的Ip地址
            String curIp = IpUtil.getGatwayIpAddress(request);
            if (ip.equals(curIp)) {
                //token未被盗用
                return jsonObject.getString("userId");
            }
            //IP地址不一致
            return "-1";
        }
        return null;
    }

    // 获取临时用户Id,添加购物车时，临时用户Id 已经存在cookie 中！ 同时也可能存在header 中
    private String getUserTempId(ServerHttpRequest request) {
        String userTempId = "";
        // 从header 中获取
        List<String> list = request.getHeaders().get("userTempId");
        if (null != list) {
            // 集合中的数据是如何存储，集合中只有一个数据因为key 是同一个
            userTempId = list.get(0);
        } else {
            // 从cookie 中获取
            MultiValueMap<String, HttpCookie> cookies = request.getCookies();
            HttpCookie cookie = cookies.getFirst("userTempId");
            if (null != cookie) {
                // 因为token 要经过url进行传送
                userTempId = URLDecoder.decode(cookie.getValue());
            }
        }
        return userTempId;
    }

    //提示信息方法
    private Mono<Void> out(ServerHttpResponse response, ResultCodeEnum permission) {
        //返回用户的权限通知提示
        Result<Object> result = Result.build(null, permission);
        //将result变成一个字节数组
        byte[] bytes = JSONObject.toJSONString(result).getBytes(StandardCharsets.UTF_8);
        DataBuffer wrap = response.bufferFactory().wrap(bytes);
        //显示到页面 给用户提示 "Content-Type"  内容, "application/json;charset=UTF-8" 格式
        response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");
        return response.writeWith(Mono.just(wrap));
    }
}
