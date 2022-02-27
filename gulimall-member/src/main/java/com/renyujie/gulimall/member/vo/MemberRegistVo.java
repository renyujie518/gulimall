package com.renyujie.gulimall.member.vo;

import lombok.Data;

/**
   注册页  注册成功后传来的会员信息
 */
@Data
public class MemberRegistVo {

    private String userName;
    private String password;
    private String phone;
}
