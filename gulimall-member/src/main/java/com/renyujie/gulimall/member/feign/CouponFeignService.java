package com.renyujie.gulimall.member.feign;

import com.renyujie.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author renyujie518
 * @version 1.0.0
 * @ClassName CouponFeignService.java
 * @Description openfeign远程调用测试
 * @createTime 2022年01月24日 20:19:00
 */
@FeignClient("gulimall-coupon")
public interface CouponFeignService {
    /**
     * @Description: 测试接口 用于测试openfeign  模拟获取会员下的优惠券
     *
     * 告诉spring cloud这个接口是一个远程客户端，要调用coupon服务，
     * 再去调用coupon服务/coupon/coupon/member/list对应的方法(注意mapping中要完整路径)
     */
    @RequestMapping("/coupon/coupon/member/list")
    public R memberCouponsTest();
}
