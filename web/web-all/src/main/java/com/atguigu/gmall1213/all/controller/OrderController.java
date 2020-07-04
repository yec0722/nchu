package com.atguigu.gmall1213.all.controller;

import com.atguigu.gmall1213.common.result.Result;
import com.atguigu.gmall1213.order.client.OrderFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;

import java.util.Map;

@Controller
public class OrderController {
    @Autowired
    private OrderFeignClient orderFeignClient;

    // http://order.gmall.com/trade.html
    @GetMapping("trade.html")
    public String trade(Model model) {
        Result<Map<String, Object>> result = orderFeignClient.trade();
        // 存储数据，给前台页面使用
        //result是Result对象    要拿其中的值
        model.addAllAttributes(result.getData());
        // 订单页面
        return "order/trade";
    }
}
