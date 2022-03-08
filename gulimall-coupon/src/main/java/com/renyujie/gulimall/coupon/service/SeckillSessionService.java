package com.renyujie.gulimall.coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.renyujie.common.utils.PageUtils;
import com.renyujie.gulimall.coupon.entity.SeckillSessionEntity;

import java.util.List;
import java.util.Map;

/**
 * 秒杀活动场次
 *
 * @author renyujie518
 * @email renyujie518@gmail.com
 * @date 2022-01-20 20:49:30
 */
public interface SeckillSessionService extends IService<SeckillSessionEntity> {

    PageUtils queryPage(Map<String, Object> params);

    /**
     *  从sms_seckill_session中扫描需要参与秒杀的活动  最近三天
     */
    List<SeckillSessionEntity> getLatest3DaysSession();

}

