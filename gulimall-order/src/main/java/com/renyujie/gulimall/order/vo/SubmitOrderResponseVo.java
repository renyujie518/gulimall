package com.renyujie.gulimall.order.vo;


import com.renyujie.gulimall.order.entity.OrderEntity;
import lombok.Data;

/**
 * 下单后的响应
 */
@Data
public class SubmitOrderResponseVo {

    //下单成功返回这个实体
    private OrderEntity order;
    //下单错误给一个状态码 0:成功
    private Integer code;
}
