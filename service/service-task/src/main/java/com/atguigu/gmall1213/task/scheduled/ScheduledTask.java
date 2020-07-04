package com.atguigu.gmall1213.task.scheduled;

import com.atguigu.gmall1213.common.constant.MqConst;
import com.atguigu.gmall1213.common.service.RabbitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling     //开启定时任务的注解
public class ScheduledTask {
    @Autowired
    private RabbitService rabbitService;

    /*
    编写一个定时任务
     */
    //cron:定时任务表达式
    // @Scheduled(cron = "0 0 1 * * ?")    每天凌晨一点钟发送一个消息
    //  0/30:每隔30S发送一个消息
    // 编写定时任务 cron 定时任务表达式
    // 每天凌晨1点钟发送一个消息。 0 0 1 * * ?
    // 每隔30秒执行一次
    @Scheduled(cron = "0/5 * * * * ?")
    public void taskActivity() {
        // System.out.println("定时任务来了。。。。。。");
        rabbitService.sendMessage(
                MqConst.EXCHANGE_DIRECT_TASK,
                MqConst.ROUTING_TASK_1, "");
    }

    //每天定时清空缓存数据
    @Scheduled(cron = "0 0 18 * * ?")
    public void task18() {
        // System.out.println("定时任务来了。。。。。。");
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_TASK, MqConst.ROUTING_TASK_18, "");
    }
}
