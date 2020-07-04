package com.atguigu.gmall1213.common.cache;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall1213.common.constant.RedisConst;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Component
@Aspect
//这个类的作用是将带有@GmallCache 这个注解的方法的返回值查出来并缓存到redis中
public class GmallCacheAspect {
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;

    // 编写一个环绕通知

    // 根据注解在哪，如果注解在getSkuInfo(Long skuId) skuId =30
    // point.getArgs() 相当于获取getSkuInfo()方法体上的参数。
    // point.proceed(point.getArgs()); 表示执行带有@GmallCache 注解的方法的方法体
    @Around("@annotation(com.atguigu.gmall1213.common.cache.GmallCache)")
    public Object cacheAroundAdvice(ProceedingJoinPoint point) throws Throwable {
        //因为切入点的返回值类型是不同的，因此只能将返回值做成共通的
        Object result = null;
        //获取到方法(切入点)中的参数
        Object[] args = point.getArgs();
        //获取方法 上的注解
        MethodSignature signature = (MethodSignature) point.getSignature();
        GmallCache gmallCache = signature.getMethod().getAnnotation(GmallCache.class);
        //获取注解中的prefix
        String prefix = gmallCache.prefix();
        //定义一个key
        String key = prefix + Arrays.asList(args).toString();
        /*
        将数据存储到缓存中   cacheHit(signature, key)
         */
        //表示根据key获取缓存中返回的数据
        result = cacheHit(signature, key);
        //判断从缓存中是否获取到了数据
        if (result != null) {
            //不为空，获取到了数据，并返回
            return result;
        }
        //获取到的数据为空，走数据库，查完后并放入缓存
        //自定义一个锁
        RLock lock = redissonClient.getLock(key + ":lock");
        try {
            boolean res = lock.tryLock(RedisConst.SKULOCK_EXPIRE_PX2, RedisConst.SKULOCK_EXPIRE_PX1, TimeUnit.SECONDS);
            //上锁成功
            try {
                if (res) {
                    //把方法中的参数(point.getArgs())传进去   相当于执行了该方法的方法体，并能够得到一个返回值
                    // 根据注解在哪，如果注解在getSkuInfo(Long skuId) skuId =30
                    // point.getArgs() 相当于获取getSkuInfo()方法体上的参数。
                    // point.proceed(point.getArgs()); 表示执行带有@GmallCache 注解的方法的方法体
                    result = point.proceed(point.getArgs()); //result 走数据库得到的值
                    if (result == null) {
                        //说明数据库中这个数据压根没有  然后缓存一个空对象进去  防止缓存穿透
                        Object o = new Object();
                        //将数据放到缓存中
                        //JSON.toJSONString(o)  将object转为json字符串
                        redisTemplate.opsForValue().set(key, JSON.toJSONString(o), RedisConst.SKUKEY_TEMPORARY_TIMEOUT, TimeUnit.SECONDS);
                        return o;
                    }
                    //从数据库中查到了 并放入缓存中
                    redisTemplate.opsForValue().set(key, JSON.toJSONString(result), RedisConst.SKUKEY_TIMEOUT, TimeUnit.SECONDS);
                    //返回数据
                    return result;
                }
                //上锁失败
                Thread.sleep(1000);
                //继续获取数据
                return cacheHit(signature, key);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            } finally {
                //解锁
                lock.unlock();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    //这个方法表示从缓存中获取数据
    private Object cacheHit(MethodSignature signature, String key) {
        //根据key从缓存中获取数据
        //因为前面代码中做了JSON.toJSONString(o)  将object转为json字符串  因此可以确定返回值类型了
        String object = (String) redisTemplate.opsForValue().get(key);
        if (null != object) {
            //表示缓存中有数据，并要获取返回值的数据类型
            //返回每个方法自己的数据类型
            Class returnType = signature.getReturnType();
            //将参数object转为每个方法自己的参数类型
            return JSON.parseObject(object, returnType);
        }
        //缓存中无数据
        return null;
    }
}
