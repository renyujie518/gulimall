package com.renyujie.gulimall.product.web;

import com.renyujie.gulimall.product.entity.CategoryEntity;
import com.renyujie.gulimall.product.service.CategoryService;
import com.renyujie.gulimall.product.vo.Catelog2Vo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

/**
 * @author renyujie518
 * @version 1.0.0
 * @ClassName IndexController.java
 * @Description 商品首页
 * @createTime 2022年02月17日 16:49:00
 */
@Controller
public class IndexController {
    @Autowired
    CategoryService categoryService;


    @GetMapping({"/", "/index.html"})
    public String  indexPage(Map<String, List<CategoryEntity>> map) {
        //查询一级分类
        List<CategoryEntity> level1Catrgorys = categoryService.getLevel1Catrgorys();
        map.put("categorys", level1Catrgorys);
        //前缀后置spring都写好了 controller返回String，就会默认加上前缀classpath://templates/  后缀为.html
        return "index";
    }

    /**
     * @Description: "首页"查出所有分类
     */
    @ResponseBody
    @GetMapping("/index/catalog.json")
    public Map<String, List<Catelog2Vo>> getCatelogJson() {
        Map<String, List<Catelog2Vo>> map = categoryService.getCatalogJson();
        return map;
    }
}
