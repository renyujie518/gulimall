package com.renyujie.gulimall.ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.renyujie.common.utils.PageUtils;
import com.renyujie.gulimall.ware.entity.PurchaseEntity;
import com.renyujie.gulimall.ware.vo.MergeVo;
import com.renyujie.gulimall.ware.vo.PurchaseDoneVo;

import java.util.List;
import java.util.Map;

/**
 * 采购信息
 *
 * @author renyujie518
 * @email renyujie518@gmail.com
 * @date 2022-01-20 21:49:20
 */
public interface PurchaseService extends IService<PurchaseEntity> {

    PageUtils queryPage(Map<String, Object> params);

    /**
     * @Description: 查询未领取的采购单
     */
    PageUtils unreceiveList(Map<String, Object> params);

    /**
     * @Description: 将采购需求合并成一个采购单
     */
    void mergePurchase(MergeVo mergeVo);

    /**
     * @Description: app端 工作人员领取采购单
     */
    void received(List<Long> purchaseIds);

    /**
     * @Description: app端  完成采购
     */
    void done(PurchaseDoneVo doneVo);
}

