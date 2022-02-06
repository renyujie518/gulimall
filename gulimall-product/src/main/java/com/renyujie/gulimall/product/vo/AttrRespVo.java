package com.renyujie.gulimall.product.vo;

import lombok.Data;

/**
 * @author renyujie518
 * @version 1.0.0
 * @ClassName AttrRespVo.java
 * @Description "规格参数"页面接收的对象
 * @createTime 2022年02月05日 13:35:00
 */
@Data
public class AttrRespVo extends AttrVo {
    /**
     * catelogName 所属分类名字 eg:手机数码
     */
    private String catelogName;
    /**
     *
     * groupName 所属分组名字  eg:主体
     */
    private String groupName;


    /**
     *
     * 分类完整路径  eg:[2, 34, 225]
     */
    private Long catelogPath[];




}
