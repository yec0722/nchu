package com.atguigu.gmall1213.product.service;

import com.atguigu.gmall1213.model.product.BaseTrademark;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;


public interface BaseTrademarkService extends IService<BaseTrademark>{
    // 分页查询品牌数据
    IPage<BaseTrademark> selectPage(Page<BaseTrademark> baseTrademarkPage);

    List<BaseTrademark> getTrademarkList();
}
