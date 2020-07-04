package com.atguigu.gmall1213.cart.service.impl;

import com.atguigu.gmall1213.cart.mapper.CartInfoMapper;
import com.atguigu.gmall1213.cart.service.CartAsyncService;
import com.atguigu.gmall1213.model.cart.CartInfo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class CartAsyncServiceImpl implements CartAsyncService {
    @Autowired
    private CartInfoMapper cartInfoMapper;

    //修改购物车
    @Override
    @Async
    public void updateCartInfo(CartInfo cartInfo) {
        System.out.println("更新方法-----");
        cartInfoMapper.updateById(cartInfo);
    }

    //保存购物车
    @Override
    @Async
    public void saveCartInfo(CartInfo cartInfo) {
        System.out.println("插入方法-----");
        cartInfoMapper.insert(cartInfo);
    }

    //删除合并之后未登录的购物车
    @Override
    @Async
    public void deleteCartInfo(String userId) {
        QueryWrapper<CartInfo> cartInfoQueryWrapper = new QueryWrapper<>();
        cartInfoQueryWrapper.eq("user_id", userId);
        cartInfoMapper.delete(cartInfoQueryWrapper);
    }

    // 选中状态变更
    @Override
    @Async    //修改数据库
    public void checkCart(String userId, Integer isChecked, Long skuId) {
        // update cartInfo set is_checked = isChecked where user_id =userId and sku_id = skuId;
        // 第一个参数，表示要修改的数据，第二个参数更新条件。
        CartInfo cartInfo = new CartInfo();
        cartInfo.setIsChecked(isChecked);
        QueryWrapper<CartInfo> cartInfoQueryWrapper = new QueryWrapper<>();
        cartInfoQueryWrapper.eq("user_id", userId).eq("sku_id", skuId);
        cartInfoMapper.update(cartInfo, cartInfoQueryWrapper);
    }

    //页面删除购物车  数据库中
    @Override
    @Async
    public void deleteCartInfo(String userId, Long skuId) {
        QueryWrapper<CartInfo> cartInfoQueryWrapper = new QueryWrapper<>();
        cartInfoQueryWrapper.eq("user_id", userId);
        cartInfoQueryWrapper.eq("sku_id", skuId);
        cartInfoMapper.delete(cartInfoQueryWrapper);
    }
}
