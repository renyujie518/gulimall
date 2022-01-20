package com.renyujie.gulimall.product.dao;

import com.renyujie.gulimall.product.entity.CategoryEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品三级分类
 * 
 * @author renyujie518
 * @email renyujie518@gmail.com
 * @date 2022-01-20 17:33:11
 */
@Mapper
public interface CategoryDao extends BaseMapper<CategoryEntity> {
	
}
