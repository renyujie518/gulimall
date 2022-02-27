package com.renyujie.gulimall.auth.vo;

import lombok.Data;
import lombok.ToString;

/**
 * @author renyujie518
 * @version 1.0.0
 * @ClassName GiteeAccessToken.java
 * @Description 社交登录-gitee
 * 将code给gitee后  https://gitee.com/oauth/token  返回给我们的信息映射实体
 * @createTime 2022年02月27日 15:14:00
 */
@Data
@ToString
public class GiteeInfoWithAccessTokenFromCode {
    /**
     * @Description: 万能牛逼的access_token
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


}
