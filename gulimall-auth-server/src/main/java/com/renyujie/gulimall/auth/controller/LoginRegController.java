package com.renyujie.gulimall.auth.controller;

import com.alibaba.fastjson.TypeReference;
import com.renyujie.common.constant.AuthServiceConstant;
import com.renyujie.common.exception.BizCodeEnum;
import com.renyujie.common.utils.R;
import com.renyujie.common.vo.MemberResVo;
import com.renyujie.gulimall.auth.feign.MemberFeignService;
import com.renyujie.gulimall.auth.feign.ThirdPartyFeignService;
import com.renyujie.gulimall.auth.vo.MemberRegistVo;
import com.renyujie.gulimall.auth.vo.UserLoginVo;
import com.renyujie.gulimall.auth.vo.UserRegistVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author renyujie518
 * @version 1.0.0
 * @ClassName LoginRegController.java
 * @Description TODO
 * @createTime 2022年02月25日 22:19:00
 */
@Controller
public class LoginRegController {
    @Resource
    ThirdPartyFeignService thirdPartyFeignService;
    @Autowired
    StringRedisTemplate stringRedisTemplate;
    @Resource
    MemberFeignService memberFeignService;

    /**
     * @Description: 注册页处理验证码功能
     */
    @ResponseBody
    @GetMapping("/sms/sendcode")
    public R sendCode(@RequestParam("phone") String phone) {
        //1 接口防刷 其实是判断是时间
        String codeFromRedis = stringRedisTemplate.opsForValue().get(AuthServiceConstant.SMS_CODE_CACHE_PREFIX + phone);
        if (!StringUtils.isEmpty(codeFromRedis)) {
            Long clickLastTime = Long.valueOf(codeFromRedis.split("_")[1]);
            if (System.currentTimeMillis() - clickLastTime < 10 * 1000) {
                //前端设置的是10秒后才可重试 10s内 不能再次发送
                return R.error(BizCodeEnum.SMS_CODE_EXCEPTION.getCode(), BizCodeEnum.SMS_CODE_EXCEPTION.getMsg());
            }
        }
        //2 验证码校验   redis   key:sms:code:18810508000  value:5个随机数_20220101
        String code = UUID.randomUUID().toString().substring(0, 5);
        String codeInRedis = code + "_" + System.currentTimeMillis();
        //redis缓存验证码code 有效期10min
        stringRedisTemplate.opsForValue().set(AuthServiceConstant.SMS_CODE_CACHE_PREFIX+phone, codeInRedis, 10, TimeUnit.MINUTES);
        //调用短信服务
        thirdPartyFeignService.sendCode(phone, code);
        return R.ok();
    }

