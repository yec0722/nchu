package com.atguigu.gmall1213.order.service;

import com.atguigu.gmall1213.model.enums.ProcessStatus;
import com.atguigu.gmall1213.model.order.OrderInfo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

public interface OrderService extends IService<OrderInfo> {

    //保存订单
    Long saveOrderInfo(OrderInfo orderInfo);


    /*
    防止页面回退无刷新重复提交订单
     */
    //获取流水号
    String getTradeNo(String userId);

    //比较流水号
    boolean checkTradeNo(String userId, String tradeNo);

    //删除流水号
    void deleteTradeNo(String userId);

    //验证库存
    boolean checkStock(Long skuId, Integer skuNum);

    //关闭过期订单
    void execExpiredOrder(Long orderId);

    //关闭过期订单后，从数据库中更新订单状态
    void updateOrderStatus(Long orderId, ProcessStatus processStatus);

    //根据订单Id 查询订单对象    getOrderInfo 这个方法可以在里面查询订单明细
    OrderInfo getOrderInfo(Long orderId);

    // 支付成功之后发送消息通知库存，准备减库存！
    void sendOrderStatus(Long orderId);

    //将orderInfo 中的部分属性数据转为map集合
    Map initWareOrder(OrderInfo orderInfo);

    //原始订单拆分后就能得到子订单集合
    List<OrderInfo> orderSplit(Long orderId, String wareSkuMap);

    //关闭过期订单
    void execExpiredOrder(Long orderId, String flag);
}
