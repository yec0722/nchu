package com.atguigu.gmall1213.mq.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.CustomExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class DelayedMqConfig {
    public static final String exchange_delay = "exchange.delay";
    public static final String routing_delay = "routing.delay";
    public static final String queue_delay_1 = "queue.delay.1";

    @Bean
    public Queue delayQeue() {
        //返回队列
        return new Queue(queue_delay_1, true);
    }

    //设置交换机
    @Bean
    public CustomExchange customExchange() {
        Map<String, Object> map = new HashMap<String, Object>();
        //固定写法  配置参数
        map.put("x-delayed-type", "direct");
        return new CustomExchange(exchange_delay, "x-delayed-message", true, false, map);
    }

    //设置绑定规则
    @Bean
    public Binding delayBbinding1() {
        //noargs():基于插件
        return BindingBuilder.bind(delayQeue()).to(customExchange()).with(routing_delay).noargs();
    }
}
