package com.renyujie.gulimall.auth.feign;


import com.renyujie.common.utils.R;
import com.renyujie.gulimall.auth.vo.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
   远程的会员服务
 */
//告诉spring cloud 这个接口是一个远程客户端 调用远程服务
@FeignClient("gulimall-member")//这个远程服务
@Component
public interface MemberFeignService {

    //注册
    @PostMapping("/member/member/regist")
    R regist(@RequestBody MemberRegistVo vo);

    //登录
    @PostMapping("/member/member/login")
    R login(@RequestBody UserLoginVo vo);

    //社交
    //社交登录
    @PostMapping("/member/member/gitee-login")
    R giteeLogin(@RequestBody GiteeInfoWithAccessTokenFromCode giteeInfoWithAccessTokenFromCode);
}
