package com.atguigu.gmall1213.product.service;

public interface TestService {

    // 测试锁
    void testLock();

    String readLock();

    String writeLock();
}
