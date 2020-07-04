package com.atguigu.gmall1213.product.service.impl;

import com.atguigu.gmall1213.model.product.BaseTrademark;
import com.atguigu.gmall1213.product.mapper.BaseTrademarkMapper;
import com.atguigu.gmall1213.product.service.BaseTrademarkService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BaseTrademarkServiceImpl extends ServiceImpl<BaseTrademarkMapper, BaseTrademark> implements BaseTrademarkService {
        @Autowired
    private BaseTrademarkMapper baseTrademarkMapper;

    @Override
    public IPage<BaseTrademark> selectPage(Page<BaseTrademark> baseTrademarkPage) {
        QueryWrapper<BaseTrademark> baseTrademarkQueryWrapper = new QueryWrapper<>();
        baseTrademarkQueryWrapper.orderByDesc("id");
        IPage<BaseTrademark> baseTrademarkIPage = baseTrademarkMapper.selectPage(baseTrademarkPage, baseTrademarkQueryWrapper);
        return baseTrademarkIPage;
    }

    @Override
    public List<BaseTrademark> getTrademarkList() {
        //查询所有品牌
        return baseTrademarkMapper.selectList(null);
    }
}
