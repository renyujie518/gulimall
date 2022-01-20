package com.renyujie.gulimall.order.dao;

import com.renyujie.gulimall.order.entity.PaymentInfoEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 支付信息表
 * 
 * @author renyujie518
 * @email renyujie518@gmail.com
 * @date 2022-01-20 21:37:56
 */
@Mapper
public interface PaymentInfoDao extends BaseMapper<PaymentInfoEntity> {
	
}
