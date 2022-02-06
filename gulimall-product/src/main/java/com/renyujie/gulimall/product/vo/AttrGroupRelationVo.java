package com.renyujie.gulimall.product.vo;

import lombok.Data;

/**
 * @description  删除属性与分组的关联关系  deleteAttrRelation
 * 前端请求post [{"attrId":1,"attrGroupId":2}]
 */
@Data
public class AttrGroupRelationVo {
    private Long attrId;
    private Long attrGroupId;
}
