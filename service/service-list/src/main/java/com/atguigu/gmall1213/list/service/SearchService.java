package com.atguigu.gmall1213.list.service;

import com.atguigu.gmall1213.model.list.SearchParam;
import com.atguigu.gmall1213.model.list.SearchResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.openfeign.FeignClient;

public interface SearchService {

    //商品上架
    void upperGoods(Long skuId);

    //商品下架
    void lowerGoods(Long skuId);

    //同时上架多个skuId.
    void upperGoods();

    // 更新热点  做商品热度排名
    void incrHotScore(Long skuId);

    //根据SearchParam中封装好的条件检索数据
    SearchResponseVo search(SearchParam searchParam) throws Exception;
}
