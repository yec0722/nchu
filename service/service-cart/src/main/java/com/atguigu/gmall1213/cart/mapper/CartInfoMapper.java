package com.atguigu.gmall1213.cart.mapper;

import com.atguigu.gmall1213.model.cart.CartInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Component;

@Component
@Mapper
public interface CartInfoMapper extends BaseMapper<CartInfo> {
}
