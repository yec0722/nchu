package com.atguigu.gmall1213.list.receiver;

import com.atguigu.gmall1213.common.constant.MqConst;
import com.atguigu.gmall1213.list.service.SearchService;
import lombok.SneakyThrows;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;

@Component
public class ListReceiver {
    @Autowired
    private SearchService searchService;

    //使用注解来监听消息
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_GOODS_UPPER, durable = "true"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_GOODS),
            key = {MqConst.ROUTING_GOODS_UPPER}
    ))
    //监听商品上架
    public void upperGoods(Long skuId, Message message, Channel channel) {
        if (null != skuId) {
            searchService.upperGoods(skuId);
        }
        //手动确认
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }


    //使用注解来监听消息
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_GOODS_LOWER, durable = "true"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_GOODS),
            key = {MqConst.ROUTING_GOODS_LOWER}
    ))
    //监听商品下架
    public void lowerGoods(Long skuId, Message message, Channel channel) {
        if (null != skuId) {
            searchService.lowerGoods(skuId);
        }
        //手动确认
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }
}
