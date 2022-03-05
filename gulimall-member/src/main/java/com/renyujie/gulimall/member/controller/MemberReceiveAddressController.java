package com.renyujie.gulimall.member.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.renyujie.gulimall.member.entity.MemberReceiveAddressEntity;
import com.renyujie.gulimall.member.service.MemberReceiveAddressService;
import com.renyujie.common.utils.PageUtils;
import com.renyujie.common.utils.R;



/**
 * 会员收货地址
 *
 * @author renyujie518
 * @email renyujie518@gmail.com
 * @date 2022-01-20 21:19:12
 */
@RestController
@RequestMapping("member/memberreceiveaddress")
public class MemberReceiveAddressController {
    @Autowired
    private MemberReceiveAddressService memberReceiveAddressService;

    /**
     * 列表
     */
    @RequestMapping("/list")
        public R list(@RequestParam Map<String, Object> params){
        PageUtils page = memberReceiveAddressService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
        public R info(@PathVariable("id") Long id){
		MemberReceiveAddressEntity memberReceiveAddress = memberReceiveAddressService.getById(id);

        return R.ok().put("memberReceiveAddress", memberReceiveAddress);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
        public R save(@RequestBody MemberReceiveAddressEntity memberReceiveAddress){
		memberReceiveAddressService.save(memberReceiveAddress);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
        public R update(@RequestBody MemberReceiveAddressEntity memberReceiveAddress){
		memberReceiveAddressService.updateById(memberReceiveAddress);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
        public R delete(@RequestBody Long[] ids){
		memberReceiveAddressService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

    /**
     * @Description: 获取指定会员地址信息
     */
    @GetMapping("/{memberId}/getAddress")
    public List<MemberReceiveAddressEntity> getAddress(@PathVariable("memberId") Long memberId) {

        List<MemberReceiveAddressEntity> entities = memberReceiveAddressService.getAddress(memberId);
        return entities;
    }

}
