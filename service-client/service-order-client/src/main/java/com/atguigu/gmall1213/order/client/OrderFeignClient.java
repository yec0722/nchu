package com.atguigu.gmall1213.order.client;

import com.atguigu.gmall1213.common.result.Result;
import com.atguigu.gmall1213.model.order.OrderInfo;
import com.atguigu.gmall1213.order.client.impl.OrderDegradeFeignClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@FeignClient(value = "service-order", fallback = OrderDegradeFeignClient.class)
public interface OrderFeignClient {
    // 订单明细
    @GetMapping("/api/order/auth/trade")
    Result<Map<String, Object>> trade();

    //根据订单Id 查询订单对象
    @GetMapping("/api/order/inner/getOrderInfo/{orderId}")
    OrderInfo getOrderInfo(@PathVariable Long orderId);

    //提交保存秒杀订单
    @PostMapping("/api/order/inner/seckill/submitOrder")
    Long submitOrder(@RequestBody OrderInfo orderInfo);
}
