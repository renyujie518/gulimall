package com.renyujie.gulimall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.renyujie.common.utils.PageUtils;
import com.renyujie.gulimall.product.entity.SkuInfoEntity;
import com.renyujie.gulimall.product.vo.SkuItemVo;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * sku信息
 *
 * @author renyujie518
 * @email renyujie518@gmail.com
 * @date 2022-01-20 17:33:09
 */
public interface SkuInfoService extends IService<SkuInfoEntity> {

    PageUtils queryPage(Map<String, Object> params);

    /**
     * "商品管理"页面获取详情  带模糊检索
     */
    PageUtils queryPageByCondition(Map<String, Object> params);

    /**
     * @Description:  通过spuId获得对应的所有sku
     */
    List<SkuInfoEntity> getSkusById(Long spuId);

    /**
     * @Description: 返回sku详细信息
     */
    SkuItemVo item(Long skuId) throws ExecutionException, InterruptedException;
}

