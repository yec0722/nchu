package com.atguigu.gmall1213.product.mapper;

import com.atguigu.gmall1213.model.product.BaseTrademark;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Component;

@Component
@Mapper
public interface BaseTrademarkMapper extends BaseMapper<BaseTrademark> {
}
