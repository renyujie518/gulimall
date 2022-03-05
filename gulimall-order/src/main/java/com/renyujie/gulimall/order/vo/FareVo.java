package com.renyujie.gulimall.order.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
    远程计算运费返回的对象
 */
@Data
public class FareVo {

    private MemberAddressVo address;
    private BigDecimal fare;
}
