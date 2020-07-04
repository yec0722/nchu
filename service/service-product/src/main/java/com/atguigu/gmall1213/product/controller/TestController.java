package com.atguigu.gmall1213.product.controller;

import com.atguigu.gmall1213.common.result.Result;
import com.atguigu.gmall1213.product.service.TestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("admin/product/test")
public class TestController {

    // 创建一个接口对象
    @Autowired
    private TestService testService;

    @GetMapping("/testLock")
    public Result testLock(){
        // 调用锁的方法
        testService.testLock();

        return Result.ok();
    }

    @GetMapping("read")
    public Result<String> read(){
        String msg = testService.readLock();

        return Result.ok(msg);
    }

    @GetMapping("write")
    public Result<String> write(){
        String msg = testService.writeLock();

        return Result.ok(msg);
    }

}
