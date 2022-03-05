package com.renyujie.gulimall.order.web;

import com.renyujie.common.exception.NoStockException;
import com.renyujie.gulimall.order.service.OrderService;
import com.renyujie.gulimall.order.vo.OrderConfirmVo;
import com.renyujie.gulimall.order.vo.OrderSubmitVo;
import com.renyujie.gulimall.order.vo.SubmitOrderResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * @author renyujie518
 * @version 1.0.0
 * @ClassName OrderWebController.java
 * @Description 点击购物车"去结算"跳转
 * @createTime 2022年03月04日 13:25:00
 */

@Controller
public class OrderWebController {
    @Resource
    OrderService orderService;

    /**
     * 点击购物车"去结算"跳转
     * 给订单"确认页"返回数据
     */
    @GetMapping("/toTrade")
    public String toTrade(Map<String, OrderConfirmVo> map, Model model) throws ExecutionException, InterruptedException {

        OrderConfirmVo confirmVo = orderService.confirmOrder();
        model.addAttribute("orderConfirmData", confirmVo);
        //map.put("orderConfirmData", confirmVo);
        //展示订单确认页 数据
        return "confirm";
    }

    /**
     * "确认页"点击提交订单   去"支付选择页"  下单失败重新回到"确认页"
     * 前端以表单的方式提交 orderSubmitVo
     */
    @PostMapping("/submitOrder")
    public String submitOrder(OrderSubmitVo orderSubmitVo,
                              Map<String, SubmitOrderResponseVo> map,
                              RedirectAttributes redirectAttributes) {

        System.out.println("点击提交后的数据" + orderSubmitVo);
        try {
            SubmitOrderResponseVo responseVo = orderService.submitOrder(orderSubmitVo);
            //在submitOrder方法中:code=0就是下单成功；令牌验证失败 失败码设置为1;验价失败  code设置为2;锁定失败  code设置为3
            if (responseVo.getCode() == 0) {
                //说明下单成功 来到支付选择页
                map.put("submitOrderResp", responseVo);
                return "pay";
            }else {
                //说明下单失败 返回确认页 回到订单确认页重新提交订单信息  同时附带原因
                String msg = "下单失败:";
                switch (responseVo.getCode()) {
                    case 1:msg += "1（令牌验证失败）订单信息过期，请刷新后提交"; break;
                    case 2:msg += "2（验价失败）订单商品价格发生变化，请确认后再次提交"; break;
                    case 3:msg += "3 库存锁定失败，商品库存不足"; break;
                }
                redirectAttributes.addFlashAttribute("msg", msg);
                return "redirect:http://127.0.0.1:9000/toTrade";
            }
        } catch (Exception e) {
            if (e instanceof NoStockException) {
                String message = ((NoStockException) e).getMessage();
                redirectAttributes.addFlashAttribute("msg", message);
            }
            return "redirect:http://127.0.0.1:9000/toTrade";
        }
    }
}
