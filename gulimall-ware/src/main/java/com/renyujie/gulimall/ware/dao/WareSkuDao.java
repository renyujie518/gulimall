package com.renyujie.gulimall.ware.dao;

import com.renyujie.gulimall.ware.entity.WareSkuEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 商品库存
 * 
 * @author renyujie518
 * @email renyujie518@gmail.com
 * @date 2022-01-20 21:49:21
 */
@Mapper
public interface WareSkuDao extends BaseMapper<WareSkuEntity> {

    /**
     * @Description: 采购成功的sku新增入库  主要更新的就是"库存量"stock字段+skuNum（新购买好的数）
     */
    void addStock(@Param("skuId") Long skuId, @Param("wareId") Long wareId, @Param("skuNum") Integer skuNum);
}
