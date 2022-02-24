package com.renyujie.gulimall.search.vo;


import com.renyujie.common.dto.es.SkuEsModel;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
    检索得到的结果返回
 */
@Data
public class SearchResult {

    /**
     * 查询到的所有商品信息（common包中）
     */
    private List<SkuEsModel> products;

    /**
     * 分页信息-当前页码
     */
    private Integer pageNum;
    /**
     * 分页信息-总记录数
     */
    private Long total;
    /**
     * 分页信息-总页码数
     */
    private Integer totalPages;
    private List<Integer> pageNavs;

    /**
     * 当前查到的结果，所有涉及到的品牌(公共显示)
     */
    private List<BrandVo> brands;
    @Data
    public static class BrandVo {
        private Long brandId;
        private String brandName;
        private String brandImg;
    }

    /**
     * 当前查到的结果，所有涉及到的分类(公共显示)
     */
    private List<CatalogVo> catalogs;
    @Data
    public static class CatalogVo {
        private Long catalogId;
        private String catalogName;
    }

    /**
     * @Description: 当前查询到的结果，所涉及到的所有属性(公共显示)
     */
    private List<AttrVo> attrs;
    @Data
    public static class AttrVo {
        private Long attrId;
        private String attrName;
        private List<String> attrValue;
    }

    /**
     * @Description: 面包屑导航数据
     * 导航名  导航值 取消后跳转
     */
    private List<NavVo> navs = new ArrayList<>();
    @Data
    public static class NavVo {
        private String navName;
        private String navValue;
        private String link;
    }

    //面包屑导航时方便前端处理
    private List<Long> attrIds = new ArrayList<>();
}
