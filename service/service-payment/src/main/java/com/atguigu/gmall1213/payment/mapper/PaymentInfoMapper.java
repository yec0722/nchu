package com.atguigu.gmall1213.payment.mapper;

import com.atguigu.gmall1213.model.payment.PaymentInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Component;

@Component
@Mapper
public interface PaymentInfoMapper extends BaseMapper<PaymentInfo> {
}
