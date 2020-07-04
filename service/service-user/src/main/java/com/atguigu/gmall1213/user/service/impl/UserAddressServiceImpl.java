package com.atguigu.gmall1213.user.service.impl;

import com.atguigu.gmall1213.model.user.UserAddress;
import com.atguigu.gmall1213.user.mapper.UserAddressMapper;
import com.atguigu.gmall1213.user.service.UserAddressService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserAddressServiceImpl implements UserAddressService {
    @Autowired
    private UserAddressMapper userAddressMapper;

    //根据用户id查询用户收货地址
    @Override
    public List<UserAddress> findUserAddressListByUserId(String userId) {
        QueryWrapper<UserAddress> userAddressQueryWrapper = new QueryWrapper<>();
        userAddressQueryWrapper.eq("user_id", userId);
        List<UserAddress> userAddresses = userAddressMapper.selectList(userAddressQueryWrapper);
        return userAddresses;
    }
}
