package com.renyujie.gulimall.product.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author renyujie518
 * @version 1.0.0
 * @ClassName Catalog3Vo.java
 * @Description 对应"首页"中的catalog.json中对应的格式3级目录的格式
 * @createTime 2022年02月17日 19:17:00
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public  class Catalog3Vo {
    //父分类 2级分类id
    private String catalog2Id;
    private String id;
    private String name;
}