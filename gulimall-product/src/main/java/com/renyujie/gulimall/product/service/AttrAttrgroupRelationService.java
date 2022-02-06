package com.renyujie.gulimall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.renyujie.common.utils.PageUtils;
import com.renyujie.gulimall.product.entity.AttrAttrgroupRelationEntity;
import com.renyujie.gulimall.product.vo.AttrGroupRelationVo;

import java.util.List;
import java.util.Map;

/**
 * 属性&属性分组关联
 *
 * @author renyujie518
 * @email renyujie518@gmail.com
 * @date 2022-01-20 17:33:10
 */
public interface AttrAttrgroupRelationService extends IService<AttrAttrgroupRelationEntity> {

    PageUtils queryPage(Map<String, Object> params);

    /**
     * @Description: 添加属性与分组关联关系
     * 在"属性分组"页面的"关联"中的"新增关联"时"确认新增"的提交
     */
    void saveBatchWhenGroup(List<AttrGroupRelationVo> vos);
}

