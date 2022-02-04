package com.renyujie.gulimall.product.service.impl;

import com.renyujie.gulimall.product.service.CategoryBrandRelationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.renyujie.common.utils.PageUtils;
import com.renyujie.common.utils.Query;

import com.renyujie.gulimall.product.dao.BrandDao;
import com.renyujie.gulimall.product.entity.BrandEntity;
import com.renyujie.gulimall.product.service.BrandService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


@Service("brandService")
public class BrandServiceImpl extends ServiceImpl<BrandDao, BrandEntity> implements BrandService {
    @Autowired
    CategoryBrandRelationService categoryBrandRelationService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        QueryWrapper<BrandEntity> wrapper = new QueryWrapper<>();
        //补充id精准查询，名字模糊查询
        String key = (String) params.get("key");
        if (!StringUtils.isEmpty(key)) {
            wrapper.eq("brand_id", key).or().like("name", key);
        }
        IPage<BrandEntity> page = this.page(
                new Query<BrandEntity>().getPage(params),
                wrapper
        );
        return new PageUtils(page);
    }



    /**
     * @Description: 根据id获取品牌信息
     */
    @Override
    public List<BrandEntity> getBrandsByIds(List<Long> brandIds) {
        return baseMapper.selectList(new QueryWrapper<BrandEntity>().in("brand_id", brandIds));
    }

    @Transactional
    @Override
    public void updateDetail(BrandEntity brand) {
        //首先肯定要更新自己这张表  即pms_brands表
        this.updateById(brand);
        if (!StringUtils.isEmpty(brand.getName())) {
            //同步更新pms_category_brand_relation表 中的brand_name字段
            categoryBrandRelationService.updateBrandNameFromBrandChange(brand.getBrandId(), brand.getName());
            //TODO 更新其他关联
        }

    }

}