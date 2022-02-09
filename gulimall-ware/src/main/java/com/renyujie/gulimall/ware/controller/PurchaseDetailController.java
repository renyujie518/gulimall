package com.renyujie.gulimall.ware.controller;

import java.util.Arrays;
import java.util.Map;

import com.renyujie.gulimall.ware.service.PurchaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.renyujie.gulimall.ware.entity.PurchaseDetailEntity;
import com.renyujie.gulimall.ware.service.PurchaseDetailService;
import com.renyujie.common.utils.PageUtils;
import com.renyujie.common.utils.R;

import javax.annotation.Resource;


/**
 * 
 *
 * @author renyujie518
 * @email renyujie518@gmail.com
 * @date 2022-01-20 21:49:20
 */
@RestController
@RequestMapping("ware/purchasedetail")
public class PurchaseDetailController {
    @Autowired
    private PurchaseDetailService purchaseDetailService;

    /**
     * 列表
     */
    @RequestMapping("/list")
        public R list(@RequestParam Map<String, Object> params){
        PageUtils page = purchaseDetailService.queryPage(params);

        return R.ok().put("page", page);
    }




    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
        public R info(@PathVariable("id") Long id){
		PurchaseDetailEntity purchaseDetail = purchaseDetailService.getById(id);

        return R.ok().put("purchaseDetail", purchaseDetail);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
        public R save(@RequestBody PurchaseDetailEntity purchaseDetail){
		purchaseDetailService.save(purchaseDetail);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
        public R update(@RequestBody PurchaseDetailEntity purchaseDetail){
		purchaseDetailService.updateById(purchaseDetail);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
        public R delete(@RequestBody Long[] ids){
		purchaseDetailService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
