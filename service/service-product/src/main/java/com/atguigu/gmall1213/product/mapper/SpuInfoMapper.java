package com.atguigu.gmall1213.product.mapper;

import com.atguigu.gmall1213.model.product.SpuInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Component;

@Mapper
@Component
public interface SpuInfoMapper extends BaseMapper<SpuInfo> {
}
