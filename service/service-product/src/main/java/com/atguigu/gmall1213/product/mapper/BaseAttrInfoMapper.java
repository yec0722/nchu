package com.atguigu.gmall1213.product.mapper;

import com.atguigu.gmall1213.model.product.BaseAttrInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Mapper
public interface BaseAttrInfoMapper extends BaseMapper<BaseAttrInfo> {

    List<BaseAttrInfo> selectBaseAttrInfoList(@Param("category1Id") Long category1Id,
                                              @Param("category2Id") Long category2Id,
                                              @Param("category3Id") Long category3Id);

    //根据skuId获取平台属性和平台属性值
    List<BaseAttrInfo> selectAttrInfoList(Long skuId);
}
