package com.atguigu.gmall1213.item.service;

import java.util.Map;

public interface ItemService {

    //通过SkuId获取数据，并存入map集合
    Map<String, Object> getBySkuId(Long skuId);
}
