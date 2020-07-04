package com.atguigu.gmall1213.product.controller;

import com.atguigu.gmall1213.common.result.Result;
import com.atguigu.gmall1213.model.product.*;
import com.atguigu.gmall1213.product.service.ManageService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api("后台接口测试")
@RestController
@RequestMapping("/admin/product")
//@CrossOrigin
public class BaseManageController {
    @Autowired
    private ManageService manageService;

    //查询一级分类id
    @GetMapping("/getCategory1")
    public Result<List<BaseCategory1>> getCategory1() {
        List<BaseCategory1> category1List = manageService.getCategory1();
        return Result.ok(category1List);
    }

    //查询二级分类id
    @GetMapping("/getCategory2/{category1Id}")
    public Result<List<BaseCategory2>> getCategory2(@PathVariable("category1Id") Long category1Id) {
        List<BaseCategory2> category2List = manageService.getCategory2(category1Id);
        return Result.ok(category2List);
    }

    ////查询三级分类id
    @GetMapping("/getCategory3/{category2Id}")
    public Result<List<BaseCategory3>> getCategory3(@PathVariable("category2Id") Long category2Id) {
        List<BaseCategory3> category3List = manageService.getCategory3(category2Id);
        return Result.ok(category3List);
    }

    //查询平台属性
    @GetMapping("/attrInfoList/{category1Id}/{category2Id}/{category3Id}")
    public Result<List<BaseAttrInfo>> getAttrInfoList(@PathVariable Long category1Id,
                                                      @PathVariable Long category2Id,
                                                      @PathVariable Long category3Id) {
        List<BaseAttrInfo> attrInfoList = manageService.getAttrInfoList(category1Id, category2Id, category3Id);
        return Result.ok(attrInfoList);
    }

    @PostMapping("/saveAttrInfo")
    /*
     * 为什么选用BaseAttrInfo这个类做接收从前台传过来的值，这是因为BaseAttrInfo这个类既有
     * 平台属性的数据，又有平台属性值的数据，可以做合并
     * */
    public Result saveAttrInfo(@RequestBody BaseAttrInfo baseAttrInfo) {
        //调用保存方法
        manageService.saveAttrInfo(baseAttrInfo);
        return Result.ok();
    }

    //修改平台属性，即根据平台属性id获取平台属性数据
    //先查询属性，如果有平台属性才有平台属性值
    //平台属性:平台属性值  1:n的关系
    @GetMapping("/getAttrValueList/{attrId}")
    public Result<List<BaseAttrValue>> getAttrValueList(@PathVariable("attrId") Long attrId) {
//        List<BaseAttrValue> baseAttrValueList = manageService.getAttrValueList(attrId);
//        return Result.ok();

        //先查询属性，如果有平台属性才有平台属性值
        //AttrInfo的id=AttrInfoValue的attr_id
        BaseAttrInfo baseAttrInfo = manageService.getAttrInfo(attrId);
        List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
        return Result.ok(attrValueList);
    }

    /*
     * http://api.gmall.com/admin/product/ {page}/{limit}?category3Id=61
     */
    //分页查询
    @GetMapping("{page}/{limit}")
    public Result getPageList(@PathVariable Long page,
                              @PathVariable Long limit,
                              SpuInfo spuInfo) {
        Page<SpuInfo> spuInfoPage = new Page<>(page, limit);
        IPage<SpuInfo> spuInfoIPageList = manageService.selectPage(spuInfoPage, spuInfo);
        return Result.ok(spuInfoIPageList);
    }
}
