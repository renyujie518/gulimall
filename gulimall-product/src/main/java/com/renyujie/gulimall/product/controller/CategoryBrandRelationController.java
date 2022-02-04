package com.renyujie.gulimall.product.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.renyujie.gulimall.product.entity.CategoryBrandRelationEntity;
import com.renyujie.gulimall.product.service.CategoryBrandRelationService;
import com.renyujie.common.utils.PageUtils;
import com.renyujie.common.utils.R;



/**
 * 品牌分类关联
 *
 * @author renyujie518
 * @email renyujie518@gmail.com
 * @date 2022-01-20 18:42:33
 */
@RestController
@RequestMapping("product/categorybrandrelation")
public class CategoryBrandRelationController {
    @Autowired
    private CategoryBrandRelationService categoryBrandRelationService;

    /**
     * 列表
     */
    @RequestMapping("/list")
        public R list(@RequestParam Map<String, Object> params){
        PageUtils page = categoryBrandRelationService.queryPage(params);

        return R.ok().put("page", page);
    }

    /**
     * @Description:一个品牌可以对应多个分类，一个分类也可以对应多个品牌
     *pms_category_brand_relation表就是存储"品牌"-"分类"间的关系  并设置了冗余字段
     * 依据接口列表  /product/categorybrandrelation/catelog/list   输入参数为品牌id: brandId
     *
     */
    @GetMapping("/catelog/list")
    public R catelogList(@RequestParam Long brandId){
        List<CategoryBrandRelationEntity> data =
                categoryBrandRelationService.findCategory2BrandRelation(brandId);
        return R.ok().put("data", data);
    }



    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
        public R info(@PathVariable("id") Long id){
		CategoryBrandRelationEntity categoryBrandRelation = categoryBrandRelationService.getById(id);

        return R.ok().put("categoryBrandRelation", categoryBrandRelation);
    }

    /**
     * 保存
     * 注意 这里前端传的是  {"brandId":1,"catelogId":2}  所以Entity其余字段为null
     */
    @PostMapping ("/save")
        public R save(@RequestBody CategoryBrandRelationEntity categoryBrandRelation){
		categoryBrandRelationService.saveDetial(categoryBrandRelation);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
        public R update(@RequestBody CategoryBrandRelationEntity categoryBrandRelation){
		categoryBrandRelationService.updateById(categoryBrandRelation);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
        public R delete(@RequestBody Long[] ids){
		categoryBrandRelationService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
