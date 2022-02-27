package com.renyujie.gulimall.auth.vo;

import lombok.Data;

/**
   验证服务与会员服务的DTO
 */
@Data
public class MemberRegistVo {

    private String userName;
    private String password;
    private String phone;
}
