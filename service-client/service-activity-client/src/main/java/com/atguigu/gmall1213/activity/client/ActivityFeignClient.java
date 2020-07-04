package com.atguigu.gmall1213.activity.client;

import com.atguigu.gmall1213.activity.client.impl.ActivityDegradeFeignClient;
import com.atguigu.gmall1213.common.result.Result;
import com.atguigu.gmall1213.model.activity.SeckillGoods;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Map;

@FeignClient(name = "service-activity", fallback = ActivityDegradeFeignClient.class)
public interface ActivityFeignClient {
    // 查询所有秒杀商品列表
    @GetMapping("/api/activity/seckill/findAll")
    Result findAll();

    // 根据秒杀商品Id 查看秒杀商品详情
    @GetMapping("/api/activity/seckill/getSeckillGoods/{skuId}")
    Result getSeckillGoods(@PathVariable Long skuId);

    //秒杀下单
    @GetMapping("/api/activity/seckill/auth/trade")
    Result<Map<String , Object>> trade();
}
