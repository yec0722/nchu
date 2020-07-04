package com.atguigu.gmall1213.user.controller;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall1213.common.constant.RedisConst;
import com.atguigu.gmall1213.common.result.Result;
import com.atguigu.gmall1213.common.util.IpUtil;
import com.atguigu.gmall1213.model.user.UserInfo;
import com.atguigu.gmall1213.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/user/passport")
public class PassportController {
    @Autowired
    private UserService userService;
    @Autowired
    private RedisTemplate redisTemplate;

    //用户登录
    @PostMapping("/login")
    //接收从前端传过来的json字符串
    public Result log(@RequestBody UserInfo userInfo, HttpServletRequest request) {
        UserInfo info = userService.login(userInfo);
        if (null != info) {
            //如果能查到用户信息
            //登录成功之后，会产生一个token
            String token = UUID.randomUUID().toString();
            //页面中有 auth.setToken(response.data.data.token)  这句代码是将token放入了cookie中
            HashMap<String, Object> map = new HashMap<>();
            map.put("token", token);
            //登录成功之后，还需在页面上方显示提个昵称
            map.put("nickName", info.getNickName());
            //如果登录成功，还要将用户信息放到缓存中
            //将此时用户的id以及用户登录的ip地址也放入缓存中  防止异地登录
            JSONObject jsonObject = new JSONObject();
            //jsonObject  本质是一个方法较多的map
            jsonObject.put("userId", info.getId().toString());
            jsonObject.put("ip", IpUtil.getIpAddress(request));
            //将数据放入缓存
            //定义key
            String userKey = RedisConst.USER_LOGIN_KEY_PREFIX + token;
            redisTemplate.opsForValue().set(userKey, jsonObject.toJSONString(), RedisConst.USERKEY_TIMEOUT, TimeUnit.SECONDS);
            return Result.ok(map);
        }
        return Result.fail().message("用户名密码不匹配！");
    }

    //退出登录
    @GetMapping("/logout")
    public Result logout(HttpServletRequest request) {
        //退出登录       删除缓存中的userKey即可
        //token 跟用户缓存key 有直接的关系，在登录的时候，将token 放入了cookie！但是，在登录的时候，token 不止放入了cookie中，还放入了其他的位置！
        /*
        request.interceptors.request.use(function(config){
            //在请求发出之前进行一些操作
            // debugger
            if(auth.getToken()) {
                config.headers['token'] = auth.getToken();
            }
            ......
       }
        上面这个拦截器做了一件事情！ 就是将token 放入了 header 中！因为：考虑到这个登录可以扩展为移动端使用！因为手机浏览器登录是没有cookie的，它只有请求头
         */
        String token = request.getHeader("token");
        String userKey = RedisConst.USER_LOGIN_KEY_PREFIX + token;
        //在缓存中，登录时生成的token是不变的  通过相同的userKey可以删除掉对应的value
        redisTemplate.delete(userKey);
        return Result.ok();
    }
}
