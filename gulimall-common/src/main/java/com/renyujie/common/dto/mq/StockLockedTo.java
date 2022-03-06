package com.renyujie.common.dto.mq;

import lombok.Data;

import java.io.Serializable;

/**
   库存锁定成功的信息
 */
@Data
public class StockLockedTo implements Serializable {
    private static final long serialVersionUID = 1L;

    //库存工作单id（wms_ware_order_task的id）
    private Long id;

    //工作单详情(wms_ware_order_task_detial)的信息
    private StockDetailTo detailTo;
}
