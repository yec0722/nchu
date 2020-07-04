package com.atguigu.gmall1213.product.client.impl;

import com.atguigu.gmall1213.common.result.Result;
import com.atguigu.gmall1213.model.product.*;
import com.atguigu.gmall1213.product.client.ProductFeignClient;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Component
public class ProductDegradeFeignClient implements ProductFeignClient {
    @Override
    public SkuInfo getSkuInfoById(Long skuId) {
        return null;
    }

    @Override
    public BaseCategoryView getCategoryView(Long category3Id) {
        return null;
    }

    @Override
    public BigDecimal getSkuPrice(Long skuId) {
        return null;
    }

    @Override
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(Long skuId, Long spuId) {
        return null;
    }

    @Override
    public Map getSkuValueIdsMap(Long spuId) {
        return null;
    }

    @Override
    public Result getBaseCategoryList() {
        return null;
    }

    @Override
    public BaseTrademark getTrademark(Long tmId) {
        return null;
    }

    @Override
    public List<BaseAttrInfo> getAttrList(Long skuId) {
        return null;
    }
}
