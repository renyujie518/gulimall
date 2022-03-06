package com.renyujie.gulimall.ware.dao;

import com.renyujie.gulimall.ware.entity.WareSkuEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

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


    /**
     * @Description: 批量查询sku是否有库存
     */
    Long getSkuStock(@Param("skuId") Long skuId);


    /**
     * @Description: sku在那个仓库中有库存（即已有-锁定>0）
     */
    List<Long> wareIdsHasSkuStock(@Param("skuId") Long skuId);


    /**
     * @Description: 在wareId这个仓库中的这个skuId，去锁定库存
     * （stock_locked累加needLockNum,同时保证还有余量 即needLockNum不能超过余量）
     */
    Long lockSkuStockInThisWare(@Param("skuId") Long skuId, @Param("wareId") Long wareId, @Param("num") Integer needLockNum);


    /**
     * @description 最终的解锁方案 最终一致性方案中的补偿项  实际上就是把之前加上去的stock_locked再减回来
     */
    void unLockStock(@Param("skuId") Long skuId, @Param("wareId") Long wareId, @Param("num") Integer num);
}
