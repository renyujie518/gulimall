package com.renyujie.gulimall.coupon.service.impl;

import com.renyujie.gulimall.coupon.controller.utils.CouponTimeForStringUtils;
import com.renyujie.gulimall.coupon.entity.SeckillSkuRelationEntity;
import com.renyujie.gulimall.coupon.service.SeckillSkuRelationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.renyujie.common.utils.PageUtils;
import com.renyujie.common.utils.Query;

import com.renyujie.gulimall.coupon.dao.SeckillSessionDao;
import com.renyujie.gulimall.coupon.entity.SeckillSessionEntity;
import com.renyujie.gulimall.coupon.service.SeckillSessionService;


@Service("seckillSessionService")
public class SeckillSessionServiceImpl extends ServiceImpl<SeckillSessionDao, SeckillSessionEntity> implements SeckillSessionService {

    @Autowired
    SeckillSkuRelationService seckillSkuRelationService;
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SeckillSessionEntity> page = this.page(
                new Query<SeckillSessionEntity>().getPage(params),
                new QueryWrapper<SeckillSessionEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<SeckillSessionEntity> getLatest3DaysSession() {

        //计算最近3天  start是当前日的凌晨  比如2022-03-07 00:00:00   end是从start后的3天  2022-03-09 23:59:59
        String start = CouponTimeForStringUtils.startTimeString();
        String end = CouponTimeForStringUtils.endTimeForString();

        //获取3天内的秒杀场次信息
        List<SeckillSessionEntity> seckillSessionEntities = this.list(
                new QueryWrapper<SeckillSessionEntity>().between("start_time", start, end));

        if (seckillSessionEntities != null && seckillSessionEntities.size() > 0) {
            List<SeckillSessionEntity> res = seckillSessionEntities.stream().map(
                    (seckillSessionEntity) -> {
                //获得场次id
                Long id = seckillSessionEntity.getId();
                //通过场次id从sms_seckill_sku_relation获得具体的参与秒杀的sku信息
                List<SeckillSkuRelationEntity> seckillSkuRelationEntities = seckillSkuRelationService.list(
                        new QueryWrapper<SeckillSkuRelationEntity>().eq("promotion_session_id", id));
                seckillSessionEntity.setSeckillSkuRelationEntities(seckillSkuRelationEntities);
                return seckillSessionEntity;
            }).collect(Collectors.toList());
            return res;
        }
        //说明当前天到三天后这个时间段内没有秒杀场次
        return null;
    }

}