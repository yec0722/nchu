package com.atguigu.gmall1213.product.client;

import com.atguigu.gmall1213.common.result.Result;
import com.atguigu.gmall1213.model.product.*;
import com.atguigu.gmall1213.product.client.impl.ProductDegradeFeignClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@FeignClient(name = "service-product", fallback = ProductDegradeFeignClient.class)
public interface ProductFeignClient {
    //根据SkuId查询SKUInfo基本信息
    @GetMapping("/api/product/inner/getSkuInfo/{skuId}")
    public SkuInfo getSkuInfoById(@PathVariable Long skuId);

    //根据三级分类id直接查询一级、二级、三级的分类id和名称
    @GetMapping("/api/product/inner/getCategoryView/{category3Id}")
    public BaseCategoryView getCategoryView(@PathVariable Long category3Id);

    //根据skuId获取价格信息
    @GetMapping("/api/product/inner/getSkuPrice/{skuId}")
    public BigDecimal getSkuPrice(@PathVariable Long skuId);

    //回显销售属性-销售属性值
    @GetMapping("/api/product/inner/getSpuSaleAttrListCheckBySku/{skuId}/{spuId}")
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(@PathVariable Long skuId,
                                                          @PathVariable Long spuId);

    //点击销售属性值进行切换
    @GetMapping("/api/product/inner/getSkuValueIdsMap/{spuId}")
    public Map getSkuValueIdsMap(@PathVariable Long spuId);

    //查询一、二、三级分类所有数据(首页数据)
    @GetMapping("/api/product/getBaseCategoryList")
    public Result getBaseCategoryList();

    //根据品牌id查询品牌数据
    @GetMapping("/api/product/inner/getTrademark/{tmId}")
    public BaseTrademark getTrademark(@PathVariable("tmId") Long tmId);

    //根据skuId获取平台属性和平台属性值  每个skuId都有属于自己的平台属性和平台属性值
    @GetMapping("/api/product/inner/getAttrList/{skuId}")
    public List<BaseAttrInfo> getAttrList(@PathVariable("skuId") Long skuId);
}
