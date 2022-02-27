package com.renyujie.gulimall.auth.vo;

import lombok.Data;
import lombok.ToString;

/**
   "登录页面"输入的内容
 */
@Data
public class UserLoginVo {

    private String loginacct;
    private String password;
}
