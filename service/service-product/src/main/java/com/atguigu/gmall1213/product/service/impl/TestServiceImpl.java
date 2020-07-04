package com.atguigu.gmall1213.product.service.impl;

import com.atguigu.gmall1213.product.service.TestService;
import com.baomidou.mybatisplus.extension.api.R;
import org.apache.commons.lang.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class TestServiceImpl implements TestService {

    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;

    //测试redisson
    @Override
    public void testLock() {
        String skuId = "30";
        String lockKey = "sku" + skuId + "lock";
        RLock lock = redissonClient.getLock(lockKey);
        lock.lock();
        String num = redisTemplate.opsForValue().get("num");
        if (StringUtils.isEmpty(num)) {
            return;
        }
        int number = Integer.parseInt(num);
        redisTemplate.opsForValue().set("num", String.valueOf(++number));
        lock.unlock();
    }


    //测试锁
//    @Override
//    public void testLock() {
//        //相当于setnx("lock","atguigu")  当key不存在的时候才会生效
////        Boolean lock = redisTemplate.opsForValue().setIfAbsent("lock", "atguigu");
////        Boolean lock = redisTemplate.opsForValue().setIfAbsent("lock", "atguigu", 1, TimeUnit.SECONDS);
//        //防止误删其他的锁
//        String uuid = UUID.randomUUID().toString();
//        Boolean lock = redisTemplate.opsForValue().setIfAbsent("lock", uuid, 1, TimeUnit.SECONDS);
//        if (lock) {
//            //上锁
//            //获取缓存中的key=num
//            String num = redisTemplate.opsForValue().get("num");
//            if (StringUtils.isEmpty(num)) {
//                return;
//            }
//            int number = Integer.parseInt(num);
//            redisTemplate.opsForValue().set("num", String.valueOf(++number));
//            if (uuid.equals(redisTemplate.opsForValue().get("lock"))) {
//                //资源操作完成后删除锁
//                redisTemplate.delete("lock");
//            }
//
//        } else {
//            //等待
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            //重新调用
//            testLock();
//        }
//    }

    @Override
    public String readLock() {
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock("readwriteLock");
        RLock rLock = readWriteLock.readLock(); // 获取读锁
        rLock.lock(10, TimeUnit.SECONDS); // 加10s锁
        String msg = this.redisTemplate.opsForValue().get("msg");
        //rLock.unlock(); // 解锁
        return msg;
    }

    @Override
    public String writeLock() {
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock("readwriteLock");
        RLock rLock = readWriteLock.writeLock(); // 获取写锁
        rLock.lock(10, TimeUnit.SECONDS); // 加10s锁
        this.redisTemplate.opsForValue().set("msg", UUID.randomUUID().toString());
        //rLock.unlock(); // 解锁
        return "成功写入了内容。。。。。。";

    }
}
