package com.atguigu.gmall1213.order.controller;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall1213.cart.client.CartFeignClient;
import com.atguigu.gmall1213.common.result.Result;
import com.atguigu.gmall1213.common.util.AuthContextHolder;
import com.atguigu.gmall1213.model.cart.CartInfo;
import com.atguigu.gmall1213.model.order.OrderDetail;
import com.atguigu.gmall1213.model.order.OrderInfo;
import com.atguigu.gmall1213.model.user.UserAddress;
import com.atguigu.gmall1213.order.service.OrderService;
import com.atguigu.gmall1213.product.client.ProductFeignClient;
import com.atguigu.gmall1213.user.client.UserFeignClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/order")
public class OrderApiController {
    @Autowired
    private UserFeignClient userFeignClient;

    @Autowired
    private CartFeignClient cartFeignClient;

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;


    // 订单 在网关中设置过这个拦截 /api/**/auth/** 必须登录才能访问
    @GetMapping("auth/trade")
    public Result<Map<String, Object>> trade(HttpServletRequest request) {
        // 登录之后的用户Id
        String userId = AuthContextHolder.getUserId(request);
        // 获取用户地址列表 根据用户Id
        List<UserAddress> userAddressList = userFeignClient.findUserAddressListByUserId(userId);
        // 获取送货清单
        List<CartInfo> cartCheckedList = cartFeignClient.getCartCheckedList(userId);
        // 声明一个OrderDetail 集合
        List<OrderDetail> orderDetailList = new ArrayList<>();
        int totalNum = 0;
        // 循环遍历，将数据赋值给orderDetail
        if (!CollectionUtils.isEmpty(cartCheckedList)) {
            // 循环遍历
            for (CartInfo cartInfo : cartCheckedList) {
                // 将cartInfo 赋值给 orderDetail
                OrderDetail orderDetail = new OrderDetail();
                orderDetail.setImgUrl(cartInfo.getImgUrl());
                orderDetail.setOrderPrice(cartInfo.getSkuPrice());
                orderDetail.setSkuName(cartInfo.getSkuName());
                orderDetail.setSkuNum(cartInfo.getSkuNum());
                orderDetail.setSkuId(cartInfo.getSkuId());
                // 计算每个商品的总个数。
                totalNum += cartInfo.getSkuNum();
                // 将每一个orderDeatil 添加到集合中
                orderDetailList.add(orderDetail);
            }
        }
        // 声明一个map 集合来存储数据
        Map<String, Object> map = new HashMap<>();
        // 存储订单明细
        map.put("detailArrayList", orderDetailList);
        // 存储收货地址列表
        map.put("userAddressList", userAddressList);
        // 存储总金额
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderDetailList(orderDetailList);
        // 计算总金额
        orderInfo.sumTotalAmount();
        map.put("totalAmount", orderInfo.getTotalAmount());
        // 存储商品的件数 记录大的商品有多少个
        map.put("totalNum", orderDetailList.size());
        //获取流水号
        String tradeNo = orderService.getTradeNo(userId);
        //将流水号传到页面
        map.put("tradeNo", tradeNo);
        // 计算小件数：
        // map.put("totalNum",totalNum);
        return Result.ok(map);
    }

