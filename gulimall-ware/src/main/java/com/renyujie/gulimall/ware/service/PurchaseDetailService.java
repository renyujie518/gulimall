package com.renyujie.gulimall.ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.renyujie.common.utils.PageUtils;
import com.renyujie.gulimall.ware.entity.PurchaseDetailEntity;

import java.util.List;
import java.util.Map;

/**
 * 
 *
 * @author renyujie518
 * @email renyujie518@gmail.com
 * @date 2022-01-20 21:49:20
 */
public interface PurchaseDetailService extends IService<PurchaseDetailEntity> {

    PageUtils queryPage(Map<String, Object> params);

    /**
     * @Description: 由于多个detail可能被merge到一个Purchase下 所以这里查询该purchaseId下所有的detail
     */
    List<PurchaseDetailEntity> DetailListByPurchaseId(Long purchaseId);
}

