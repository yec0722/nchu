package com.atguigu.gmall1213.product.api;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall1213.common.result.Result;
import com.atguigu.gmall1213.model.product.*;
import com.atguigu.gmall1213.product.service.ManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@RestController
@RequestMapping("/api/product")
public class ProductApiController {
    @Autowired
    private ManageService manageService;

    //根据SkuId查询SKU基本信息
    @GetMapping("/inner/getSkuInfo/{skuId}")
    public SkuInfo getSkuInfoById(@PathVariable Long skuId) {
        return manageService.getSkuInfo(skuId);
    }

    //根据三级分类id直接查询一级、二级、三级的分类id和名称
    @GetMapping("/inner/getCategoryView/{category3Id}")
    public BaseCategoryView getCategoryView(@PathVariable Long category3Id) {
        return manageService.getBaseCategoryViewBycategory3Id(category3Id);
    }

    //根据skuId获取价格信息
    @GetMapping("/inner/getSkuPrice/{skuId}")
    public BigDecimal getSkuPrice(@PathVariable Long skuId) {
        return manageService.getSkuPriceBySkuId(skuId);
    }

    // 回显销售属性-销售属性值
    @GetMapping("/inner/getSpuSaleAttrListCheckBySku/{skuId}/{spuId}")
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(@PathVariable Long skuId,
                                                          @PathVariable Long spuId) {
        return manageService.getSpuSaleAttrListCheckBySku(skuId, spuId);
    }

    // 点击销售属性值进行切换
    @GetMapping("/inner/getSkuValueIdsMap/{spuId}")
    public Map getSkuValueIdsMap(@PathVariable Long spuId) {
        return manageService.getSkuValueIdsMap(spuId);
    }

    //查询一、二、三级分类所有数据(首页数据)
    @GetMapping("/getBaseCategoryList")
    public Result getBaseCategoryList() {
        List<JSONObject> baseCategoryList = manageService.getBaseCategoryList();
        return Result.ok(baseCategoryList);
    }

    //根据品牌id查询品牌数据
    @GetMapping("/inner/getTrademark/{tmId}")
    public BaseTrademark getTrademark(@PathVariable("tmId") Long tmId) {
        return manageService.getBaseTrademarkByTmId(tmId);
    }

    //根据skuId获取平台属性和平台属性值  每个skuId都有属于自己的平台属性和平台属性值
         /*
         SELECT
            *
        FROM
            base_attr_info bai
            INNER JOIN base_attr_value bav ON bai.id = bav.attr_id
            INNER JOIN sku_attr_value sav ON sav.value_id = bav.id
        WHERE
            sav.sku_id = 30
          */
    @GetMapping("/inner/getAttrList/{skuId}")
    public List<BaseAttrInfo> getAttrList(@PathVariable("skuId") Long skuId) {
        List<BaseAttrInfo> attrInfoList = manageService.getAttrInfoList(skuId);
        return attrInfoList;
    }
}

