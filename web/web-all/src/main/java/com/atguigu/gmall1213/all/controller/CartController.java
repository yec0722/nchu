package com.atguigu.gmall1213.all.controller;

import com.atguigu.gmall1213.cart.client.CartFeignClient;
import com.atguigu.gmall1213.model.product.SkuInfo;
import com.atguigu.gmall1213.product.client.ProductFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;

@Controller
public class CartController {
    @Autowired
    private CartFeignClient cartFeignClient;
    @Autowired
    private ProductFeignClient productFeignClient;

    @RequestMapping("addCart.html")
    public String addCart(@RequestParam(name = "skuId") Long skuId,
                          @RequestParam(name = "skuNum") Integer skuNum,
                          HttpServletRequest request) {
        //添加购物车
        cartFeignClient.addToCart(skuId, skuNum);
        //还要获取skuInfo 因为要显示图片、商品名称、价格等   页面需要传递一个skuInfo
        SkuInfo skuInfo = productFeignClient.getSkuInfoById(skuId);
        request.setAttribute("skuNum", skuNum);
        request.setAttribute("skuInfo", skuInfo);
        return "cart/addCart";
    }

    //展示购物车
    @RequestMapping("cart.html")
    public String cartList() {
        return "cart/index";
    }
}
