package com.atguigu.gmall1213.user.service;


import com.atguigu.gmall1213.model.user.UserAddress;

import java.util.List;

public interface UserAddressService {
    //根据用户id查询用户收货地址
    List<UserAddress> findUserAddressListByUserId(String userId);
}
