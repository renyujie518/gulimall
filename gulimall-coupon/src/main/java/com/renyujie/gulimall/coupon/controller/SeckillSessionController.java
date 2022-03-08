package com.renyujie.gulimall.coupon.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.renyujie.gulimall.coupon.entity.SeckillSessionEntity;
import com.renyujie.gulimall.coupon.service.SeckillSessionService;
import com.renyujie.common.utils.PageUtils;
import com.renyujie.common.utils.R;



/**
 * 秒杀活动场次
 *
 * @author renyujie518
 * @email renyujie518@gmail.com
 * @date 2022-01-20 20:49:30
 */
@RestController
@RequestMapping("coupon/seckillsession")
public class SeckillSessionController {
    @Autowired
    private SeckillSessionService seckillSessionService;

    /**
     * 列表
     */
    @RequestMapping("/list")
        public R list(@RequestParam Map<String, Object> params){
        PageUtils page = seckillSessionService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
        public R info(@PathVariable("id") Long id){
		SeckillSessionEntity seckillSession = seckillSessionService.getById(id);

        return R.ok().put("seckillSession", seckillSession);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
        public R save(@RequestBody SeckillSessionEntity seckillSession){
		seckillSessionService.save(seckillSession);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
        public R update(@RequestBody SeckillSessionEntity seckillSession){
		seckillSessionService.updateById(seckillSession);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
        public R delete(@RequestBody Long[] ids){
		seckillSessionService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

    /**
     * 给远程服务gulimall-seckill调用
     * 扫描需要参与秒杀的活动 最近3天
     */
    @GetMapping("/latest3DaysSession")
    public R getLatest3DaysSession() {
        List<SeckillSessionEntity> seckillSessionEntities = seckillSessionService.getLatest3DaysSession();
        return R.ok().setData(seckillSessionEntities);
    }


}
