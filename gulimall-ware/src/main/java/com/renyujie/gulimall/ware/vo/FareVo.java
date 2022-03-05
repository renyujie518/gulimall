package com.renyujie.gulimall.ware.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
   运费信息
 */
@Data
public class FareVo {

    private MemberAddressVo address;
    private BigDecimal fare;
}
