package com.atguigu.gmall1213.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/**
 * @author mqx
 * 限流方式在配置文件中只有存在一种！
 * @date 2020/7/4 14:48
 */
@Configuration
public class KeyResolverConfig {
    // ip 限流
    // 配置文件中：key-resolver: "#{@ipKeyResolver}" # 遵循哪个限流规则！
    @Bean
    public KeyResolver ipKeyResolver(){
        System.out.println("使用Ip限流------");
        return exchange -> Mono.just(exchange.getRequest().getRemoteAddress().getHostName());
    }

//    // 用户限流
//    @Bean
//    public KeyResolver userKeyResolver() {
//        System.out.println("用户限流");
//        return exchange -> Mono.just(exchange.getRequest().getHeaders().get("token").get(0));
//    }
//
//    // 接口限流
//    @Bean
//    public KeyResolver apiKeyResolver() {
//        System.out.println("接口限流");
//        return exchange -> Mono.just(exchange.getRequest().getPath().value());
//    }

}
