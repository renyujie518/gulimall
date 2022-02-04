package com.renyujie.gulimall.product.dao;

import com.renyujie.gulimall.product.entity.CategoryBrandRelationEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 品牌分类关联
 * 
 * @author renyujie518
 * @email renyujie518@gmail.com
 * @date 2022-01-20 17:33:10
 */
@Mapper
public interface CategoryBrandRelationDao extends BaseMapper<CategoryBrandRelationEntity> {
    /**
     * @Description: gms_category中的category_name字段变化也更新pms_category_brand_relation表中的category_name字段
     */
    void updateCategoryNameFromCategoryChange(@Param("catId") Long catId, @Param("name") String name);
}
