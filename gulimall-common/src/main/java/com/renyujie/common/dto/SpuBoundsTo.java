package com.renyujie.common.dto;

import lombok.Data;

import java.math.BigDecimal;


@Data
public class SpuBoundsTo {

    private Long spuId;
    private BigDecimal buyBounds;
    private BigDecimal growBounds;

}
