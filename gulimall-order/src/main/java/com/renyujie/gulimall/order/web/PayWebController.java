package com.renyujie.gulimall.order.web;

import com.alipay.api.AlipayApiException;

import com.renyujie.gulimall.order.service.OrderService;
import com.renyujie.gulimall.order.utils.AlipayTemplate;
import com.renyujie.gulimall.order.vo.PayVo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;

/**
   "收银台"点击支付宝图标后的跳转
 */
@Controller
public class PayWebController {

    @Resource
    AlipayTemplate alipayTemplate;

    @Resource
    OrderService orderService;

    /**
     * 获取当前订单的支付信息 PayVo
     * 1 将支付页让浏览器显示
     * 2 支付成功以后，我们要跳到用户的订单列表页
     */
    @ResponseBody
    //text/html:告诉这里返回的是一个html的内容
    @GetMapping(value = "/payOrder", produces = "text/html")
    public String payOrder(@RequestParam("orderSn") String orderSn) throws AlipayApiException {

        //构建订单的支付信息 PayVo
        PayVo payVo = orderService.getPayOrder(orderSn);

        //支付宝返回的是一个"支付二维码页面"，应当将此页面直接交给浏览器（实际上是一段脚本，会自动请求到支付宝的网关）
        String pay = alipayTemplate.pay(payVo);
        return pay;
    }
}
