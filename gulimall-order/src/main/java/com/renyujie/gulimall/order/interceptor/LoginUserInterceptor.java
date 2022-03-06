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

        /**
         * @description 在p297的时候遇到bug  库存服务远程feign调用次order服务的时候先进过了这个preHandle拦截器
         * 这样其实就不对了  因为设置这个拦截器的初衷是为了在订单服务的确认页和结算页等涉及最终交易的问题上保证用户是登录状态
         * 从浏览器发出的请求中获取session中的loginUser字段得到用户信息 再放到ThreadLocal中共享
         *
         * 在处理"库存解锁"问题时由于需要"根据订单号 远程查询订单详情"feign调用了本order服务  这种微服务间的调用是不需要登录状态的
         * 而且由于此拦截器会导致feign调用失败  因为直接进到下面的else中去了，返回了登录页
         * 所以feign中的response是/login.html 而不是想要的/order/order/status/{orderSn}
         *
         * 解决办法就是针对这种请求特殊处理，拦截器匹配下  路径如果是/order/order/...的 就直接放行  antPathMatcher路径匹配器
         *
         * 同理在处理支付系统的/payed/notify的时候也不需要登录状态  直接放行
         *
         * 备注：RequestURI是请求中的地址  比如/order/order/status/... 这样的
         *       RequestURL是请求的全称 会加上服务器地址名 如 127.0.0.1:10000/order/order/status/  这样的
         */
        AntPathMatcher antPathMatcher = new AntPathMatcher();
        boolean matchForWareFeign = antPathMatcher.match("/order/order/status/**", request.getRequestURI());
        boolean matchForPay = antPathMatcher.match("/payed/notify", request.getRequestURI());
        if (matchForWareFeign || matchForPay) {
            return true;
        }


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
