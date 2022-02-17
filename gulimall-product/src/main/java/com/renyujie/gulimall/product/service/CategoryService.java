package com.renyujie.gulimall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.renyujie.common.utils.PageUtils;
import com.renyujie.gulimall.product.entity.CategoryEntity;
import com.renyujie.gulimall.product.vo.Catelog2Vo;

import java.util.List;
import java.util.Map;

/**
 * 商品三级分类
 *
 * @author renyujie518
 * @email renyujie518@gmail.com
 * @date 2022-01-20 17:33:11
 */
public interface CategoryService extends IService<CategoryEntity> {

    PageUtils queryPage(Map<String, Object> params);

    /**
     * @Description: 查出所有分类以及子分类，以树形结构组装起来
     */
    List<CategoryEntity> listWithTree();

    /**
     * @Description: 删除目录
     */
    void removeCategoryByIds(List<Long> asList);

    /**
     * @Description: 根据当前所属分类catelogId找到完整路径 [父,子，孙] 比如[2,25,225]
     */
    Long[] findCatelogPath(Long catelogId);


    /**
     * @Description: 级联更新 所有数据
     * 比如gms_category中的category_name字段变化也更新pms_category_brand_relation表中的category_name字段
     */
    void updateCascade(CategoryEntity category);

    /**
     * @Description: "首页"获取一级分类
     */
    List<CategoryEntity> getLevel1Catrgorys();


    /**
     * @Description: "首页"查出所有分类
     */
    Map<String, List<Catelog2Vo>> getCatalogJson();
}

