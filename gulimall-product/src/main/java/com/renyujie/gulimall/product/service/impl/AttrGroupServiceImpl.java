package com.renyujie.gulimall.product.service.impl;


import com.renyujie.gulimall.product.entity.AttrEntity;
import com.renyujie.gulimall.product.service.AttrService;
import com.renyujie.gulimall.product.vo.AttrGroupWithAttrsVo;
import com.renyujie.gulimall.product.vo.SpuItemAttrGroupVo;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.renyujie.common.utils.PageUtils;
import com.renyujie.common.utils.Query;

import com.renyujie.gulimall.product.dao.AttrGroupDao;
import com.renyujie.gulimall.product.entity.AttrGroupEntity;
import com.renyujie.gulimall.product.service.AttrGroupService;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;


@Service("attrGroupService")
public class AttrGroupServiceImpl extends ServiceImpl<AttrGroupDao, AttrGroupEntity> implements AttrGroupService {

    @Resource
    AttrService attrService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<AttrGroupEntity> page = this.page(
                new Query<AttrGroupEntity>().getPage(params),
                new QueryWrapper<AttrGroupEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * @Description: 通过id查询分页list
     */
    @Override
    public PageUtils queryPage(Map<String, Object> params, Long catelogId) {
        //由于有搜索框的出现  所以有了迷糊匹配的需求  参照开发文档 搜索的关键字为key
        // select * from pms_attr_group where catelog_id=? and (attr_group_id=key or attr_group_name like  %key%)
        String key = (String) params.get("key");
        QueryWrapper<AttrGroupEntity> wrapper = new QueryWrapper<AttrGroupEntity>();
        if (!StringUtils.isEmpty(key)) {
            wrapper.and((obj) -> {
                obj.eq("attr_group_id", key).or().like("attr_group_name", key);


            });
        }
        if (catelogId == 0) {
            IPage<AttrGroupEntity> page = this.page(new Query<AttrGroupEntity>().getPage(params), wrapper);
            return new PageUtils(page);
        } else {

            wrapper.eq("catelog_id", catelogId);
            IPage<AttrGroupEntity> page = this.page(new Query<AttrGroupEntity>().getPage(params), wrapper);
            return new PageUtils(page);

        }
    }

    /**
     * @Description: 获取分类下所有分组&关联属性
     *"发布商品"的第二步"规格参数"需要获取1category下的所有group,每个group下又要获取所有的attr
     *1、查出当前分类下的所有属性分组，
     *2、查出每个属性分组的所有属性
     */
    @Override
    public List<AttrGroupWithAttrsVo> getAttrGroupWithAttrsByCatelogId(Long catelogId) {
        List<AttrGroupEntity> attrGroupEntities = this.list(new QueryWrapper<AttrGroupEntity>()
                .eq("catelog_id", catelogId));
        List<AttrGroupWithAttrsVo> result = attrGroupEntities.stream().map((attrGroupEntity) -> {
            //首先  attrGroupEntity中包含了AttrGroupWithAttrsVo初attrs[]的所有信息 直接copy
            AttrGroupWithAttrsVo attrGroupWithAttrsVo = new AttrGroupWithAttrsVo();
            BeanUtils.copyProperties(attrGroupEntity, attrGroupWithAttrsVo);
            //再补全attrs[] 即每个group下的的所有属性
            List<AttrEntity> attrs = attrService.getAttrsFromGroupId(attrGroupEntity.getAttrGroupId());
            attrGroupWithAttrsVo.setAttrs(attrs);
            return attrGroupWithAttrsVo;
        }).collect(Collectors.toList());

        return result;
    }

    /**
     * @Description: 查出当前spuId对应的所有分组信息 以及 当前分组下所有属性对应的attrvalue
     */
    @Override
    public List<SpuItemAttrGroupVo> getAttrGroupWithAttrsBySpuIdAndCatalogId(Long spuId, Long catalogId) {
        List<SpuItemAttrGroupVo> res = this.baseMapper.getAttrGroupWithAttrsBySpuIdAndCatalogId(spuId, catalogId);
        return res;
    }

}