    /**
     * @Description: 注册页  注册
     * 注意点1：为了防止重复表单提交问题，采用RedirectAttributes（共享数据） 重定向携带数据 代替Model
     * 同时不是利用WebMvc的registry 即直接return "reg"(405 POST not support,原因：registry默认get,这里是postmapping)
     * 而是使用return redirect
     *
     * 注意点2：在解决①时使用了使用addFlashAttribute(只转一次)
     * 背后的机制是RedirectAttributes利用session原理 将数据放在session中 只要跳转到下一个页面取出数据后 session中的数据就会删除（达到使用一次的目的）
     * 这样会导致 分布式session问题  TODO
     *
     * 注意点3：若是在②中直接返回  return "redirect:http:/reg.html" 来重定向 会有bug：
     * RedirectAttributes默认会以ip+port 再+return后的值 作为url 来重定向  这样就有拼成的url不带域名的问题（视频中用的都是auth.gulimall.com域名）
     * 所以老师改为了 return "redirect:http://auth.gulimall.com/reg.html"来重定向
     *
     * 注意点4：自己遇到的bug  使用 redirect 重定向无效
     * https://blog.csdn.net/yan88888888888888888/article/details/83502897
     * 将重定向的地址作为数据内容封装到responseBody中返回到页面了，可以把restController改成Controller，
     */
    @PostMapping("/regist")
    public String regist(@Valid UserRegistVo userRegistVo,
                         BindingResult bindingResult,
                         RedirectAttributes redirectAttributes) {
        //如果前端输入的格式有问题  @valid起作用并打包成map发给前端
        if (bindingResult.hasErrors()) {
            //key:出错属性名  value:注解message的项
            Map<String, String> errors = bindingResult.getFieldErrors().stream().collect(Collectors.toMap(
                    FieldError::getField, FieldError::getDefaultMessage
            ));
            //model.addAttribute("errors", errors);
            redirectAttributes.addFlashAttribute("errors", errors);
            //后端校验出错，重新定位到到注册页（带上次的表单出错信息）
            return "redirect:http:/reg.html";
        }
        //用户输入的格式都没问题 开始真正的注册， 调用远程服务注册


        //首先查看验证码是否否正确（点击"发送验证码"即上面的sendCode方法时把验证码存到redis里了）
        String code = userRegistVo.getCode();
        String fromRedis = stringRedisTemplate.opsForValue().get(AuthServiceConstant.SMS_CODE_CACHE_PREFIX + userRegistVo.getPhone());
        if (!StringUtils.isEmpty(fromRedis)) {
            //说明redis存了验证码
            if (code.equals(fromRedis.split("_")[0])) {
                //说明redis验证码 = 前端传过来的 可以远程注册
                //先删除验证码 令牌机制
                stringRedisTemplate.delete(AuthServiceConstant.SMS_CODE_CACHE_PREFIX + userRegistVo.getPhone());
                /**远程注册 调用member服务  MemberRegistVo是验证服务与会员服务的DTO**/
                MemberRegistVo memberRegistVo = new MemberRegistVo();
                BeanUtils.copyProperties(userRegistVo, memberRegistVo);
                R r = memberFeignService.regist(memberRegistVo);
                if (r.getCode() == 0) {
                    //注册成功后回到首页，或者回到登录页
                    return "redirect:http:/login.html";
                }else {
                    //出现异常 或者 失败
                    Map<String, String> errors = new HashMap<>();
                    //R错误消息都在msg里
                    errors.put("msg", r.getData("msg", new TypeReference<String>(){}));
                    redirectAttributes.addFlashAttribute("errors", errors);
                    return "redirect:http:/reg.html";
                }
            } else {
                //说明redis验证码 != 前端传过来的
                Map<String, String> errors = new HashMap<>();
                errors.put("code", "验证码匹配不上");
                redirectAttributes.addFlashAttribute("errors", errors);
                return "redirect:http:/reg.html";
            }
        } else {
            //说明验证码没了，过期了
            Map<String, String> errors = new HashMap<>();
            errors.put("code", "redis中没有验证码");
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:http:/reg.html";
        }
    }

    /**
     * @Description: 登录页面
     * 注意点1 ：在前端处理的时候点击"登录"按钮提交的是表单kv,所以在r入参UserLoginVo前不加@ResponseBody（传输是http或者json才加）
     * 但在调用memberFeignService.login这里面的方法加了@ResponseBody  因为在远程是接收json数据再转为对应对象
     */
    @PostMapping("/login")
    public String login(UserLoginVo userLoginVo,
                        RedirectAttributes redirectAttributes) {
        //远程登录 调用member服务
        R r = memberFeignService.login(userLoginVo);
        if (r.getCode() == 0) {
            //远程登录成功，将远程服务返回的entity放入session中
            MemberResVo memberResVo = r.getData("data", new TypeReference<MemberResVo>(){});
            //session.setAttribute(AuthServiceConstant.LOGIN_USER, memberResVo);
            return "redirect:http://127.0.0.1:10000/";
        }else {
            //远程登录失败
            Map<String, String> errors = new HashMap<>();
            errors.put("msg", r.getData("msg", new TypeReference<String>(){}));
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:http:/login.html";
        }
    }


    /**
     * ！！！！后来代码优化 见本文的->loginPage()
     * 下面两个空方法仅仅是发送一个请求【直接】跳转一个页面
     * 这样不太好 不要写空方法 去GulimallWebConfig.class
     * 使用 SpringMVC ViewController 将请求和页面映射过来
     *
     *
     * 处理已经登录的用户，误操作到登录页面
     */
    @GetMapping("/login.html")
    public String loginPage(HttpSession session) {

        //判断用户是否已经登录
        Object attribute = session.getAttribute(AuthServiceConstant.LOGIN_USER);
        if (attribute == null) {
            //没有登录过 可以跳转到登录页面
            return "login";
        }else {
            //已经登录，禁止跳转到登录页，跳转首页即可
            //return "redirect:http://gulimall.com";
            return "redirect:http://127.0.0.1:10000";
        }
    }
    //@GetMapping("/login.html")
    //public String loginPage() {
    //    return "login";
    //}
    //
    //@GetMapping("/reg.html")
    //public String regPage() {
    //    return "reg";
    //}




}
