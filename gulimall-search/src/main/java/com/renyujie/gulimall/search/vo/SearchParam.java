package com.renyujie.gulimall.search.vo;

import lombok.Data;

import java.util.List;

/**
 * 封装页面所有可能传递过来的查询条件
 */

@Data
public class SearchParam {
    /**
     * 页面传递过来的检索参数 相当于全文匹配关键字 skutitle
     */
    private String keyword;

    /**
     * 三级分类id（点击"首页"的3级catelog的时候会将catalog3Id当参数传入）
     */
    private Long catalog3Id;

    /**
     * 排序条件
     *  sort=saleCount_asc/desc   销量
     *  sort=skuPrice_asc/desc 根据价格
     *  sort=hotScore_asc/desc  热度（）
     */
    private String sort;

    /**
     * hasStock(是否有货)
     * hasStock 0/1     0 无库存 1有库存
     */
    private Integer hasStock;

    /**
     * 价格区查询  skuPrice=1_500 500_ _500
     */
    private String skuPrice;

    /**
     * 多个品牌id    brandId = 1&brandId = 2  一个key多个value
     */
    private List<Long> brandId;

    /**
     * 按照属性进行筛选   attrs1_5寸:6寸&attrs2_安卓:ios
     */
    private List<String> attrs;

    /**
     * 页码
     */
    private Integer pageNum = 1;

    /**
     * 原生所有的查询条件
     */
    private String _queryString;
}
