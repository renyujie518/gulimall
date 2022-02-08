package com.renyujie.gulimall.coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.renyujie.common.dto.SkuReductionTo;
import com.renyujie.common.utils.PageUtils;
import com.renyujie.gulimall.coupon.entity.SkuFullReductionEntity;

import java.util.Map;

/**
 * 商品满减信息
 *
 * @author renyujie518
 * @email renyujie518@gmail.com
 * @date 2022-01-20 20:49:31
 */
public interface SkuFullReductionService extends IService<SkuFullReductionEntity> {

    PageUtils queryPage(Map<String, Object> params);

    /**
     * @Description: "发布商品"的最终大保存会远程调用此方法用于  保存sku的优惠、满减等信息
     */
    void saveSkuReduction(SkuReductionTo skuReductionTo);
}

