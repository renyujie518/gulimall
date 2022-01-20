package com.renyujie.gulimall.order.dao;

import com.renyujie.gulimall.order.entity.OrderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单
 * 
 * @author renyujie518
 * @email renyujie518@gmail.com
 * @date 2022-01-20 21:37:57
 */
@Mapper
public interface OrderDao extends BaseMapper<OrderEntity> {
	
}
