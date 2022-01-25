package com.renyujie.gulimall.member.controller;

import java.util.Arrays;
import java.util.Map;

import com.renyujie.gulimall.member.feign.CouponFeignService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.renyujie.gulimall.member.entity.MemberEntity;
import com.renyujie.gulimall.member.service.MemberService;
import com.renyujie.common.utils.PageUtils;
import com.renyujie.common.utils.R;

import javax.annotation.Resource;


/**
 * 会员
 *
 * @author renyujie518
 * @email renyujie518@gmail.com
 * @date 2022-01-20 21:19:14
 */
@RefreshScope
@RestController
@RequestMapping("member/member")
public class MemberController {
    @Autowired
    private MemberService memberService;
    @Resource
    CouponFeignService couponFeignService;


    /**
     * @Description: 测试接口 用于测试openfeign  模拟获取会员下的优惠券
     */
    @RequestMapping("/coupons")
    public R couponTest(){
        MemberEntity memberEntity = new MemberEntity();
        memberEntity.setNickname("张三");
        //这里memberCouponsTest的逻辑实际上是在coupon微服务中写的，这里测试fegin是否调用成功
        R memberCouponsTest = couponFeignService.memberCouponsTest();
        return R.ok().put("memberTest", memberEntity).put("memberCouponsTest", memberCouponsTest);
    }

    /**
     * @Description: 测试接口 用于测试nacos的配置中心的能力 需要配置 bootstrap.properties
     */
    @Value("${memberNacosConfigTest.user.name}")
    private String nacosConfigTestName;
    @Value("${memberNacosConfigTest.user.age}")
    private String nacosConfigTestAge;

    @RequestMapping("/nacosConfigTest")
    public R nacosConfigTest(){
        return R.ok().put("nacosConfigTestName", nacosConfigTestName)
                .put("nacosConfigTestAge", nacosConfigTestAge);
    }

    /**
     * 列表
     */
    @RequestMapping("/list")
        public R list(@RequestParam Map<String, Object> params){
        PageUtils page = memberService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
        public R info(@PathVariable("id") Long id){
		MemberEntity member = memberService.getById(id);

        return R.ok().put("member", member);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
        public R save(@RequestBody MemberEntity member){
		memberService.save(member);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
        public R update(@RequestBody MemberEntity member){
		memberService.updateById(member);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
        public R delete(@RequestBody Long[] ids){
		memberService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
