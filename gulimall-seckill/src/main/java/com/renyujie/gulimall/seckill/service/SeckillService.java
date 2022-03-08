package com.renyujie.gulimall.seckill.service;

import com.renyujie.gulimall.seckill.vo.SeckillSkuRedisTo;

import java.util.List;

/**
 * @author renyujie518
 * @version 1.0.0
 * @ClassName SeckillService.java
 * @Description 秒杀接口
 * @createTime 2022年03月07日 15:49:00
 */
public interface SeckillService {
    /**
     * @description 秒杀的定时上架任务:每天晚上3:00 ：上架最近三天需要秒杀的商品
     */
    void uploadSeckillSkuLatest3Days();


    /**
     * 给首页返回当前时间可以参与的秒杀商品信息
     */
    List<SeckillSkuRedisTo> getCurrentSeckillSkus();

    /**
     * 给远程服务gulimall-product使用(商品详情页)
     * 获取当前sku的秒杀预告信息
     */
    SeckillSkuRedisTo getSkuSeckillInfo(Long skuId);

    /**
     * 商品详情页 点击"立刻抢购"  执行秒杀
     * 参数 killId 对应redis中的sku的键
     *     key是参与秒杀商品的随机码
     *     num是抢购的数量
     *最终返回秒杀成功生成的订单号
     */
    String kill(String killId, String key, Integer num);
}
