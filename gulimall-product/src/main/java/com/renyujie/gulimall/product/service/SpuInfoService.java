package com.renyujie.gulimall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.renyujie.common.utils.PageUtils;
import com.renyujie.gulimall.product.entity.SpuInfoEntity;
import com.renyujie.gulimall.product.vo.SpuSaveVo;

import java.util.Map;

/**
 * spu信息
 *
 * @author renyujie518
 * @email renyujie518@gmail.com
 * @date 2022-01-20 17:33:09
 */
public interface SpuInfoService extends IService<SpuInfoEntity> {

    PageUtils queryPage(Map<String, Object> params);

    /**
     * /product/spuinfo/save
     * "发布商品"的最终大保存
     * 非常大的一个方法
     * SpuSaveVo是前端json生成的大对象
     */
    void saveSpuInfo(SpuSaveVo spuSaveVo);

    /**
     * "spu管理"页面获取详情  带模糊检索
     */
    PageUtils queryPageByCondition(Map<String, Object> params);

    /**
     * @Description: 商品上架  对应"spu管理"的"上架"按钮 同时存储sku到es中
     */
    void up(Long spuId);

    /**
     * 根据skuId返回spu信息 order服务调用
     */
    SpuInfoEntity getSpuInfoBuSkuId(Long skuId);
}

