package com.renyujie.gulimall.product.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.renyujie.gulimall.product.entity.ProductAttrValueEntity;
import com.renyujie.gulimall.product.service.ProductAttrValueService;
import com.renyujie.gulimall.product.vo.AttrRespVo;
import com.renyujie.gulimall.product.vo.AttrVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.renyujie.gulimall.product.entity.AttrEntity;
import com.renyujie.gulimall.product.service.AttrService;
import com.renyujie.common.utils.PageUtils;
import com.renyujie.common.utils.R;

import javax.annotation.Resource;


/**
 * 商品属性
 *
 * @author renyujie518
 * @email renyujie518@gmail.com
 * @date 2022-01-20 18:42:34
 */
@RestController
@RequestMapping("product/attr")
public class AttrController {
    @Autowired
    private AttrService attrService;

    @Resource
    ProductAttrValueService productAttrValueService;

    /**
     * 列表
     */
    @RequestMapping("/list")
        public R list(@RequestParam Map<String, Object> params){
        PageUtils page = attrService.queryPage(params);

        return R.ok().put("page", page);
    }

    /**
     * @Description: "规格参数"页面获取基本属性
     * product/attr/sale/list/0?
     * product/attr/base/list/{catelogId}
     * 注意 这里本来是/base/list/  但是"销售属性"页面的信息获取逻辑和路径都与此类似  所以修改公用一个controller
     *
     */
    @GetMapping("/{attrType}/list/{catelogId}")
    public R baseAttrList(@RequestParam Map<String, Object> params,
                          @PathVariable("catelogId") Long catelogId,
                          @PathVariable("attrType") String attrType) {
        PageUtils page = attrService.queryBaseAttrPage(params, catelogId,attrType);
        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{attrId}")
    public R info(@PathVariable("attrId") Long attrId) {
        //AttrEntity attr = attrService.getById(attrId);
        AttrRespVo attrRespVo = attrService.getAttrInfo(attrId);
        return R.ok().put("attr", attrRespVo);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
        public R save(@RequestBody AttrVo attr){
		attrService.saveAttr(attr);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
        public R update(@RequestBody AttrVo attrVo){
        attrService.updateAttr(attrVo);
        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
        public R delete(@RequestBody Long[] attrIds){
		attrService.removeByIds(Arrays.asList(attrIds));

        return R.ok();
    }
/**
 * @Description: 获取spu规格  用于"spu管理"点击"规格维护"后的回显
 */
    @GetMapping("/base/listforspu/{spuId}")
    public R baseAttrList(@PathVariable("spuId") Long spuId) {
        List<ProductAttrValueEntity> entities = productAttrValueService.baseListForSpu(spuId);
        return R.ok().put("data", entities);
    }

    /**
     * @Description: 修改商品规格  用于"spu管理"点击"规格维护"后 修改后跟新
     */
    @PostMapping("/update/{spuId}")
    public R updateSpuAttr(@PathVariable("spuId") Long spuId,
                           @RequestBody List<ProductAttrValueEntity> entities){
        productAttrValueService.updateSpuAttr(spuId, entities);
        return R.ok();
    }

}
