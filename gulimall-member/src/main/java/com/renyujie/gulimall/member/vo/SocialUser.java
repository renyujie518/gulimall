package com.renyujie.gulimall.member.vo;

import lombok.Data;
import lombok.ToString;

/**
 * @author 孟享广
 * @date 2021-01-31 2:52 下午
 * @description
 */
@Data
@ToString
public class SocialUser {

    /**
     * @Description: 万能牛逼的access_token 用户授权的唯一票据，
     */
    private String access_token;
    private String token_type;
    /**
     * @Description: access_token的生命周期
     */
    private long expires_in;
    private String refresh_token;
    private String scope;
    private long created_at;

    /**
     * @Description: 用户uid  要去gitee查
     */
    private String socialUid;


}
