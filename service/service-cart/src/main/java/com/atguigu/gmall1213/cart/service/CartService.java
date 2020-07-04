package com.atguigu.gmall1213.cart.service;

import com.atguigu.gmall1213.model.cart.CartInfo;

import java.util.List;

public interface CartService {
    // 添加购物车
    void addToCart(Long skuId, String userId, Integer skuNum);

    //查询显示购物车  查询登录和查询未登录
    List<CartInfo> getCartList(String userId, String userTempId);

    //更新选中状态
    void checkCart(String userId, Integer isChecked, Long skuId);

    //删除购物车
    void deleteCart(Long skuId, String userId);

    //根据用户Id查询送货清单
    List<CartInfo> getCartCheckedList(String userId);

    //根据用户Id 查询数据库中的购物车信息并将数据放入缓存。
    List<CartInfo> loadCartCache(String userId);
}
