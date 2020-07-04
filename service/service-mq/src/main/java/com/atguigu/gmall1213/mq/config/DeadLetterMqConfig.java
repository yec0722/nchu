package com.atguigu.gmall1213.mq.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;


@Configuration
public class DeadLetterMqConfig {
    //声明一些常量
    public static final String exchange_dead = "exchange.dead";
    public static final String routing_dead_1 = "routing.dead.1";
    public static final String routing_dead_2 = "routing.dead.2";
    public static final String queue_dead_1 = "queue.dead.1";
    public static final String queue_dead_2 = "queue.dead.2";

    //定义交换机
    @Bean
    public DirectExchange exchange() {
        //durable:  是否要做持久化
        return new DirectExchange(exchange_dead, true, false, null);
    }

    //定义一个队列
    @Bean
    public Queue queue1() {
        //配置相关参数
        HashMap<String, Object> map = new HashMap<>();
        //参数绑定  此处的key是固定值   不能随意写
        //如果队列一出现问题，则通过参数转到exchange_dead上，通过routing_dead_2转到其他队列
        map.put("x-dead-letter-exchange", exchange_dead);
        map.put("x-dead-letter-routing-key", routing_dead_2);
        //统一配置延迟时间
        map.put("x-message-ttl", 10 * 1000);
        return new Queue(queue_dead_1, true, false, false, map);
    }

    //定义一个队列一的绑定关系
    @Bean
    public Binding binding() {
        //将队列一通过routing_dead_1 绑定到exchange_dead
        return BindingBuilder.bind(queue1()).to(exchange()).with(routing_dead_1);
    }

    @Bean
    public Queue queue2() {
        return new Queue(queue_dead_2, true, false, false, null);
    }

    //队列二的绑定规则
    @Bean
    public Binding deadBinding() {
        return BindingBuilder.bind(queue2()).to(exchange()).with(routing_dead_2);
    }

}
