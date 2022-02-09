package com.renyujie.gulimall.product.service.impl;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.renyujie.common.utils.PageUtils;
import com.renyujie.common.utils.Query;

import com.renyujie.gulimall.product.dao.ProductAttrValueDao;
import com.renyujie.gulimall.product.entity.ProductAttrValueEntity;
import com.renyujie.gulimall.product.service.ProductAttrValueService;
import org.springframework.transaction.annotation.Transactional;


@Service("productAttrValueService")
public class ProductAttrValueServiceImpl extends ServiceImpl<ProductAttrValueDao, ProductAttrValueEntity> implements ProductAttrValueService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<ProductAttrValueEntity> page = this.page(
                new Query<ProductAttrValueEntity>().getPage(params),
                new QueryWrapper<ProductAttrValueEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * @Description: 获取spu规格  用于"spu管理"点击"规格维护"后的回显
     */
    @Override
    public List<ProductAttrValueEntity> baseListForSpu(Long spuId) {
        List<ProductAttrValueEntity> entities = this.baseMapper.selectList(new QueryWrapper<ProductAttrValueEntity>().eq("spu_id", spuId));
        return entities;
    }

    /**
     * @Description: 修改商品规格  用于"spu管理"点击"规格维护"后 修改后跟新
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateSpuAttr(Long spuId, List<ProductAttrValueEntity> entities) {
        //1 删除这个spuId之前对应的所有属性
        this.baseMapper.delete(new QueryWrapper<ProductAttrValueEntity>().eq("spu_id", spuId));

        //2 更新
        List<ProductAttrValueEntity> collect = entities.stream().map((attrValueEntity) -> {
            attrValueEntity.setSpuId(spuId);
            return attrValueEntity;
        }).collect(Collectors.toList());

        this.saveBatch(collect);
    }

}