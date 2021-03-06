package com.renyujie.gulimall.ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.renyujie.common.utils.PageUtils;
import com.renyujie.gulimall.ware.entity.WareOrderTaskEntity;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

/**
 * 库存工作单
 *
 * @author renyujie518
 * @email renyujie518@gmail.com
 * @date 2022-01-20 21:49:21
 */
public interface WareOrderTaskService extends IService<WareOrderTaskEntity> {

    PageUtils queryPage(Map<String, Object> params);

    /**
     * @description 依据订单号 查询 库存工作单
     */
    WareOrderTaskEntity getOrderTaskByOrderSn(String orderSn);
}

