package com.renyujie.gulimall.member.interceptor;


import com.renyujie.common.constant.AuthServiceConstant;
import com.renyujie.common.vo.MemberResVo;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
   拦截器
 */
@Component
public class LoginUserInterceptor implements HandlerInterceptor {

    public static ThreadLocal<MemberResVo> loginUser = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        /**
         * @description
         * 这里要排除一下ware查询运费的接口，因为ware要远程调用member查询用户信息【用户地址，来确定运费】
         * 	所以在拦截器里面要放行/member/ 服务间的feign远程调用，因为这个远程接口是不需要登录状态的
         */
        boolean match = new AntPathMatcher().match("/member/**", request.getRequestURI());
        if (match) {
            return true;
        }

        HttpSession session = request.getSession();
        MemberResVo attribute = (MemberResVo) session.getAttribute(AuthServiceConstant.LOGIN_USER);
        if (attribute != null) {
            //说明登录了 放行
            loginUser.set(attribute);
            return true;
        } else {
            //没登录 去登录页 来拦截
            session.setAttribute("msg", "请先登录～～");
            //response.sendRedirect("http://auth.gulimall.com/login.html");
            response.sendRedirect("http://127.0.0.1:20000/login.html");
            return false;
        }
    }
}