    //提交订单   http://api.gmall.com/api/order/auth/submitOrder?tradeNo=null
    @PostMapping("/auth/submitOrder")
    public Result submitOrder(@RequestBody OrderInfo orderInfo, HttpServletRequest request) {
        //获取用户id
        String userId = AuthContextHolder.getUserId(request);
        orderInfo.setUserId(Long.parseLong(userId));
        //防止无刷新回退重复提交
        /*http://api.gmall.com/api/order/auth/submitOrder?tradeNo=null
         */
        //获取页面路径中传过来的流水号
        String tradeNo = request.getParameter("tradeNo");
        boolean flag = orderService.checkTradeNo(userId, tradeNo);
        //作比较
        if (!flag) {
            return Result.fail().message("您已提交成功，请不要重复提交！");
        }
        // 优化，使用异步编排来执行      声明一个集合来存储异步编排对象
        List<CompletableFuture> futureList = new ArrayList<>();
        // 创建一个集合对象，来存储异常信息
        List<String> errorList = new ArrayList<>();
        //验证库存  因为，每个商品的信息都在OrderDetailList属性中
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        if (!CollectionUtils.isEmpty(orderDetailList)) {
            for (OrderDetail orderDetail : orderDetailList) {
                //开一个异步编排来验证库存
                CompletableFuture<Void> checkStockCompletableFuture = CompletableFuture.runAsync(() -> {
                    // 调用验证库存的方法
                    boolean result = orderService.checkStock(orderDetail.getSkuId(), orderDetail.getSkuNum());
                    if (!result) {
                        //无货  提示库存不足
                        errorList.add(orderDetail.getSkuName() + "库存不足！");
                    }
                }, threadPoolExecutor);
                // 将验证库存的异步编排对象放入存储异步编排对象的这个集合
                futureList.add(checkStockCompletableFuture);

                // 利用另一个异步编排来验证价格
                CompletableFuture<Void> skuPriceCompletableFuture = CompletableFuture.runAsync(() -> {
                    //验证价格  判断下订单与结算时的价格是否相同  即获取最新价格信息
                    //skuPrice：实时价格
                    BigDecimal skuPrice = productFeignClient.getSkuPrice(orderDetail.getSkuId());
                    //orderDetail.getOrderPrice(): 数据库中的价格信息
                    if (orderDetail.getOrderPrice().compareTo(skuPrice) != 0) {
                        //如果价格有变动，则重新查询
                        //订单的价格来自于购物车，只要将购物车中的价格改了，重新下单即可
                        //调用loadCartCache(),从数据库中查询最新的价格信息
                        cartFeignClient.loadCartCache(userId);
                        errorList.add(orderDetail.getSkuName() + "价格有变动，请重新下单!");
                    }
                }, threadPoolExecutor);
                // 将验证库存的异步编排对象放入存储异步编排对象的这个集合
                futureList.add(skuPriceCompletableFuture);

            }
        }
        //合并线程
        CompletableFuture.allOf(futureList.toArray(new CompletableFuture[futureList.size()])).join();
        //返回页面提示信息
        if (errorList.size() > 0) {
            // 获取异常集合的数据
            return Result.fail().message(StringUtils.join(errorList, ","));
        }
//        if (!CollectionUtils.isEmpty(orderDetailList)) {
//            for (OrderDetail orderDetail : orderDetailList) {
//                //调用验证库存的方法
//                boolean result = orderService.checkStock(orderDetail.getSkuId(), orderDetail.getSkuNum());
//                if (!result) {
//                    //无货  提示库存不足
//                    return Result.fail().message(orderDetail.getSkuName() + "库存不足！");
//                }
//                //验证价格  判断下订单与结算时的价格是否相同  即获取最新价格信息
//                //skuPrice：实时价格
//                BigDecimal skuPrice = productFeignClient.getSkuPrice(orderDetail.getSkuId());
//                //orderDetail.getOrderPrice(): 数据库中的价格信息
//                if (orderDetail.getOrderPrice().compareTo(skuPrice) != 0) {
//                    //如果价格有变动，则重新查询
//                    //订单的价格来自于购物车，只要将购物车中的价格改了，重新下单即可
//                    //调用loadCartCache(),从数据库中查询最新的价格信息
//                    cartFeignClient.loadCartCache(userId);
//                    return Result.fail().message(orderDetail.getSkuName() + "价格有变动，请重新下单!");
//                }
//            }
//        }
        //比较成功后，删除缓存中的流水号
        orderService.deleteTradeNo(userId);
        Long orderId = orderService.saveOrderInfo(orderInfo);
        //返回订单编号
        return Result.ok(orderId);
    }

    //根据订单Id 查询订单对象
    @GetMapping("inner/getOrderInfo/{orderId}")
    public OrderInfo getOrderInfo(@PathVariable Long orderId) {
        OrderInfo orderInfo = orderService.getOrderInfo(orderId);
        return orderInfo;
    }

    //拆单接口   http://order.gmall.com/orderSplit?orderId=xxx&wareSkuMap=xxx
    @RequestMapping("/orderSplit")
    public String orderSplit(@RequestParam("orderId") String orderId, @RequestParam("wareSkuMap") String wareSkuMap) {
        //参考接口文档
        /*
          orderId   订单系统的订单ID
          wareSkuMap   仓库编号与商品的对照关系:例如
                        [{"wareId":"1","skuIds":["2","10"]},{"wareId":"2","skuIds":["3"]}]
                        表示：sku为2号，10号的商品在1号仓库
                         sku为3号，10号的商品在2号仓库
         */
        //原始订单拆分后就能得到子订单集合
        List<OrderInfo> subOrderInfoList = orderService.orderSplit(Long.parseLong(orderId), wareSkuMap);
        ArrayList<Map> mapList = new ArrayList<>();
        for (OrderInfo orderInfo : subOrderInfoList) {
            //将部分数据转为map
            Map map = orderService.initWareOrder(orderInfo);
            mapList.add(map);
        }
        //将子订单中的部分数据转为json
        return JSON.toJSONString(mapList);
    }

    // 封装秒杀订单数据 控制器可以从页面获取到！
    // 将前台获取到json 字符串变为java 对象
    @PostMapping("inner/seckill/submitOrder")
    public Long submitOrder(@RequestBody OrderInfo orderInfo) {
        // 调用保存订单方法
        Long orderId = orderService.saveOrderInfo(orderInfo);
        // 返回订单Id
        return orderId;
    }
}
