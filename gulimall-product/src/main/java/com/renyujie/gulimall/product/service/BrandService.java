package com.renyujie.gulimall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.renyujie.common.utils.PageUtils;
import com.renyujie.gulimall.product.entity.BrandEntity;

import java.util.List;
import java.util.Map;

/**
 * 品牌
 *
 * @author renyujie518
 * @email renyujie518@gmail.com
 * @date 2022-01-20 17:33:11
 */
public interface BrandService extends IService<BrandEntity> {

    PageUtils queryPage(Map<String, Object> params);

    /**
     * @Description: 根据id获取品牌信息
     */
    List<BrandEntity> getBrandsByIds(List<Long> brandIds);

    /**
     * @Description: 在品牌变化时  如brandName变化时  其他与品牌做了关联的表
     * 如pms_category_brand_relation表  里的brand_name字段  应该也变化
     */
    void updateDetail(BrandEntity brand);
}

