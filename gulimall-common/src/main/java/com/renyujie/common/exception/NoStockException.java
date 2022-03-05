package com.renyujie.common.exception;

import lombok.Getter;
import lombok.Setter;

/**
 锁库存时没有任何仓库有库存  异常
 */
public class NoStockException extends RuntimeException {

    @Getter
    @Setter
    private Long SkuId;

    public NoStockException(Long SkuId) {
        super("商品ID：" + SkuId + ":没有足够的库存");
    }

    public NoStockException(String msg1) {
        super(msg1);
    }
}
