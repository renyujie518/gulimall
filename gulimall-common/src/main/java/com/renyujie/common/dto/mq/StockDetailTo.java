package com.renyujie.common.dto.mq;

import lombok.Data;

import java.io.Serializable;

/**
   来自WareOrderTaskDetailEntity (wms_ware_order_task_detial)
 */
@Data
public class StockDetailTo implements Serializable {
    private static final long serialVersionUID = 1L;


    /**
     * id
     */
    private Long id;
    /**
     * sku_id
     */
    private Long skuId;
    /**
     * sku_name
     */
    private String skuName;
    /**
     * 购买个数
     */
    private Integer skuNum;
    /**
     * 工作单id
     */
    private Long taskId;
    /**
     * 仓库id
     */
    private Long wareId;
    /**
     * 1-已锁定  2-已解锁  3-扣减
     */
    private Integer lockStatus;
}
