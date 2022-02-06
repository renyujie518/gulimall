package com.renyujie.gulimall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.renyujie.common.utils.PageUtils;
import com.renyujie.gulimall.product.entity.AttrEntity;
import com.renyujie.gulimall.product.vo.AttrGroupRelationVo;
import com.renyujie.gulimall.product.vo.AttrRespVo;
import com.renyujie.gulimall.product.vo.AttrVo;

import java.util.List;
import java.util.Map;

/**
 * 商品属性
 *
 * @author renyujie518
 * @email renyujie518@gmail.com
 * @date 2022-01-20 17:33:12
 */
public interface AttrService extends IService<AttrEntity> {

    PageUtils queryPage(Map<String, Object> params);

    /**
     * @Description: 保存"规格参数"页面 新增属性的save方法
     */
    void saveAttr(AttrVo attr);

    /**
     * @Description: "规格参数"页面获取基本属性
     */
    PageUtils queryBaseAttrPage(Map<String, Object> params, Long catelogId, String attrType);

    /**
     * @Description:"规格参数"新增页面回显
     *
     */
    AttrRespVo getAttrInfo(Long attrId);

    /**
     *  @Description:"规格参数"有内容变化时更新表单
     */
    void updateAttr(AttrVo attrVo);

    /**
     * @Description: 获取指定分组关联的所有属性
     * pms_attr_attrgroup_relation表中 一个attr_group_id对应多个attr_id  即一个分组下有多个属性
     */
    List<AttrEntity> getAttrsFromGroupId(Long attrgroupId);

    /**
     * @Description: 删除属性与分组的关联关系   与attrRelation方法对应
     * 一个attr_group_id对应多个attr_id  即一个分组下有多个属性  删除这组关系
     */
    void deleteAttrRelation(AttrGroupRelationVo[] attrGroupRelationVos);

    /**
     * @Description: 获取属性分组没有关联的其他属性
     * 在"属性分组"页面的"关联"中的"新增关联"时  需要获取该分组下没有关联的其他属性 而且有分类category的要求
     */
    PageUtils getNoRelationAttr(Map<String, Object> params, Long attrGroupId);
}

