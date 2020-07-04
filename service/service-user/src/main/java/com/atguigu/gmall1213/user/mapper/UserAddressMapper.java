package com.atguigu.gmall1213.user.mapper;

import com.atguigu.gmall1213.model.user.UserAddress;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Component;

@Component
@Mapper
public interface UserAddressMapper extends BaseMapper<UserAddress> {
}
