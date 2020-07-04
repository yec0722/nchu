package com.atguigu.gmall1213.order.receiver;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall1213.common.constant.MqConst;
import com.atguigu.gmall1213.common.service.RabbitService;
import com.atguigu.gmall1213.model.enums.PaymentStatus;
import com.atguigu.gmall1213.model.enums.ProcessStatus;
import com.atguigu.gmall1213.model.order.OrderInfo;
import com.atguigu.gmall1213.model.payment.PaymentInfo;
import com.atguigu.gmall1213.order.service.OrderService;
import com.atguigu.gmall1213.payment.client.PaymentFeignClient;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class OrderReceiver {
    @Autowired
    private OrderService orderService;
    @Autowired
    private PaymentFeignClient paymentFeignClient;
    @Autowired
    private RabbitService rabbitService;

    // 监听消息时获取订单Id
    @SneakyThrows
    @RabbitListener(queues = MqConst.QUEUE_ORDER_CANCEL)
    //这里的绑定关系在OrderCanelMqConfig配置类中已经声明好了
    public void orderCancel(Long orderId, Message message, Channel channel) {
        // 判断订单Id 是否为空！
        // 这里不止要关闭电商平台的交易记录，还需要关闭支付宝的交易记录。
        if (null != orderId) {
            // 为了防止重复消息这个消息。判断订单状态
            // 通过订单Id 来获取订单对象 select * from orderInfo where id = orderId
            OrderInfo orderInfo = orderService.getById(orderId);
            // 涉及到关闭orderInfo ,paymentInfo ,aliPay
            // 订单状态:未支付
            if (null != orderInfo && orderInfo.getOrderStatus().equals(ProcessStatus.UNPAID.getOrderStatus().name())) {
                // 关闭过期订单
                // orderService.execExpiredOrder(orderId);
                // 订单创建时就是未付款，判断是否有交易记录产生
                PaymentInfo paymentInfo = paymentFeignClient.getPaymentInfo(orderInfo.getOutTradeNo());
                if (null != paymentInfo && paymentInfo.getPaymentStatus().equals(PaymentStatus.UNPAID.name())) {
                    // 先查看是否有交易记录       即用户是否扫了二维码
                    Boolean aBoolean = paymentFeignClient.checkPayment(orderId);
                    if (aBoolean) {
                        // 有交易记录 ，关闭支付宝         防止用户在过期时间到的一个瞬间，付款。
                        Boolean flag = paymentFeignClient.closePay(orderId);
                        if (flag) {
                            // 用户未付款,关闭支付宝交易成功    开始关闭订单，关闭交易记录              2:表示要关闭交易
                            orderService.execExpiredOrder(orderId, "2");
                        } else {
                            // 用户已经付款,关闭支付宝交易失败      支付成功之后，监听消息通知订单更新订单状态、减库存
                            rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_PAYMENT_PAY, MqConst.ROUTING_PAYMENT_PAY, orderId);
                        }
                    } else {
                        // 在支付宝中没有交易记录
                        orderService.execExpiredOrder(orderId, "2");
                    }
                } else {
                    //没有订单 也就是说在paymentInfo 中根本没有交易记录。
                    orderService.execExpiredOrder(orderId, "1");
                }
            }
        }
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }

    // 支付成功之后，监听消息通知订单更新订单状态、减库存
    @SneakyThrows
    @RabbitListener(bindings =
    @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_PAYMENT_PAY, durable = "true"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_PAYMENT_PAY),
            key = {MqConst.ROUTING_PAYMENT_PAY}
    ))
    public void updOrder(Long orderId, Message message, Channel channel) {
        // 判断orderId 不为空
        if (null != orderId) {
            // 更新订单的状态，还有进度的状态
            OrderInfo orderInfo = orderService.getById(orderId);
            // 判断状态
            if (null != orderInfo && orderInfo.getOrderStatus().equals(ProcessStatus.UNPAID.getOrderStatus().name())) {
                //如果是PAID或CLOSED，那么已经交易完成了，也不需要减库存
                //准备更新数据
                orderService.updateOrderStatus(orderId, ProcessStatus.PAID);
                // 支付成功之后发送消息通知库存，准备减库存！
                orderService.sendOrderStatus(orderId);
            }
        }
        // 手动确认
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }

    //监听减库存的消息队列   更改货物状态
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_WARE_ORDER, durable = "true"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_WARE_ORDER),
            key = {MqConst.ROUTING_WARE_ORDER}
    ))
    public void updOrderStatus(String msgJson, Message message, Channel channel) {
        if (StringUtils.isNotEmpty(msgJson)) {
            //获取传过来的数据
            Map map = JSON.parseObject(msgJson, Map.class);
            String orderId = (String) map.get("orderId");
            String status = (String) map.get("status");
            if ("DEDUCTED".equals(status)) {
                orderService.updateOrderStatus(Long.parseLong(orderId), ProcessStatus.WAITING_DELEVER);
            } else {
                //库存超卖
                orderService.updateOrderStatus(Long.parseLong(orderId), ProcessStatus.STOCK_EXCEPTION);
            }
        }
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }
}
