package com.atguigu.gmall1213.product.controller;


import com.atguigu.gmall1213.common.result.Result;
import com.atguigu.gmall1213.model.product.BaseSaleAttr;
import com.atguigu.gmall1213.model.product.SpuInfo;
import com.atguigu.gmall1213.product.service.ManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/product")
public class SpuManageController {

    @Autowired
    private ManageService manageService;

    @GetMapping("/baseSaleAttrList")
    //加载销售属性
    public Result getBaseSaleAttrList() {
        List<BaseSaleAttr> baseSaleAttrList = manageService.getBaseSaleAttrList();
        return Result.ok(baseSaleAttrList);
    }

    //http://api.gmall.com/admin/product/saveSpuInfo
    @PostMapping("/saveSpuInfo")
    public Result saveSpuInfo(@RequestBody SpuInfo spuInfo) {
        if (null != spuInfo) {
            manageService.saveSpuInfo(spuInfo);
        }
        return Result.ok();
    }
}
