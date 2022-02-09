package com.renyujie.gulimall.ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.renyujie.common.utils.PageUtils;
import com.renyujie.gulimall.ware.entity.WareSkuEntity;

import java.util.Map;

/**
 * 商品库存
 *
 * @author renyujie518
 * @email renyujie518@gmail.com
 * @date 2022-01-20 21:49:21
 */
public interface WareSkuService extends IService<WareSkuEntity> {

    PageUtils queryPage(Map<String, Object> params);

    /**
     * @Description: 采购成功的sku更新入库
     * skuId 对应 sku_id
     * wareId 对应 ware_id
     * skuNum 对应 stock
     *
     * sku_name  通过feign去查
     * stock_locked  锁定库存默认为0
     */
    void addStock(Long skuId, Long wareId, Integer skuNum);
}

