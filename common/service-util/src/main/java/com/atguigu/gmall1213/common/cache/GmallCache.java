package com.atguigu.gmall1213.common.cache;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)  //表明这个注解在方法上使用
@Retention(RetentionPolicy.RUNTIME)   //表示这个注解的生命周期
public @interface GmallCache {
    // 表示前缀
    String prefix() default "cache";
}
