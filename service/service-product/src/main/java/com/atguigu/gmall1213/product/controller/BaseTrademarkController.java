package com.atguigu.gmall1213.product.controller;

import com.atguigu.gmall1213.common.result.Result;
import com.atguigu.gmall1213.model.product.BaseTrademark;
import com.atguigu.gmall1213.product.service.BaseTrademarkService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/product/baseTrademark")
public class BaseTrademarkController {
    @Autowired
    private BaseTrademarkService baseTrademarkService;

    @GetMapping("{page}/{limit}")
    //  http://api.gmall.com/admin/product/baseTrademark/{page}/{limit}
    public Result getPageList(@PathVariable("page") Long page,
                              @PathVariable("limit") Long limit) {
        Page<BaseTrademark> baseTrademarkPage = new Page<>(page, limit);
        IPage<BaseTrademark> baseTrademarkIPage = baseTrademarkService.selectPage(baseTrademarkPage);
        return Result.ok(baseTrademarkIPage);
    }

    /*
     http://api.gmall.com/admin/product/baseTrademark/save
     */
    //添加品牌
    @PostMapping("/save")
    public Result save(@RequestBody BaseTrademark baseTrademark) {
        //调用服务处，保存
        baseTrademarkService.save(baseTrademark);
        return Result.ok();
    }

    //修改品牌
    @PutMapping("/update")
    public Result update(@RequestBody BaseTrademark baseTrademark) {
        baseTrademarkService.updateById(baseTrademark);
        return Result.ok();
    }

    //删除品牌
    @DeleteMapping("remove/{id}")
    public Result remove(@PathVariable Long id) {
        baseTrademarkService.removeById(id);
        return Result.ok();
    }

    //根据id查询品牌
    @GetMapping("get/{id}")
    public Result getInfo(@PathVariable Long id) {
        BaseTrademark baseTrademark = baseTrademarkService.getById(id);
        return Result.ok(baseTrademark);
    }

    /*
    http://api.gmall.com/admin/product/baseTrademark/getTrademarkList
    品牌加载
     */
    //查询所有的品牌
    @GetMapping("getTrademarkList")
    public Result getTrademarkList() {
        List<BaseTrademark> baseTrademarkList = baseTrademarkService.getTrademarkList();
        return Result.ok(baseTrademarkList);
    }
}
