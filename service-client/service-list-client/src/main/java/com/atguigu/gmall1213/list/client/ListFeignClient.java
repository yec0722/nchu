package com.atguigu.gmall1213.list.client;

import com.atguigu.gmall1213.common.result.Result;
import com.atguigu.gmall1213.list.client.impl.ListDegradeFeignClient;
import com.atguigu.gmall1213.model.list.SearchParam;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "service-list",fallback = ListDegradeFeignClient.class)
public interface ListFeignClient {
    //商品热度排名
    @GetMapping("/api/list/inner/incrHotScore/{skuId}")
    public Result incrHotScore(@PathVariable Long skuId);

    //上架
    @GetMapping("/api/list/inner/upperGoods/{skuId}")
    public Result upperGoods(@PathVariable Long skuId);

    //下架
    @GetMapping("/api/list/inner/lowerGoods/{skuId}")
    public Result lowerGoods(@PathVariable Long skuId);

    //查询的时候输入的是json 字符串   检索
    @PostMapping("/api/list")
    public Result getList(@RequestBody SearchParam searchParam);
}
