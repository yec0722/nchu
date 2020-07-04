package com.atguigu.gmall1213.cart.service.impl;

import com.atguigu.gmall1213.cart.mapper.CartInfoMapper;
import com.atguigu.gmall1213.cart.service.CartAsyncService;
import com.atguigu.gmall1213.cart.service.CartService;
import com.atguigu.gmall1213.common.constant.RedisConst;
import com.atguigu.gmall1213.model.cart.CartInfo;
import com.atguigu.gmall1213.model.product.SkuInfo;
import com.atguigu.gmall1213.product.client.ProductFeignClient;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.aspectj.weaver.ast.Var;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService {
    @Autowired
    private CartInfoMapper cartInfoMapper;
    @Autowired
    private ProductFeignClient productFeignClient;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private CartAsyncService cartAsyncService;

    /*
    添加购物车
        添加购物车业务逻辑：
        1.  判断当前要添加的商品，在购物车中是否存在！
            1.1  存在：
                    则商品的数量相加 {更新}
            1.2  不存在：
                    则直接添加到购物车 {插入}
         2. 无论你是存在，还是不存在，我们都需要更新缓存
         */
    @Override
    public void addToCart(Long skuId, String userId, Integer skuNum) {
        //  获取用户购物车cartkey
        String cartKey = getCartKey(userId);
        if (!redisTemplate.hasKey(cartKey)) {
            loadCartCache(userId);
        }
        //查询谁的购物车，买哪个商品   select * from cart_info where userId=? and skuId=?
        QueryWrapper<CartInfo> cartInfoQueryWrapper = new QueryWrapper<>();
        cartInfoQueryWrapper.eq("user_id", userId);
        cartInfoQueryWrapper.eq("sku_id", skuId);
        CartInfo cartInfoExist = cartInfoMapper.selectOne(cartInfoQueryWrapper);
        if (null != cartInfoExist) {
            //购物车存在该商品  做更新操作 更新数量
            cartInfoExist.setSkuNum(cartInfoExist.getSkuNum() + skuNum);
            /*@TableField(exist = false)
              BigDecimal skuPrice;
             */
            //初始化一个skuPrice   数据库中不存在skuPrice   skuPrice=skuInfo.price
            //  cartInfoExist.setSkuPrice(cartInfoExist.getCartPrice());
            cartInfoExist.setSkuPrice(productFeignClient.getSkuPrice(skuId));
            //更新
            //  cartInfoMapper.updateById(cartInfoExist);
            cartAsyncService.updateCartInfo(cartInfoExist);
        } else {
            //购物车中没有商品
            //购物车中的数据，都是来自于商品详情，商品详情的数据是来自于servce-product.
            SkuInfo skuInfo = productFeignClient.getSkuInfoById(skuId);
            // 声明一个cartInfo 对象
            CartInfo cartInfo = new CartInfo();
            cartInfo.setSkuPrice(skuInfo.getPrice());
            cartInfo.setCartPrice(skuInfo.getPrice());
            cartInfo.setSkuNum(skuNum);
            cartInfo.setSkuName(skuInfo.getSkuName());
            cartInfo.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfo.setSkuId(skuId);
            cartInfo.setUserId(userId);
            //新增数据
            //cartInfoMapper.insert(cartInfo);
            //优化 使用异步
            cartAsyncService.saveCartInfo(cartInfo);
            // 如果代码走到了这，说明cartInfoExist 是空。cartInfoExist 可能会被GC吃了。废物再利用
            cartInfoExist = cartInfo;
        }
        //放入缓存
        redisTemplate.boundHashOps(cartKey).put(skuId.toString(), cartInfoExist);
        //给缓存设置一个过期时间
        setCartKeyExpire(cartKey);
    }

    //查询展示购物车
    @Override
    public List<CartInfo> getCartList(String userId, String userTempId) {
        List<CartInfo> cartInfoList = new ArrayList<>();
        //判断登录还是未登录
        if (StringUtils.isEmpty(userId)) {
            //说明未登录   获取临时用户id
            cartInfoList = getCartList(userTempId);
        }
        if (!StringUtils.isEmpty(userId)) {
            //说明已经登录   登录的时候要进行合并购物车，即将未登录时的购物车数据合并到登录后的购物车中
            //合并之前 要判断未登录时的购物车中有没有商品数据
            List<CartInfo> cartInfoNoLoginList = getCartList(userTempId);
            if (!CollectionUtils.isEmpty(cartInfoNoLoginList)) {
                // 开始合并购物车       cartInfoNoLoginList:未登录时的购物车    userId:用户登录id
                cartInfoList = mergeToCartList(cartInfoNoLoginList, userId);
                //合并之后 删除未登录时的购物车
                deleteCartList(userTempId);
            }
            //如果未登录时的未登录时的购物车中没有商品数据   直接返回登录时的购物车数据
            if (CollectionUtils.isEmpty(cartInfoNoLoginList) || StringUtils.isEmpty(userTempId)) {
                cartInfoList = getCartList(userId);
            }
        }
        return cartInfoList;
    }

    //获取购物车列表  先从缓存中取，若没有，从数据库查找并放入缓存
    private List<CartInfo> getCartList(String userId) {
        List<CartInfo> cartInfoList = new ArrayList<>();
        if (StringUtils.isEmpty(userId)) {
            return null;
        }
        //获取缓存中的购物车数据信息
        String cartKey = getCartKey(userId);
        cartInfoList = redisTemplate.opsForHash().values(cartKey);
        //判断集合数据cartInfoExist是否存在
        if (!CollectionUtils.isEmpty(cartInfoList)) {
            //表示从缓存中拿到了数据   数据展示的时候应该是有规则排序
            cartInfoList.sort(new Comparator<CartInfo>() {
                //按照id比较
                @Override
                public int compare(CartInfo o1, CartInfo o2) {
                    return o1.getId().compareTo(o2.getId());
                }
            });
            return cartInfoList;
        } else {
            //表示缓存中没有数据  先从数据库中查询，查出来后再放入缓存中
            cartInfoList = loadCartCache(userId);
            return cartInfoList;
        }
    }

    //合并购物车   userId:用户登录id
    private List<CartInfo> mergeToCartList(List<CartInfo> cartInfoNoLoginList, String userId) {
        // 先根据登录后的userId   获取登录的购物车数据
        List<CartInfo> cartListLogin = getCartList(userId);
        // 登录购物车数据分为两种状态 一个登录购物车中有数据，未登录插入时可能需要做循环遍历合并，一个登录购物车中没有数据，未登录购物车数据直接插入数据库
        // 将登录的集合数据转化为map key=skuId,value=cartInfo     cartInfoMap:登录的
        Map<Long, CartInfo> cartInfoMap = cartListLogin.stream().collect(Collectors.toMap(CartInfo::getSkuId, cartInfo -> cartInfo));
        //对未登录数据库进行循环    cartInfoNoLoginList:未登录时的购物车
        for (CartInfo cartInfoNoLogin : cartInfoNoLoginList) {
            //未登录数据在登录中存在
            //获取到未登录时的skuId
            Long skuId = cartInfoNoLogin.getSkuId();
            // 判断登录时购物车转化后的map中是否包含未登录购物车中的skuId
            if (cartInfoMap.containsKey(skuId)) {
                // 登录和未登录有相同的商品 ,将数量相加，相加之后的数据给登录
                CartInfo cartInfoLogin = cartInfoMap.get(skuId);
                cartInfoLogin.setSkuNum(cartInfoLogin.getSkuNum() + cartInfoNoLogin.getSkuNum());
                //在合并的时候，我们只处理未登录状态下被选中的商品
                if (cartInfoNoLogin.getIsChecked().intValue() == 1) {
                    // 将登录购物车中的商品也变为选中状态!
                    cartInfoLogin.setIsChecked(1);
                }
                //更新数据库中的信息
                cartAsyncService.updateCartInfo(cartInfoLogin);
            } else {
                // 未登录数据在登录中没有或者不存在
                // 未登录中有一个临时用户Id，此时需要将临时用户Id 变为登录用户Id
                cartInfoNoLogin.setUserId(userId);
                //异步插入数据库
                cartAsyncService.saveCartInfo(cartInfoNoLogin);
            }
        }
        //最终的合并结果  因为上述两个状态最后在处理完自己的业务之后都更新了数据库
        List<CartInfo> cartInfoList = loadCartCache(userId);
        return cartInfoList;
    }

    //合并之后 删除未登录时的购物车
    private void deleteCartList(String userTempId) {
        //删两个地方  一个是数据库 一个是缓存
        //异步删除数据库中的临时用户id
        cartAsyncService.deleteCartInfo(userTempId);
        //同步删除缓存中的临时用户id
        String cartKey = getCartKey(userTempId);
        if (redisTemplate.hasKey(cartKey)) {
            redisTemplate.delete(cartKey);
        }
    }

    //将更新后的选中状态保存 cartInfoNoLogin.getIsChecked().intValue() == 1      也分为数据库更新和缓存更新
    @Override
    public void checkCart(String userId, Integer isChecked, Long skuId) {
        //异步更新数据库
        cartAsyncService.checkCart(userId, isChecked, skuId);
        //同步更新缓存   cartKey放入缓存的格式： redisTemplate.boundHashOps(cartKey).put(skuId.toString(), cartInfoExist);
        String cartKey = getCartKey(userId);
        BoundHashOperations boundHashOperations = redisTemplate.boundHashOps(cartKey);
        //boundHashOperations的数据格式是 map(skuId.toString(), cartInfoExist)
        //缓存中的购物车商品中包含该商品
        if (boundHashOperations.hasKey(skuId.toString())) {
            // 根据skuId 获取到对应的cartInfo
            CartInfo cartInfo = (CartInfo) boundHashOperations.get(skuId.toString());
            // 对应修改选中状态
            cartInfo.setIsChecked(isChecked);
            // 修改完成之后，将修改好的cartInfo 放入缓存
            boundHashOperations.put(skuId.toString(), cartInfo);
            // 修改一下过期时间
            setCartKeyExpire(cartKey);
        }
    }


    //根据用户Id 查询数据库中的购物车信息并将数据放入缓存。
    public List<CartInfo> loadCartCache(String userId) {
        //从数据库中查询
        QueryWrapper<CartInfo> cartInfoQueryWrapper = new QueryWrapper<>();
        cartInfoQueryWrapper.eq("user_id", userId);
        List<CartInfo> cartInfoList = cartInfoMapper.selectList(cartInfoQueryWrapper);
        if (CollectionUtils.isEmpty(cartInfoList)) {
            //从数据库中也没查到  返回一个空对象
            return cartInfoList;
        }
        // 声明一个map 集合来存储数据
        HashMap<String, CartInfo> map = new HashMap<>();
        // 获取到缓存key
        String cartKey = getCartKey(userId);
        //循环遍历集合将map中填入数据
        for (CartInfo cartInfo : cartInfoList) {
            //还有可能价格会发生变化
            cartInfo.setSkuPrice(productFeignClient.getSkuPrice(cartInfo.getSkuId()));
            map.put(cartInfo.getSkuId().toString(), cartInfo);
        }
        //从数据库中查到了  放入缓存
        redisTemplate.opsForHash().putAll(cartKey, map);
        //设置过期时间
        setCartKeyExpire(cartKey);
        return cartInfoList;
    }

    //删除购物车  也分为异步数据库删除和同步缓存删除
    @Override
    public void deleteCart(Long skuId, String userId) {
        //异步数据库删除
        cartAsyncService.deleteCartInfo(userId, skuId);
        //同步缓存删除购物车  获取cartKey
        String cartKey = getCartKey(userId);
        BoundHashOperations boundHashOperations = redisTemplate.boundHashOps(cartKey);
        if (boundHashOperations.hasKey(skuId.toString())) {
            boundHashOperations.delete(skuId.toString());
        }
    }

    //根据用户Id查询送货清单
    @Override
    public List<CartInfo> getCartCheckedList(String userId) {
        List<CartInfo> cartInfos = new ArrayList<>();
        // 查询购物车列表是因为，我们的送货清单是从购物车来的，购物车中选中的商品才是送货清单！
        // 在此直接查询缓存即可！
        String cartKey = getCartKey(userId);
        //根据cartKey 可查出key和value  我们直接取value(cartInfo)
        List<CartInfo> cartInfoList = redisTemplate.opsForHash().values(cartKey);
        if (!CollectionUtils.isEmpty(cartInfoList)) {
            for (CartInfo cartInfo : cartInfoList) {
                //购物车中选中的商品才是送货清单！
                if (cartInfo.getIsChecked().intValue() == 1) {
                    cartInfos.add(cartInfo);
                }
            }
        }
        return cartInfos;
    }

    //购物车中的商品数据要想放入缓存中，则必须设置一个cartKey
    private String getCartKey(String userId) {
        return RedisConst.USER_KEY_PREFIX + userId + RedisConst.USER_CART_KEY_SUFFIX;
    }

    // 设置缓存中cartKey的过期时间
    private void setCartKeyExpire(String cartKey) {
        redisTemplate.expire(cartKey, RedisConst.USER_CART_EXPIRE, TimeUnit.SECONDS);
    }
}
