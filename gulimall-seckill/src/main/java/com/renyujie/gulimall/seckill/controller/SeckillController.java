package com.renyujie.gulimall.seckill.controller;

import com.renyujie.common.utils.R;
import com.renyujie.gulimall.seckill.service.SeckillService;
import com.renyujie.gulimall.seckill.vo.SeckillSkuRedisTo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

/**
 * @author renyujie518
 * @version 1.0.0
 * @ClassName SeckillController.java
 * @Description 秒杀返还给前端
 * @createTime 2022年03月08日 11:46:00
 */
@Controller
@Slf4j
public class SeckillController {
    @Autowired
    SeckillService seckillService;

    /**
     * 给首页返回当前时间可以参与的秒杀商品信息
     */
    @ResponseBody
    @GetMapping("/currentSeckillSkus")
    public R getCurrentSeckillSkus() {
        log.info("/currentSeckillSkus正在执行..");

        List<SeckillSkuRedisTo> seckillSkuRedisTos = seckillService.getCurrentSeckillSkus();
        return R.ok().setData(seckillSkuRedisTos);
    }

    /**
     * 给远程服务gulimall-product使用(商品详情页)
     * 获取当前sku的秒杀预告信息
     */
    @ResponseBody
    @GetMapping("/sku/seckill/{skuId}")
    public R getSkuSeckillInfo(@PathVariable("skuId") Long skuId) {

        //try {
        //    Thread.sleep(300);
        //} catch (InterruptedException e) {
        //    e.printStackTrace();
        //}
        SeckillSkuRedisTo to = seckillService.getSkuSeckillInfo(skuId);
        return R.ok().setData(to);
    }

    /**
     * 商品详情页 点击"立刻抢购"  执行秒杀
     * 参数 killId 对应redis中的sku的键
     *     key是参与秒杀商品的随机码
     *     num是抢购的数量
     * http://seckill.gulimall.com/kill?killId=1_1&key=320c924165244276882adfaea84dac12&num=1
     *
     * 后端再次验证是否登录（拦截器实现）
     *
     * 最终返回秒杀成功生成的订单号
     */
    @GetMapping("/kill")
    public String getKill(@RequestParam("killId") String killId,
                          @RequestParam("key") String key,
                          @RequestParam("num") Integer num,
                          Map<String, String> map){

        String orderSn = seckillService.kill(killId, key, num);
        map.put("orderSn", orderSn);
        return "success";
    }

}
