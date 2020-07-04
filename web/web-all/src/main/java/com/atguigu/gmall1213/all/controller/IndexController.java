package com.atguigu.gmall1213.all.controller;

import com.atguigu.gmall1213.common.result.Result;
import com.atguigu.gmall1213.product.client.ProductFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class IndexController {

    @Autowired
    private ProductFeignClient productFeignClient;

    // 访问 / 或者 index.html 时都可以显示首页信息
    @GetMapping({"/", "index.html"})
    public String index(Model model) {
        Result baseCategoryList = productFeignClient.getBaseCategoryList();
        model.addAttribute("list", baseCategoryList.getData());
        return "index/index";
    }
}
