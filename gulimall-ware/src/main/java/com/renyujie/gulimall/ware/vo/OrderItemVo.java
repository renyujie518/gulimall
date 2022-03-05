package com.renyujie.gulimall.ware.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
   订单中的商品详情（就是confirm.html中展示的商品项的信息）
 */
@Data
public class OrderItemVo {

    private Long skuId;
    private String title;
    private String image;
    //套餐信息
    private List<String> skuAttr;
    //涉及到计算 必须用BigDecimal
    private BigDecimal price;
    private Integer count;
    //总价
    private BigDecimal totalPrice;

    //TODO 是否有货 商品重量
//    private boolean hasStock;
    private BigDecimal weight;

}
