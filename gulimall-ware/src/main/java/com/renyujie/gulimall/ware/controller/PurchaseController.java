package com.renyujie.gulimall.ware.controller;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.renyujie.gulimall.ware.vo.MergeVo;
import com.renyujie.gulimall.ware.vo.PurchaseDoneVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.renyujie.gulimall.ware.entity.PurchaseEntity;
import com.renyujie.gulimall.ware.service.PurchaseService;
import com.renyujie.common.utils.PageUtils;
import com.renyujie.common.utils.R;



/**
 * 采购信息
 *
 * @author renyujie518
 * @email renyujie518@gmail.com
 * @date 2022-01-20 21:49:20
 */
@RestController
@RequestMapping("ware/purchase")
public class PurchaseController {
    @Autowired
    private PurchaseService purchaseService;

    /**
     * 列表
     */
    @RequestMapping("/list")
        public R list(@RequestParam Map<String, Object> params){
        PageUtils page = purchaseService.queryPage(params);

        return R.ok().put("page", page);
    }

    /**
     * @Description: 查询未领取的采购单
     */
    @GetMapping("/unreceive/list")
    public R unreceiveList(@RequestParam Map<String, Object> params) {
        PageUtils page = purchaseService.unreceiveList(params);
        return R.ok().put("page", page);
    }


    /**
     * @Description: 将采购需求合并成一个采购单
     */
    @PostMapping("/merge")
    public R merge(@RequestBody MergeVo mergeVo) {
        purchaseService.mergePurchase(mergeVo);
        return R.ok();
    }

    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
        public R info(@PathVariable("id") Long id){
		PurchaseEntity purchase = purchaseService.getById(id);

        return R.ok().put("purchase", purchase);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
        public R save(@RequestBody PurchaseEntity purchase){
        purchase.setCreateTime(new Date());
        purchase.setUpdateTime(new Date());
		purchaseService.save(purchase);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
        public R update(@RequestBody PurchaseEntity purchase){
		purchaseService.updateById(purchase);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
        public R delete(@RequestBody Long[] ids){
		purchaseService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

    /**
     * @Description: app端 工作人员领取采购单
     */
    @PostMapping("/received")
    public R received(@RequestBody List<Long> purchaseIds) {
        purchaseService.received(purchaseIds);

        return R.ok();
    }


    /**
     * @Description: app端  完成采购
     */
    @PostMapping("/done")
    public R finish(@RequestBody PurchaseDoneVo doneVo){
        purchaseService.done(doneVo);

        return R.ok();
    }
}
