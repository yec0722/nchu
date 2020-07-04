package com.atguigu.gmall1213.order.mapper;

import com.atguigu.gmall1213.model.order.OrderInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Component;

@Mapper
@Component
public interface OrderInfoMapper extends BaseMapper<OrderInfo> {

}
