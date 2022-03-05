package com.renyujie.gulimall.order.interceptor;


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


        HttpSession session = request.getSession();
        MemberResVo attribute = (MemberResVo) session.getAttribute(AuthServiceConstant.LOGIN_USER);
        if (attribute != null) {
            //说明登录了 放到ThreadLocal其他共享  放行
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
