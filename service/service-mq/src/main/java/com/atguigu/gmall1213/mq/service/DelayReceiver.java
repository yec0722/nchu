package com.atguigu.gmall1213.mq.service;


import com.atguigu.gmall1213.mq.config.DelayedMqConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

//延迟插件测试接收消息
@Component
@Configuration
public class DelayReceiver {
    //监听消息
    @RabbitListener(queues = DelayedMqConfig.queue_delay_1)
    public void get(String msg) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        System.out.println("监听到的消息 queue_delay_1: " + "接收的时间" + sdf.format(new Date()) + msg);
    }

}
