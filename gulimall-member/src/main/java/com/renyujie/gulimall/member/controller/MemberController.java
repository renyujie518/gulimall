package com.renyujie.gulimall.member.controller;

import java.util.Arrays;
import java.util.Map;

import com.renyujie.common.exception.BizCodeEnum;
import com.renyujie.gulimall.member.exception.PhoneExistException;
import com.renyujie.gulimall.member.exception.UsernameExistException;
import com.renyujie.gulimall.member.feign.CouponFeignService;
import com.renyujie.gulimall.member.vo.MemberLoginVo;
import com.renyujie.gulimall.member.vo.MemberRegistVo;
import com.renyujie.gulimall.member.vo.SocialUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.*;

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

    /**
     * @Description: 保存会员信息（注册页）
     */
    @PostMapping("/regist")
    public R regist(@RequestBody MemberRegistVo vo) {

        //尝试注册  因为里面的regist方法用到了"异常机制"  所以要尝试捕捉
        try {
            memberService.regist(vo);
        } catch (PhoneExistException e) {
            //捕获了异常 返回失败信息
            return R.error(BizCodeEnum.PHONE_EXISTS_EXCEPTION.getCode(), BizCodeEnum.PHONE_EXISTS_EXCEPTION.getMsg());
        } catch (UsernameExistException e) {
            return R.error(BizCodeEnum.USER_EXISTS_EXCEPTION.getCode(), BizCodeEnum.USER_EXISTS_EXCEPTION.getMsg());
        }

        //成功
        return R.ok();
    }
    /**
     * @Description: "登录页"  本站登录
     *
     */
    @PostMapping("/login")
    public R login(@RequestBody MemberLoginVo memberLoginVo) {

        MemberEntity entity = memberService.login(memberLoginVo);
        if (entity != null) {
            //登录成功
            //给远程调用我的服务返回->真正返回的数据 远程调用者要把这个entity放入session的
            return R.ok().setData(entity);
        }
        //登录失败
         return R.error(BizCodeEnum.LOGIN_ACCOUNT_PASSWORD_INVALID.getCode(), BizCodeEnum.LOGIN_ACCOUNT_PASSWORD_INVALID.getMsg());
    }

    /**
     * @Description: "登录页"  社交gitee登录
     */
    @PostMapping("/gitee-login")
    public R giteeLogin(@RequestBody SocialUser vo) throws Exception {
        MemberEntity entity = memberService.giteeLogin(vo);
        if (entity != null) {
            //登录成功
            //给远程调用我的服务返回->真正返回的数据 远程调用者要把这个entity放入session的
            return R.ok().setData(entity);
        }
        //登录失败
        return R.error(BizCodeEnum.LOGIN_ACCOUNT_PASSWORD_INVALID.getCode(), BizCodeEnum.LOGIN_ACCOUNT_PASSWORD_INVALID.getMsg());
    }



}