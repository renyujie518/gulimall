package com.renyujie.gulimall.order.feign;


import com.renyujie.gulimall.order.vo.MemberAddressVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
   会员远程接口
 */
//告诉spring cloud 这个接口是一个远程客户端 调用远程服务
@FeignClient("gulimall-member")//这个远程服务
public interface MemberFeignService {

    /**
     * @Description: 获取指定会员地址信息
     */
    @GetMapping("/member/memberreceiveaddress/{memberId}/getAddress")
    List<MemberAddressVo> getAddress(@PathVariable("memberId") Long memberId);

}
