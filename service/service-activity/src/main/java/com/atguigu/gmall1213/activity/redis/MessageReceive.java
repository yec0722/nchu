package com.atguigu.gmall1213.activity.redis;

import com.atguigu.gmall1213.activity.util.CacheHelper;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class MessageReceive {
    /**
     * 接收消息的方法
     */
    public void receiveMessage(String message) {
        System.out.println("----------收到消息了message：" + message);
        if (!StringUtils.isEmpty(message)) {
            /*
             消息格式
                skuId:0 表示没有秒杀商品
                skuId:1 表示有秒杀商品
             */
            message = message.replaceAll("\"", "");
            String[] split = StringUtils.split(message, ":");
//            String[] split = message.split(":");

            // 无论你的数据是否正确，都会将状态位放入内存！
            if (split == null || split.length == 2) {
                //CacheHelper的本质就是hashMap   将skuId:1存入map中
                //split[0]:skuId      split[1]:状态位
                CacheHelper.put(split[0], split[1]);
            }
        }
    }

}
