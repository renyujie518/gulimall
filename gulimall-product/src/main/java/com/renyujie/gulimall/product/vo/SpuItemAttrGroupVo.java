package com.renyujie.gulimall.product.vo;

import lombok.Data;
import lombok.ToString;

import java.util.List;

/**
  详情页  spu的规格参数信息
 */
@ToString
@Data
public class SpuItemAttrGroupVo {
    private String groupName;
    private List<Attr> attrs;
}
