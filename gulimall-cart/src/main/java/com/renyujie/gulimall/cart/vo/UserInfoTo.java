package com.renyujie.gulimall.cart.vo;

import lombok.Data;
import lombok.ToString;

/**
  购物车用到的用户信息
 */
@ToString
@Data
public class UserInfoTo {

    private Long userId;
    /**
     * @Description: 临时用户在cokkie中的临时键名
     */
    private String userKey;

    private boolean tempUser = false;
}
