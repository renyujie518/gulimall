package com.renyujie.gulimall.seckill.feign;


import com.renyujie.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

/**
 远程优惠服务
 */
//告诉spring cloud 这个接口是一个远程客户端 调用远程服务
@FeignClient("gulimall-coupon")//这个远程服务
public interface CouponFeignService {

    /**
     *  从sms_seckill_session中扫描需要参与秒杀的活动
     */
    @GetMapping("/coupon/seckillsession/latest3DaysSession")
    R getLatest3DaysSession();
}
