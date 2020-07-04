package com.atguigu.gmall1213.cart.service;

import com.atguigu.gmall1213.model.cart.CartInfo;

public interface CartAsyncService {
    // 定义两个接口，要给是upd，一个insert

    //修改购物车
    void updateCartInfo(CartInfo cartInfo);

    //保存购物车
    void saveCartInfo(CartInfo cartInfo);

    //删除合并之后未登录的购物车
    void deleteCartInfo(String userId);

    //选中状态变更
    void checkCart(String userId, Integer isChecked, Long skuId);

    //页面删除购物车  数据库中
    void deleteCartInfo(String userId, Long skuId);
}