package com.renyujie.gulimall.product.dao;

import com.renyujie.gulimall.product.entity.AttrGroupEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.renyujie.gulimall.product.vo.SpuItemAttrGroupVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 属性分组
 * 
 * @author renyujie518
 * @email renyujie518@gmail.com
 * @date 2022-01-20 17:33:11
 */
@Mapper
public interface AttrGroupDao extends BaseMapper<AttrGroupEntity> {
    /**
     * @Description: 查出当前spuId对应的所有分组信息 以及 当前分组下所有属性对应的attrvalue
     * 首先从pns_attr_group中利用catalogId获取该分类下的所有组信息：attr_group_id,attr_group_name
     * 再通过组id：attr_group_id 从 pms_attr_attrgroup_relation获取 该组下的属性id attr_id
     * 有了attr_id就可以去pms_attr中获得属性名 attr_name
     * 最后一步  最后的attr_value是在 pms_product_attr_value中存着  依据刚得到的attr_id可获取
     *
     *
     */
    List<SpuItemAttrGroupVo> getAttrGroupWithAttrsBySpuIdAndCatalogId(@Param("spuId") Long spuId, @Param("catalogId") Long catalogId);
}
