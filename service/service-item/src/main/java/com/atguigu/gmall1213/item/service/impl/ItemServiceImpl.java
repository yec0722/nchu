package com.atguigu.gmall1213.item.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall1213.common.result.Result;
import com.atguigu.gmall1213.item.service.ItemService;
import com.atguigu.gmall1213.list.client.ListFeignClient;
import com.atguigu.gmall1213.model.product.BaseCategoryView;
import com.atguigu.gmall1213.model.product.SkuInfo;
import com.atguigu.gmall1213.model.product.SpuSaleAttr;
import com.atguigu.gmall1213.product.client.ProductFeignClient;
import org.apache.tomcat.util.threads.ThreadPoolExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Service
public class ItemServiceImpl implements ItemService {
    @Resource
    private ProductFeignClient productFeignClient;
    @Resource
    private ListFeignClient listFeignClient;

    //通过SkuId获取数据
    @Override
    public Map<String, Object> getBySkuId(Long skuId) {
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                50, // 核心线程池数
                200, // 最大线程池数据
                30, // 剩余空闲线程的存活时间
                TimeUnit.SECONDS, // 时间单位
                new ArrayBlockingQueue<>(50));// 阻塞队列);
        HashMap<String, Object> result = new HashMap<>();
        /*
        使用异步编排
         */
        // 保存skuInfo
        CompletableFuture<SkuInfo> skuInfoCompletableFuture = CompletableFuture.supplyAsync(() -> {
            SkuInfo skuInfo = productFeignClient.getSkuInfoById(skuId);
            // 保存skuInfo
            result.put("skuInfo", skuInfo);
            //这个skuInfo 后续代码要用，故要返回
            return skuInfo;
        }, threadPoolExecutor);
        //因为是从上边代码获得的skuInfo，因为参数只能传skuInfo
        CompletableFuture<Void> spuSaleAttrCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync((skuInfo) -> {
            //通过skuId、spuId获取销售属性集合数据
            List<SpuSaleAttr> spuSaleAttrListCheckBySku = productFeignClient.getSpuSaleAttrListCheckBySku(skuId, skuInfo.getSpuId());
            result.put("spuSaleAttrList", spuSaleAttrListCheckBySku);
        }, threadPoolExecutor);
        //通过getCategory3Id获取分类数据
        CompletableFuture<Void> categoryViewCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync((skuInfo) -> {
            //通过skuId、spuId获取销售属性集合数据
            BaseCategoryView categoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id());
            result.put("categoryView", categoryView);
        }, threadPoolExecutor);
        //通过skuId获取价格信息  不需要传skuInfo
        CompletableFuture<Void> priceCompletableFuture = CompletableFuture.runAsync(() -> {
            BigDecimal skuPrice = productFeignClient.getSkuPrice(skuId);
            result.put("price", skuPrice);
        }, threadPoolExecutor);
        //根据spuId获取由销售属性值id和skuId组成的map(切换sku)
        CompletableFuture<Void> valuesSkuJsonCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync((skuInfo) -> {
            //通过skuId、spuId获取销售属性集合数据
            Map skuValueIdsMap = productFeignClient.getSkuValueIdsMap(skuInfo.getSpuId());
            //需要将skuValueIdsMap转换成json字符串传给前端
            String valuesSkuJson = JSON.toJSONString(skuValueIdsMap);
            result.put("valuesSkuJson", valuesSkuJson);
        }, threadPoolExecutor);
        //热度排名
        CompletableFuture<Void> incrHotScoreCompletableFuture = CompletableFuture.runAsync(() -> {
            listFeignClient.incrHotScore(skuId);
        }, threadPoolExecutor);

        // allOf：等待所有任务完成
        CompletableFuture.allOf(skuInfoCompletableFuture,
                spuSaleAttrCompletableFuture,
                categoryViewCompletableFuture,
                priceCompletableFuture,
                valuesSkuJsonCompletableFuture,
                incrHotScoreCompletableFuture).join();
        //通过skuId获取skuInfo信息
//        SkuInfo skuInfo = productFeignClient.getSkuInfoById(skuId);
//        //通过skuId、spuId获取销售属性集合数据
//        List<SpuSaleAttr> spuSaleAttrListCheckBySku = productFeignClient.getSpuSaleAttrListCheckBySku(skuId, skuInfo.getSpuId());
//        //通过getCategory3Id获取分类数据
//        BaseCategoryView categoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id());
//        //通过skuId获取价格信息
//        BigDecimal skuPrice = productFeignClient.getSkuPrice(skuId);
//        //根据spuId获取由销售属性值id和skuId组成的map(切换sku)
//        Map skuValueIdsMap = productFeignClient.getSkuValueIdsMap(skuInfo.getSpuId());
        //需要将skuValueIdsMap转换成json字符串传给前端
//        String valuesSkuJson = JSON.toJSONString(skuValueIdsMap);
        //保存三级分类
//        result.put("categoryView", categoryView);
        //保存商品价格
//        result.put("price", skuPrice);
        // 保存通过skuId、spuId获取的销售属性集合数据
//        result.put("valuesSkuJson", valuesSkuJson);
        //保存销售属性-销售属性值
//        result.put("spuSaleAttrList", spuSaleAttrListCheckBySku);
        // 保存skuInfo
//        result.put("skuInfo", skuInfo);
        return result;
    }
}
