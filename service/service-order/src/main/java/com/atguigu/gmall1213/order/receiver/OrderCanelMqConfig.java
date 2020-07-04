package com.atguigu.gmall1213.order.receiver;

import com.atguigu.gmall1213.common.constant.MqConst;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.CustomExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class OrderCanelMqConfig {

    //创建一个队列
    @Bean
    public Queue delayQueueOrder() {
        // 第一个参数是创建的queue的名字，第二个参数是是否支持持久化
        return new Queue(MqConst.QUEUE_ORDER_CANCEL, true);
    }

    //创建一个交换机
    @Bean
    public CustomExchange customExchange() {
        Map<String, Object> map = new HashMap<String, Object>();
        //固定写法  配置参数
        map.put("x-delayed-type", "direct");
        return new CustomExchange(MqConst.EXCHANGE_DIRECT_ORDER_CANCEL, "x-delayed-message", true, false, map);
    }

    //设置绑定规则
    @Bean
    public Binding delayBbinding1() {
        //noargs():基于插件
        return BindingBuilder.bind(delayQueueOrder()).to(customExchange()).with(MqConst.ROUTING_ORDER_CANCEL).noargs();
    }
}
