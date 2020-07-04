package com.atguigu.gmall1213.cart.controller;

import com.atguigu.gmall1213.cart.service.CartService;
import com.atguigu.gmall1213.common.result.Result;
import com.atguigu.gmall1213.common.util.AuthContextHolder;
import com.atguigu.gmall1213.model.cart.CartInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/cart")
public class CartApiController {
    @Autowired
    private CartService cartService;

    //保存购物车
    //http://cart.gmall.com/addCart.html?skuId=30&skuNum=1 页面提交过来的数据，应该属于web-all项目的！
    @PostMapping("/addToCart/{skuId}/{skuNum}")
    public Result addToCart(@PathVariable Long skuId,
                            @PathVariable Integer skuNum,
                            HttpServletRequest request) {
        //用户id从网关传过来 用户信息都放入了header中
        String userId = AuthContextHolder.getUserId(request);
        if (StringUtils.isEmpty(userId)) {
            //未登录  产生一个临时用户id
            userId = AuthContextHolder.getUserTempId(request);
        }
        //调用添加购物车方法
        cartService.addToCart(skuId, userId, skuNum);
        return Result.ok();
    }

    // 展示购物车列表
    @GetMapping("/cartList")
    public Result cartList(HttpServletRequest request) {
        // 获取登录的用户Id
        String userId = AuthContextHolder.getUserId(request);
        // 获取临时用户Id
        String userTempId = AuthContextHolder.getUserTempId(request);
        List<CartInfo> cartList = cartService.getCartList(userId, userTempId);
        // 返回数据
        return Result.ok(cartList);
    }

    // 更改选中状态控制器
    @GetMapping("checkCart/{skuId}/{isChecked}")
    public Result checkCart(@PathVariable Long skuId,
                            @PathVariable Integer isChecked,
                            HttpServletRequest request) {
        // 选中状态的变更 在登录，未登录的情况下都可以！
        // 获取登录的用户Id
        String userId = AuthContextHolder.getUserId(request);
        if (StringUtils.isEmpty(userId)) {
            //未登录
            userId = AuthContextHolder.getUserTempId(request);
        }
        cartService.checkCart(userId, isChecked, skuId);
        return Result.ok();
    }

    //删除页面购物车控制器
    @DeleteMapping("deleteCart/{skuId}")
    public Result deleteCart(@PathVariable Long skuId,
                             HttpServletRequest request) {
        String userId = AuthContextHolder.getUserId(request);
        if (StringUtils.isEmpty(userId)) {
            userId = AuthContextHolder.getUserTempId(request);
        }
        cartService.deleteCart(skuId, userId);
        return Result.ok();
    }

    //根据用户Id查询送货清单
    @GetMapping("getCartCheckedList/{userId}")
    public List<CartInfo> getCartCheckedList(@PathVariable String userId) {
        List<CartInfo> cartCheckedList = cartService.getCartCheckedList(userId);
        return cartCheckedList;
    }

    //根据用户Id 查询最新价格    与CartAsyncService中的loadCartCache()是同一个
    @GetMapping("loadCartCache/{userId}")
    public Result loadCartCache(@PathVariable("userId") String userId) {
        cartService.loadCartCache(userId);
        return Result.ok();
    }
}
