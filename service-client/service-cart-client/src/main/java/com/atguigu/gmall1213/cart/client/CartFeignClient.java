package com.atguigu.gmall1213.cart.client;

import com.atguigu.gmall1213.cart.client.impl.CartDegradeFeignClient;
import com.atguigu.gmall1213.common.result.Result;
import com.atguigu.gmall1213.model.cart.CartInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@FeignClient(name = "service-cart", fallback = CartDegradeFeignClient.class)
public interface CartFeignClient {

    //添加购物车
    @PostMapping("/api/cart/addToCart/{skuId}/{skuNum}")
    Result addToCart(@PathVariable("skuId") Long skuId,
                     @PathVariable("skuNum") Integer skuNum);

    //根据用户Id查询送货清单
    @GetMapping("/api/cart/getCartCheckedList/{userId}")
    public List<CartInfo> getCartCheckedList(@PathVariable String userId);

    //
    @GetMapping("/api/cart/loadCartCache/{userId}")
    Result loadCartCache(@PathVariable("userId") String userId);

}
