package com.atguigu.gmall1213.product.controller;

import com.atguigu.gmall1213.common.result.Result;
import com.atguigu.gmall1213.model.product.*;
import com.atguigu.gmall1213.product.service.ManageService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/admin/product")
public class SkuManageController {
    @Autowired
    private ManageService manageService;

    /*http://api.gmall.com/admin/product/spuImageList/14
      回显图片列表
     */
    @GetMapping("/spuImageList/{spuId}")
    public Result getSpuImageList(@PathVariable("spuId") Long spuId) {
        List<SpuImage> spuImageList = manageService.getSpuImageList(spuId);
        return Result.ok(spuImageList);
    }

    /*回显销售属性、销售属性值
    http://api.gmall.com/admin/product/spuSaleAttrList/14
    */
    @GetMapping("/spuSaleAttrList/{spuId}")
    public Result getSpuSaleAttrList(@PathVariable("spuId") Long spuId) {
        List<SpuSaleAttr> spuSaleAttrList = manageService.getSpuSaleAttrList(spuId);
        return Result.ok(spuSaleAttrList);
    }

    //SKU数据保存
    @PostMapping("/saveSkuInfo")
    public Result saveSkuInfo(@RequestBody SkuInfo skuInfo) {
        manageService.saveSkuInfo(skuInfo);
        return Result.ok();
    }

    /*
    查询所有的SKU数据
    http://api.gmall.com/admin/product/list/1/10
     */
    @GetMapping("/list/{page}/{limit}")
    public Result getList(@PathVariable Long page,
                          @PathVariable Long limit) {
        Page<SkuInfo> skuInfoPagenfo = new Page<>(page, limit);
        IPage<SkuInfo> skuInfoIPage = manageService.selectPage(skuInfoPagenfo);
        return Result.ok(skuInfoIPage);
    }

    /*
     * 商品的上架
     * http://api.gmall.com/admin/product/onSale/32
     * */
    @GetMapping("/onSale/{skuId}")
    public Result onSale(@PathVariable Long skuId) {
        manageService.onSale(skuId);
        return Result.ok();
    }

    //商品下架  http://api.gmall.com/admin/product/cancelSale/32
    @GetMapping("/cancelSale/{skuId}")
    public Result cancelSale(@PathVariable Long skuId) {
        manageService.cancelSale(skuId);
        return Result.ok();
    }
}


