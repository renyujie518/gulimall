package com.renyujie.gulimall.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.renyujie.gulimall.product.dao.BrandDao;
import com.renyujie.gulimall.product.entity.BrandEntity;
import com.renyujie.gulimall.product.entity.CategoryEntity;
import com.renyujie.gulimall.product.service.BrandService;
import com.renyujie.gulimall.product.service.CategoryService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.renyujie.common.utils.PageUtils;
import com.renyujie.common.utils.Query;

import com.renyujie.gulimall.product.dao.CategoryBrandRelationDao;
import com.renyujie.gulimall.product.entity.CategoryBrandRelationEntity;
import com.renyujie.gulimall.product.service.CategoryBrandRelationService;

import javax.annotation.Resource;


@Service("categoryBrandRelationService")
public class CategoryBrandRelationServiceImpl extends ServiceImpl<CategoryBrandRelationDao, CategoryBrandRelationEntity> implements CategoryBrandRelationService {
    @Resource
    CategoryService categoryService;
    @Resource
    BrandService brandService;



    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryBrandRelationEntity> page = this.page(
                new Query<CategoryBrandRelationEntity>().getPage(params),
                new QueryWrapper<CategoryBrandRelationEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<CategoryBrandRelationEntity> findCategory2BrandRelation(Long brandId) {
        //根据 brandId 查出该品牌所有 "品牌"-"分类"间的关系
        return baseMapper.selectList(
                new QueryWrapper<CategoryBrandRelationEntity>().eq("brand_id", brandId)
        );
    }

    @Override
    public void saveDetial(CategoryBrandRelationEntity categoryBrandRelation) {
        CategoryEntity category = categoryService.getById(categoryBrandRelation.getCatelogId());
        BrandEntity brand = brandService.getById(categoryBrandRelation.getBrandId());

        //补全冗余列
        categoryBrandRelation.setBrandName(brand.getName());
        categoryBrandRelation.setCatelogName(category.getName());

        this.save(categoryBrandRelation);

    }

    /**
     * @Description:gms_brand中的brand_name字段变化也更新pms_category_brand_relation表中的brand_name字段
     *
     */
    @Override
    public void updateBrandNameFromBrandChange(Long brandId, String name) {
        CategoryBrandRelationEntity categoryBrandRelationEntity = new CategoryBrandRelationEntity();
        categoryBrandRelationEntity.setBrandId(brandId);
        categoryBrandRelationEntity.setBrandName(name);

        this.update(categoryBrandRelationEntity,
                new QueryWrapper<CategoryBrandRelationEntity>().eq("brand_id", brandId));
    }

    @Override
    public void updateCategoryNameFromCategoryChange(Long catId, String name) {
        //CategoryBrandRelationEntity categoryBrandRelationEntity = new CategoryBrandRelationEntity();
        //categoryBrandRelationEntity.setCatelogId(catId);
        //categoryBrandRelationEntity.setCatelogName(name);
        //this.update(categoryBrandRelationEntity, new UpdateWrapper<CategoryBrandRelationEntity>().eq("catelog_id", catId));

        //这里为了学习  在mapper中编写sql语句
        this.baseMapper.updateCategoryNameFromCategoryChange(catId, name);

    }

    /**
     * @Description: 获取分类关联的品牌
     * 在"发布商品"的"品牌选择"时获取该catelog目录下的所有品牌
     */
    @Override
    public List<BrandEntity> getBrandsBycatId(Long catId) {
        List<CategoryBrandRelationEntity> relationEntities = this.baseMapper.selectList(new QueryWrapper<CategoryBrandRelationEntity>()
                .eq("catelog_id", catId));
        //原视频中是在循环中查表 这样其实在公司是禁止的
        //其实视频中的意思是 relationEntities关于品牌的信息补全 想通过brand_id去pms_brand中查到brandEntity
        List<Long> brandIds = relationEntities.stream().map((relationEntity) -> {
            return relationEntity.getBrandId();
        }).collect(Collectors.toList());
        List<BrandEntity> brandsByIds = brandService.getBrandsByIds(brandIds);
        return brandsByIds;
    }

}