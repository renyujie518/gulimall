package com.renyujie.gulimall.order.controller;

import java.util.Arrays;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.renyujie.gulimall.order.entity.OrderEntity;
import com.renyujie.gulimall.order.service.OrderService;
import com.renyujie.common.utils.PageUtils;
import com.renyujie.common.utils.R;


/**
 * 订单
 *
 * @author renyujie518
 * @email renyujie518@gmail.com
 * @date 2022-01-20 21:37:57
 */
@RestController
@RequestMapping("order/order")
public class OrderController {
    @Autowired
    private OrderService orderService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = orderService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id) {
        OrderEntity order = orderService.getById(id);

        return R.ok().put("order", order);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R save(@RequestBody OrderEntity order) {
        orderService.save(order);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@RequestBody OrderEntity order) {
        orderService.updateById(order);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] ids) {
        orderService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

    /**
     * @description 远程库存服务调用  按照订单号查询订单详情
     */
    @GetMapping("/status/{orderSn}")
    public  R getOrderStatus(@PathVariable("orderSn") String orderSn){
        OrderEntity entity = orderService.getOrderByOrderSn(orderSn);
        return R.ok().setData(entity);
    }
}
