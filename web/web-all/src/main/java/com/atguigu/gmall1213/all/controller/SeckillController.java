package com.atguigu.gmall1213.all.controller;

import com.atguigu.gmall1213.activity.client.ActivityFeignClient;
import com.atguigu.gmall1213.common.result.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@Controller
public class SeckillController {
    @Autowired
    private ActivityFeignClient activityFeignClient;

    // 查询所有秒杀商品列表
    @GetMapping("seckill.html")
    public String seckill(Model model) {
        Result result = activityFeignClient.findAll();
        model.addAttribute("list", result.getData());
        return "seckill/index";
    }

    //   http://activity.gmall.com/seckill/31.html
    @GetMapping("seckill/{skuId}.html")
    public String seckillItem(@PathVariable Long skuId, Model model) {
        // 获取秒杀商品详情
        Result result = activityFeignClient.getSeckillGoods(skuId);
        // 页面需要在后台存储一个item 对象
        model.addAttribute("item", result.getData());
        return "seckill/item";
    }

    //进入排队页面                    http://activity.gmall.com/seckill/queue.html?skuId=31&skuIdStr=c4ca4238a0b923820dcc509a6f75849b
    @GetMapping("seckill/queue.html")
    public String queue(HttpServletRequest request) {
        // 由页面得知，需要在后台存储两个参数 skuId skuIdStr
        String skuId = request.getParameter("skuId");
        String skuIdStr = request.getParameter("skuIdStr");
        System.out.println("skuId:\t" + skuId + "\t skuIdStr:\t" + skuIdStr);
        request.setAttribute("skuId", skuId);
        request.setAttribute("skuIdStr", skuIdStr);
        // 跳转到排队页面！
        return "seckill/queue";
    }

    //秒杀下单
    @GetMapping("seckill/trade.html")
    public String trade(Model model) {
        Result<Map<String, Object>> result = activityFeignClient.trade();
        //model.addAllAttributes(result.getData());
        //return "seckill/trade";
        if (result.isOk()) {
            // 下单正常
            model.addAllAttributes(result.getData());
            return "seckill/trade";
        } else {
            model.addAttribute("message", result.getMessage());
            //秒杀抢购失败页面
            return "seckill/fail";
        }
    }
}
