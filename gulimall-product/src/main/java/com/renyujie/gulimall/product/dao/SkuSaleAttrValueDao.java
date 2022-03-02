package com.renyujie.gulimall.product.dao;

import com.renyujie.gulimall.product.entity.SkuSaleAttrValueEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.renyujie.gulimall.product.vo.SkuItemSaleAttrVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * sku销售属性&值
 * 
 * @author renyujie518
 * @email renyujie518@gmail.com
 * @date 2022-01-20 17:33:10
 */
@Mapper
public interface SkuSaleAttrValueDao extends BaseMapper<SkuSaleAttrValueEntity> {

    /**
     * @Description: 根据spuId获得spu旗下的所有销售属性组合
     * 先去pms_sku_info中得到属于输入的spuId 下所有的sku_id有哪些
     * 在去pms_sku_sale_attr_value可以得到各个sku下的属性名attr_name和属性值attr_value
     *
     */
    List<SkuItemSaleAttrVo> getSaleAttrBySpuId(@Param("spuId") Long spuId);


    /**
     * @Description: 依据skuid获取sku的销售属性组合（返回值是List<String>） cart中远程调用  attr_name:attr_value
     */
    List<String> getSkuSaleAttrValuesAsStringList(@Param("skuId") Long skuId);
}
