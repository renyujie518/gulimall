package com.renyujie.gulimall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.renyujie.common.utils.PageUtils;
import com.renyujie.gulimall.product.entity.CategoryBrandRelationEntity;

import java.util.List;
import java.util.Map;

/**
 * 品牌分类关联
 *
 * @author renyujie518
 * @email renyujie518@gmail.com
 * @date 2022-01-20 17:33:10
 */
public interface CategoryBrandRelationService extends IService<CategoryBrandRelationEntity> {

    PageUtils queryPage(Map<String, Object> params);

    /**
     * @Description: 在pms_category_brand_relation表中依据 brandId查询该行数据
     */
    List<CategoryBrandRelationEntity> findCategory2BrandRelation(Long brandId);

    /**
     * @Description: 保存详细细节
     * 因为做了冗余设置 每次在前端新建"关联分类"时要从brand和category表中查到对应的name填到pms_category_brand_relation表中
     */
    void saveDetial(CategoryBrandRelationEntity categoryBrandRelation);

    /**
     * @Description: gms_brand中的brand_name字段变化也更新pms_category_brand_relation表中的brand_name字段
     */
    void updateBrandNameFromBrandChange(Long brandId, String name);

    /**
     * @Description: gms_category中的category_name字段变化也更新pms_category_brand_relation表中的category_name字段
     */
    void updateCategoryNameFromCategoryChange(Long catId, String name);
}

