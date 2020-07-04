package com.atguigu.gmall1213.product.service;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall1213.model.product.*;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface ManageService {
    /*
     * 查询所有的一级分类
     * */
    List<BaseCategory1> getCategory1();

    /*
     * 根据一级分类id查询二级分类
     * */
    List<BaseCategory2> getCategory2(Long category1Id);

    /*
     * 根据二级分类id查询三级分类
     * */
    List<BaseCategory3> getCategory3(Long category2Id);

    /*
     * 根据三级分类id查询平台属性数据
     * */
    List<BaseAttrInfo> getAttrInfoList(Long category1Id, Long category2Id, Long category3Id);

    void saveAttrInfo(BaseAttrInfo baseAttrInfo);


    BaseAttrInfo getAttrInfo(Long attrId);

    IPage<SpuInfo> selectPage(Page<SpuInfo> spuInfoPageParam, SpuInfo spuInfo);


    List<BaseSaleAttr> getBaseSaleAttrList();

    void saveSpuInfo(SpuInfo spuInfo);

    //回显SpuImage列表
    List<SpuImage> getSpuImageList(Long spuId);

    //回显销售属性、属性值
    List<SpuSaleAttr> getSpuSaleAttrList(Long spuId);

    //保存SKU
    void saveSkuInfo(SkuInfo skuInfo);

    //查询所有的 SKU
    IPage<SkuInfo> selectPage(Page<SkuInfo> skuInfoPagenfo);

    //实现商品上架
    void onSale(Long skuId);

    //实现商品下架
    void cancelSale(Long skuId);

    //根据SkuId查询SKU基本信息
    SkuInfo getSkuInfo(Long skuId);

    /**
     * 根据三级分类Id 来获取分类名称
     */
    BaseCategoryView getBaseCategoryViewBycategory3Id(Long category3Id);

    //根据skuId查询商品价格信息
    BigDecimal getSkuPriceBySkuId(Long skuId);


    //根据skuId spuId 查询销售属性及销售属性值集合数据
    List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(Long skuId, Long spuId);

    /**
     * 根据spuId 查询数据
     */
    Map getSkuValueIdsMap(Long spuId);

    /**
     * 获取全部分类数据信息
     */
    List<JSONObject> getBaseCategoryList();

    //根据品牌id查询品牌数据
    BaseTrademark getBaseTrademarkByTmId(Long tmId);

    //根据skuId获取平台属性和平台属性值
    List<BaseAttrInfo> getAttrInfoList(Long skuId);
}
