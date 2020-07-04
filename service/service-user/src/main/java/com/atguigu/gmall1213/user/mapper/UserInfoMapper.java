package com.atguigu.gmall1213.user.mapper;

import com.atguigu.gmall1213.model.user.UserInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Component;

@Mapper
@Component
public interface UserInfoMapper extends BaseMapper<UserInfo> {
}