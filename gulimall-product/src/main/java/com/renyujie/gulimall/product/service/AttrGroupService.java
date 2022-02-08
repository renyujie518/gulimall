package com.renyujie.gulimall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.renyujie.common.utils.PageUtils;
import com.renyujie.gulimall.product.entity.AttrGroupEntity;
import com.renyujie.gulimall.product.vo.AttrGroupWithAttrsVo;

import java.util.List;
import java.util.Map;

/**
 * 属性分组
 *
 * @author renyujie518
 * @email renyujie518@gmail.com
 * @date 2022-01-20 17:33:11
 */
public interface AttrGroupService extends IService<AttrGroupEntity> {

    PageUtils queryPage(Map<String, Object> params);

    /**
     * @Description: 通过id查询分页list
     */
    PageUtils queryPage(Map<String, Object> params, Long catelogId);

    /**
     * @Description: 获取分类下所有分组&关联属性
     *"发布商品"的第二步"规格参数"需要获取1category下的所有group,每个group下又要获取所有的attr
     *1、查出当前分类下的所有属性分组，
     *2、查出每个属性分组的所有属性
     */
    List<AttrGroupWithAttrsVo> getAttrGroupWithAttrsByCatelogId(Long catelogId);
}

