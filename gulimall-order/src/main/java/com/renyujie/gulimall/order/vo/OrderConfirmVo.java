package com.renyujie.gulimall.order.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**

 * @description 订单确认页需要的数据  confirm.html
 */
@Data
public class OrderConfirmVo {

    //收货地址列表
    List<MemberAddressVo> address;

    //所有选中的购物项
    List<OrderItemVo> items;

    //发票....本次不做

    //优惠券 积分（本次做会员积分）
    private Integer integration;

    //订单的防重令牌
    String orderToken;

    //库存  key=skuId value=是否有库存
    Map<Long, Boolean> stocks;

    //总件数
    public Integer getCount() {
        Integer sum = 0;
        if (items != null) {
            for (OrderItemVo item : items) {
                sum += item.getCount();
            }
        }
        return sum;
    }

    //订单总额 需要计算
    public BigDecimal getTotal() {
        BigDecimal sum = new BigDecimal("0");
        if (items != null) {
            for (OrderItemVo item : items) {
                BigDecimal multiply = item.getPrice().multiply(new BigDecimal(item.getCount().toString()));
                sum = sum.add(multiply);
            }
        }
        return sum;
    }

//    private BigDecimal payPrice;
    //应付价格 需要计算
    public BigDecimal getPayPrice() {

        return getTotal();
    }
}
