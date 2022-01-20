package com.renyujie.gulimall.coupon.dao;

import com.renyujie.gulimall.coupon.entity.CouponHistoryEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券领取历史记录
 * 
 * @author renyujie518
 * @email renyujie518@gmail.com
 * @date 2022-01-20 20:49:32
 */
@Mapper
public interface CouponHistoryDao extends BaseMapper<CouponHistoryEntity> {
	
}
