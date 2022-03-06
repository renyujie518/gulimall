package com.renyujie.gulimall.ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.renyujie.common.dto.mq.OrderTo;
import com.renyujie.common.dto.mq.StockLockedTo;
import com.renyujie.common.utils.PageUtils;
import com.renyujie.gulimall.ware.entity.WareSkuEntity;
import com.renyujie.gulimall.ware.vo.SkuHasStockVo;
import com.renyujie.gulimall.ware.vo.WareSkuLockVo;

import java.util.List;
import java.util.Map;

/**
 * 商品库存
 *
 * @author renyujie518
 * @email renyujie518@gmail.com
 * @date 2022-01-20 21:49:21
 */
public interface WareSkuService extends IService<WareSkuEntity> {

    PageUtils queryPage(Map<String, Object> params);

    /**
     * @Description: 采购成功的sku更新入库
     * skuId 对应 sku_id
     * wareId 对应 ware_id
     * skuNum 对应 stock
     *
     * sku_name  通过feign去查
     * stock_locked  锁定库存默认为0
     */
    void addStock(Long skuId, Long wareId, Integer skuNum);


    /**
     * @Description:  order调用
     * 批量查询sku是否有库存
     */
    List<SkuHasStockVo> getSkuHasStock(List<Long> skuIds);

    /**
     * 为某个订单锁定库存  order服务调用
     */
    Boolean orderLockStock(WareSkuLockVo vo);


    /**
     * 库存解锁的场景1
     下订单成功，库存锁定成功，接下来的业务调用失败，导致订单回滚。之前锁定的库存就要自动解锁
     */
    void unLockStock(StockLockedTo to);

    /**
     * 防止订单服务卡顿，导致库存订单状态一直改变不了为4（已取消），库存消息优先到，查订单状态，查到的是新建状态0
     * 这会导致上面的解锁服务什么都不做就走了，但是该mq已消费，导致这个取消的但是卡顿到的订单，永远不能解锁库存
     *
     * 即在处理OrderServiceImpl#closeOrder问题
     */
    void unLockStock(OrderTo orderTo);
}

