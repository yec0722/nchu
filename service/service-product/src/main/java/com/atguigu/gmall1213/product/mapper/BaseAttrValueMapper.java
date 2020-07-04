package com.atguigu.gmall1213.product.mapper;

import com.atguigu.gmall1213.model.product.BaseAttrInfo;
import com.atguigu.gmall1213.model.product.BaseAttrValue;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Component;

@Component
@Mapper
public interface BaseAttrValueMapper extends BaseMapper<BaseAttrValue> {
}
