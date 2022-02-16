package com.renyujie.gulimall.product.dao;

import com.renyujie.gulimall.product.entity.AttrEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商品属性
 * 
 * @author renyujie518
 * @email renyujie518@gmail.com
 * @date 2022-01-20 17:33:12
 */
@Mapper
public interface AttrDao extends BaseMapper<AttrEntity> {
    /**
     *
     * @Description: 在指定的所有属性集合attrIds里 挑出允许检索属性（search_type=1）的attrIds
     */
    List<Long> selectSearchAttrIds(@Param("attrIds") List<Long> attrIds);
}
