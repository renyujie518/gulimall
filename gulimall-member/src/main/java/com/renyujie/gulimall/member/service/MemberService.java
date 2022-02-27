package com.renyujie.gulimall.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.renyujie.common.utils.PageUtils;
import com.renyujie.gulimall.member.entity.MemberEntity;
import com.renyujie.gulimall.member.exception.PhoneExistException;
import com.renyujie.gulimall.member.exception.UsernameExistException;
import com.renyujie.gulimall.member.vo.MemberLoginVo;
import com.renyujie.gulimall.member.vo.MemberRegistVo;

import java.util.Map;

/**
 * 会员
 *
 * @author renyujie518
 * @email renyujie518@gmail.com
 * @date 2022-01-20 21:19:14
 */
public interface MemberService extends IService<MemberEntity> {

    PageUtils queryPage(Map<String, Object> params);

    /**
     * @Description: 保存会员信息（注册页）
     */
    void regist(MemberRegistVo vo) throws PhoneExistException, UsernameExistException;
    /**
     * @Description: 异常机制  确保memberRegistVo携带的phone信息在数据库中唯一
     */
    void checkPhoneUnique(String phone) throws PhoneExistException;

    /**
     * @Description: 异常机制  确保memberRegistVo携带的username信息在数据库中唯一
     */
    void checkUsernameUnique(String username) throws UsernameExistException;

    /**
     * @Description: "登录页"  本站登录
     *
     */
    MemberEntity login(MemberLoginVo memberLoginVo);
}

