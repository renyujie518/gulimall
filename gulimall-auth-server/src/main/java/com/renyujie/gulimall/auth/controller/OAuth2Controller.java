package com.renyujie.gulimall.auth.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.renyujie.common.constant.AuthServiceConstant;
import com.renyujie.common.utils.HttpUtils;
import com.renyujie.common.utils.R;
import com.renyujie.common.vo.MemberResVo;
import com.renyujie.gulimall.auth.feign.MemberFeignService;
import com.renyujie.gulimall.auth.vo.GiteeInfoWithAccessTokenFromCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

/**
 * @description 处理社交登录请求
    点击授权后重定向到这，参数带个code
    eg:http://127.0.0.1:20000/oauth2.0/gitee/success?code=c783af319c29bdbbfafe47f890656a395e55b2dc87040f3508ae890e17767902
    通过@RequestParam获取到code值   要在用这个code从这里https://gitee.com/oauth/token换取Accesstoken
 */
@Slf4j
@Controller
public class OAuth2Controller {
    @Resource
    private MemberFeignService memberFeignService;

    @GetMapping("/oauth2.0/gitee/success")
    public String gitee(@RequestParam("code") String code,HttpSession session) throws Exception {

        Map<String,String> bodys = new HashMap<>();
        bodys.put("grant_type","authorization_code");
        bodys.put("code",code);
        bodys.put("client_id","4f6762ba5826ffb2574608c75ffa64269b9d49838215ef2a20e15827e5ad2767");
        bodys.put("redirect_uri","http://127.0.0.1:20000/oauth2.0/gitee/success");
        bodys.put("client_secret","63ecd85cc6d5012ca218528d931423e752d81d3467537f717908937cc20c36e0");
        /**1.根据code(只能用一次)换取GiteeAccessToken（AccessToken有过期时间）;**/
        HttpResponse response_GiteeAccesstoken = HttpUtils.doPost("https://gitee.com", "/oauth/token", "post", new HashMap(), new HashMap(), bodys);

        //处理返回值
        if (response_GiteeAccesstoken.getStatusLine().getStatusCode() == 200) {
            //获取到了GiteeAccessToken
            String tokenJson2String = EntityUtils.toString(response_GiteeAccesstoken.getEntity());
            GiteeInfoWithAccessTokenFromCode giteeInfoWithAccessTokenFromCode = JSON.parseObject(tokenJson2String, GiteeInfoWithAccessTokenFromCode.class);
            /**2得知道是哪个社交用户登录的**/
            //1） 第一次用 gitee 进行 社交登录=>注册，进行一对一绑定注册到member服务的数据库
            //2） 多次用 gitee 进行 社交登录=>直接登录
            //由member服务来做判断
            R r = memberFeignService.giteeLogin(giteeInfoWithAccessTokenFromCode);
            if (r.getCode() == 0) {
                MemberResVo data = r.getData("data", new TypeReference<MemberResVo>() {
                });
                log.info("社交登录成功，用户信息为：{}" + data.toString());
                //1 第一次使用SESSION 命令浏览器保存JSESSIONID的cookie,以后浏览器访问哪个网站就会带上这个网站的cookie
                //子域之间：gulimall.com auth.guliamll.com member.gulimall.com
                //发卡发的时候(指定域名为父域名)，即使是子系统发的卡，也能让父系统使用
                //TODO 1 默认发的令牌 session=asdfg 作用域是当前域：解决子域session共享问题->springSession
                //TODO 2 希望使用json序列化对象到redis中 ->GulimallSessionConfig
                //远程登录成功，将远程服务返回的entity放入session中
                session.setAttribute(AuthServiceConstant.LOGIN_USER, data);
//                servletResponse.addCookie(new Cookie("JSESSIONID", "dada").setDomain());
                //登录成功 -> 跳转首页
                return "redirect:http://127.0.0.1:10000/";
            } else {
                //失败 重新登录
                return "redirect:http:/login.html";
            }
        } else {
            //没有获取了access_token 登录失败 返回到登录页
            return "redirect:http:/login.html";
        }
    }
}
