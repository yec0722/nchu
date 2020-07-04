package com.atguigu.gmall1213.item;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;


//exclude = DataSourceAutoConfiguration.class 表示不与数据库进行连接
//service-item 这个项目中有数据库链接jar 。如果不配置数据库链接则项目会出错。
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)//取消数据源自动配置
@ComponentScan({"com.atguigu.gmall1213"})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = {"com.atguigu.gmall1213"})
public class ServiceItemApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceItemApplication.class, args);
    }

}
