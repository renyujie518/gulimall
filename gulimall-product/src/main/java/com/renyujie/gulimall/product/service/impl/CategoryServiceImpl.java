package com.renyujie.gulimall.product.service.impl;

import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.renyujie.common.utils.PageUtils;
import com.renyujie.common.utils.Query;

import com.renyujie.gulimall.product.dao.CategoryDao;
import com.renyujie.gulimall.product.entity.CategoryEntity;
import com.renyujie.gulimall.product.service.CategoryService;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * @Description: 查出所有分类以及子分类，以树形结构组装起来
     * 备注：
     * (c1, c2) -> { return (c1.getSort() == null ? 0 : c1.getSort()) - (c2.getSort() == null ? 0 : c2.getSort());
     * 按option+/优化成下面的comparingInt 意思与上面的一致
     */
    @Override
    public List<CategoryEntity> listWithTree() {
        //首先查出所有的分类  注意  由于extends ServiceImpl  其中泛型最顶层的父是baseMapper  所以直接用baseMapper即可
        List<CategoryEntity> allEntities = baseMapper.selectList(null);
        //要从所有分类中递归的找到该分类下的子分类  依据parentCid判断  1为一级
        List<CategoryEntity> listWithTree = allEntities.stream().filter(categoryEntity ->
                categoryEntity.getParentCid() == 0
        ).map((category) -> {
            category.setChildren(getCurrentMenuChilren(category, allEntities));
            return category;
        }).sorted(
                //升序
                Comparator.comparingInt(c -> (c.getSort() == null ? 0 : c.getSort()))
        ).collect(Collectors.toList());
        return listWithTree;
    }

    /**
     * @Description: 逻辑删除
     */
    @Override
    public void removeCategoryByIds(List<Long> idList) {
        // TODO 1 检查当前删除的菜单，是否被别的地方引用
        // 逻辑删除
        baseMapper.deleteBatchIds(idList);
    }

    /**
     * @param all 所有的目录
     * @Description: 递归查找当前目录curr的所有子目录children
     */
    private List<CategoryEntity> getCurrentMenuChilren(CategoryEntity curr, List<CategoryEntity> all) {
        List<CategoryEntity> children = all.stream().filter(category -> {
            //对于all中的每个实体  其ParentCid = 当前目录的id  即该实体为curr的children
            // 注意此处应该用longValue()来比较，否则会出先bug，因为parentCid和catId是long类型
            return category.getParentCid().longValue() == curr.getCatId().longValue();
        }).map(category -> {
            //children可能还有children 所以做递归
            category.setChildren(getCurrentMenuChilren(category, all));
            return category;
        }).sorted(
                //升序
                Comparator.comparingInt(c -> (c.getSort() == null ? 0 : c.getSort()))
        ).collect(Collectors.toList());
        return children;
    }

}