package com.renyujie.gulimall.member.web;

import com.alibaba.fastjson.JSON;
import com.renyujie.common.utils.R;
import com.renyujie.gulimall.member.feign.OrderFeignService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.Map;

/**
    订单列表： 付款成功后跳转
 */
@Controller
public class MemberWebController {

    @Autowired
    OrderFeignService orderFeignService;

    /**
     * 我的订单
     */
    @GetMapping("/memberOrder.html")
    public String memberOrderPage(@RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum, Map<String, R> map) {

        Map<String, Object> page = new HashMap<>();
        page.put("page", pageNum.toString());
        //查出当前登录的用户的所有订单列表数据
        R r = orderFeignService.listWithItem(page);
        System.out.println(JSON.toJSONString(r));
        map.put("orders", r);
        return "orderList";
    }
}
