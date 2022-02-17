package com.renyujie.gulimall.product.app;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.renyujie.gulimall.product.entity.AttrEntity;
import com.renyujie.gulimall.product.service.AttrAttrgroupRelationService;
import com.renyujie.gulimall.product.service.AttrService;
import com.renyujie.gulimall.product.service.CategoryService;
import com.renyujie.gulimall.product.vo.AttrGroupRelationVo;
import com.renyujie.gulimall.product.vo.AttrGroupWithAttrsVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.renyujie.gulimall.product.entity.AttrGroupEntity;
import com.renyujie.gulimall.product.service.AttrGroupService;
import com.renyujie.common.utils.PageUtils;
import com.renyujie.common.utils.R;

import javax.annotation.Resource;


/**
 * 属性分组
 *
 * @author renyujie518
 * @email renyujie518@gmail.com
 * @date 2022-01-20 18:42:34
 */
@RestController
@RequestMapping("product/attrgroup")
public class AttrGroupController {
    @Autowired
    private AttrGroupService attrGroupService;
    @Resource
    private CategoryService categoryService;
    @Resource
    private AttrService attrService;
    @Resource
    private AttrAttrgroupRelationService attrAttrgroupRelationService;

    /**
     * 通过id查询分页list
     */
    @RequestMapping("/list/{catelogId}")
    public R list(@RequestParam Map<String, Object> params, @PathVariable("catelogId") Long catelogId) {
        PageUtils page = attrGroupService.queryPage(params, catelogId);

        return R.ok().put("page", page);
    }


    /**
     * 获取attrGroup分组信息
     */
    @RequestMapping("/info/{attrGroupId}")
    public R info(@PathVariable("attrGroupId") Long attrGroupId) {
        //首先找到所属分类catelogId
        AttrGroupEntity attrGroup = attrGroupService.getById(attrGroupId);
        Long catelogId = attrGroup.getCatelogId();
        //根据当前所属分类catelogId找到完整路径 [父,子，孙] 比如[2,25,225]
        Long[] catelogPath = categoryService.findCatelogPath(catelogId);
        attrGroup.setCatelogPath(catelogPath);
        return R.ok().put("attrGroup", attrGroup);
    }

    /**
     * @Description: 获取分类下所有分组&关联属性
     * "发布商品"的第二步"规格参数"需要获取1category下的所有group,每个group下又要获取所有的attr
     * 1、查出当前分类下的所有属性分组，
     * 2、查出每个属性分组的所有属性
     */
    @GetMapping("/{catelogId}/withattr")
    public R attrGroupWithAttrs(@PathVariable("catelogId") Long catelogId) {
        List<AttrGroupWithAttrsVo> vos = attrGroupService.getAttrGroupWithAttrsByCatelogId(catelogId);
        //这里fileter解释参见spuadd.vue 680行的注释  filter满足表达式的留下  https://blog.csdn.net/Hatsune_Miku_/article/details/73432351
        vos.stream().filter(vo -> Objects.nonNull(vo.getAttrs()));
        return R.ok().put("data", vos);
    }


    /**
     * @Description: 获取指定分组关联的所有属性
     * pms_attr_attrgroup_relation表中 一个attr_group_id对应多个attr_id  即一个分组下有多个属性
     */
    @GetMapping("/{attrgroupId}/attr/relation")
    public R attrRelation(@PathVariable("attrgroupId") Long attrgroupId) {
        List<AttrEntity> entities = attrService.getAttrsFromGroupId(attrgroupId);
        return R.ok().put("data", entities);
    }

    /**
     * @Description: 获取属性分组没有关联的其他属性
     * 在"属性分组"页面的"关联"中的"新增关联"时  需要获取该分组下没有关联的其他属性 而且有分类category的要求
     */
    @GetMapping("/{attrgroupId}/noattr/relation")
    public R attrNoRelation(
            @RequestParam Map<String, Object> params,
            @PathVariable("attrgroupId") Long attrGroupId) {
        PageUtils page = attrService.getNoRelationAttr(params, attrGroupId);
        return R.ok().put("page", page);
    }


    /**
     * @Description: 添加属性与分组关联关系
     * 在"属性分组"页面的"关联"中的"新增关联"时"确认新增"的提交
     * 实际上操作的是pms_attr_attrgroup_relation表  所以引入attrAttrgroupRelationService
     */
    @PostMapping("attr/relation")
    public R addRelation(@RequestBody List<AttrGroupRelationVo> vos) {
        attrAttrgroupRelationService.saveBatchWhenGroup(vos);
        return R.ok();
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R save(@RequestBody AttrGroupEntity attrGroup) {
        attrGroupService.save(attrGroup);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@RequestBody AttrGroupEntity attrGroup) {
        attrGroupService.updateById(attrGroup);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] attrGroupIds) {
        attrGroupService.removeByIds(Arrays.asList(attrGroupIds));

        return R.ok();
    }

    /**
     * @Description: 删除属性与分组的关联关系   与attrRelation方法对应
     * 一个attr_group_id对应多个attr_id  即一个分组下有多个属性  删除这组关系
     */
    @PostMapping("/attr/relation/delete")
    public R deleteAttrRelation(@RequestBody AttrGroupRelationVo attrGroupRelationVos[]) {
        attrService.deleteAttrRelation(attrGroupRelationVos);
        return R.ok();
    }

}
