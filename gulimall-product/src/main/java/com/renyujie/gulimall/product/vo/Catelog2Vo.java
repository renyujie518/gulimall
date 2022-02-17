package com.renyujie.gulimall.product.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 *对应"首页"中的catalog.json中对应的格式2级目录的格式（1级是一级的catalog1Id）
 * static.index.js.catalogLoader.js
 * 这个js会发送一个ajax请求，然后解析数据
 *    $.getJSON("index/json/catalog.json",function (data) {}// 这里使用了静态数据catalog.json
 */

//无参构造器
@NoArgsConstructor
//全参构造器
@AllArgsConstructor
@Data
public class Catelog2Vo {

    //一级父分类
    private String catalog1Id;
    // 3级子分类
    private List<Catalog3Vo> catalog3List;
    private String id;
    private String name;

}
