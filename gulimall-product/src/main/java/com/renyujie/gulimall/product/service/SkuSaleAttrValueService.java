package com.renyujie.gulimall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.renyujie.common.utils.PageUtils;
import com.renyujie.gulimall.product.entity.SkuSaleAttrValueEntity;
import com.renyujie.gulimall.product.vo.SkuItemSaleAttrVo;

import java.util.List;
import java.util.Map;

/**
 * sku销售属性&值
 *
 * @author renyujie518
 * @email renyujie518@gmail.com
 * @date 2022-01-20 17:33:10
 */
public interface SkuSaleAttrValueService extends IService<SkuSaleAttrValueEntity> {

    PageUtils queryPage(Map<String, Object> params);

    /**
     * @Description: 根据spuId获得spu旗下的所有销售属性组合
     */
    List<SkuItemSaleAttrVo> getSaleAttrBySpuId(Long spuId);

    /**
     * @Description: 依据skuid获取sku的销售属性组合（返回值是List<String>） cart中远程调用 attr_name:attr_value
     */
    List<String> getSkuSaleAttrValuesAsStringList(Long skuId);
}

