package com.renyujie.gulimall.product.service.impl;

import com.renyujie.common.constant.ProductConstant;
import com.renyujie.gulimall.product.dao.AttrAttrgroupRelationDao;
import com.renyujie.gulimall.product.dao.AttrGroupDao;
import com.renyujie.gulimall.product.dao.CategoryDao;
import com.renyujie.gulimall.product.entity.AttrAttrgroupRelationEntity;
import com.renyujie.gulimall.product.entity.AttrGroupEntity;
import com.renyujie.gulimall.product.entity.CategoryEntity;
import com.renyujie.gulimall.product.service.AttrAttrgroupRelationService;
import com.renyujie.gulimall.product.service.CategoryService;
import com.renyujie.gulimall.product.vo.AttrGroupRelationVo;
import com.renyujie.gulimall.product.vo.AttrRespVo;
import com.renyujie.gulimall.product.vo.AttrVo;
import org.apache.commons.lang.ArrayUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.renyujie.common.utils.PageUtils;
import com.renyujie.common.utils.Query;

import com.renyujie.gulimall.product.dao.AttrDao;
import com.renyujie.gulimall.product.entity.AttrEntity;
import com.renyujie.gulimall.product.service.AttrService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;


@Service("attrService")
public class AttrServiceImpl extends ServiceImpl<AttrDao, AttrEntity> implements AttrService {
    @Resource
    AttrAttrgroupRelationDao attrAttrgroupRelationDao;

    //@Resource
    //AttrAttrgroupRelationService attrAttrgroupRelationService;

    @Resource
    AttrGroupDao attrGroupDao;

    @Resource
    CategoryDao categoryDao;

    @Resource
    CategoryService categoryService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<AttrEntity> page = this.page(
                new Query<AttrEntity>().getPage(params),
                new QueryWrapper<AttrEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * @Description: 保存"规格参数"页面 新增属性的save方法
     */
    @Transactional
    @Override
    public void saveAttr(AttrVo attrVo) {
        AttrEntity attrEntity = new AttrEntity();
        //VO转DO
        BeanUtils.copyProperties(attrVo, attrEntity);

        //首先保存到pms_attr表中
        this.save(attrEntity);

        //再保存pms_attr_attrgroup_relation  "attr_id"和"attr_group_id"
        /**注意：销售属性不存在分组的  只有基本属性base才会有**/
        if (attrVo.getAttrType() == ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode() && attrVo.getAttrGroupId() != null) {
            AttrAttrgroupRelationEntity relationEntity = new AttrAttrgroupRelationEntity();
            relationEntity.setAttrGroupId(attrVo.getAttrGroupId());
            relationEntity.setAttrId(attrEntity.getAttrId());
            attrAttrgroupRelationDao.insert(relationEntity);
        }

    }

    /**
     * @Description: "规格参数"页面获取基本属性
     * attrType有两种可能   "base"/"sale"  在pms_attr中对应0：销售属性sale    1:基本属性base
     */
    @Override
    public PageUtils queryBaseAttrPage(Map<String, Object> params, Long catelogId, String attrType) {
        //在构造wrapper的时候就区分是"base"还是"sale"
        QueryWrapper<AttrEntity> wrapper = new QueryWrapper<AttrEntity>().eq("attr_type",
                "base".equalsIgnoreCase(attrType) ?
                        ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode() :
                        ProductConstant.AttrEnum.ATTR_ENUM_SALE.getCode());
        //前端带catelogId 就查询指定,否则就查询全部，所以首先就在wrapper中判断catelogId
        if (catelogId != 0) {
            wrapper.eq("catelog_id", catelogId);
        }
        //处理模糊查询
        String key = String.valueOf(params.get("key"));
        if (!StringUtils.isEmpty(key)) {
            wrapper.and((wrap) -> {
                wrap.eq("attr_id", key).or().like("attr_name", key);
            });
        }
        IPage<AttrEntity> page = this.page(
                new Query<AttrEntity>().getPage(params),
                wrapper
        );
        //查看接口文档 返回字段还包括   catelogName：所属分类名字  groupName ：所属分组名字
        //这两个字段 明显要去联合表中查  然后在page中依次封装新的vo
        PageUtils pageResponse = new PageUtils(page);
        List<AttrEntity> records = page.getRecords();
        List<AttrRespVo>  attrRespVoResponse = records.stream().map((attrEntity) -> {
            AttrRespVo attrRespVo = new AttrRespVo();
            //基本字段先复制过去
            BeanUtils.copyProperties(attrEntity, attrRespVo);

            //补充groupName字段
            /**注意：销售属性不存在分组的  只有基本属性base才会有**/
            if ("base".equalsIgnoreCase(attrType)){
                AttrAttrgroupRelationEntity attr_idEntity = attrAttrgroupRelationDao.selectOne(
                        new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attrEntity.getAttrId()));
                /**
                 ！！！！！！！！
                 当一个DAO查出的结果紧接着被另个一DAO用到或者当参数参数传入的时候 别不下面的情况 最好做参数非空校验  否则容易空指针异常
                 ！！！！！！！！
                 **/
                if (attr_idEntity != null && attr_idEntity.getAttrGroupId() != null) {
                    AttrGroupEntity attrGroupEntity = attrGroupDao.selectById(attr_idEntity.getAttrGroupId());
                    attrRespVo.setGroupName(attrGroupEntity.getAttrGroupName());
                }
            }

            //补充catelogName字段
            CategoryEntity categoryEntity = categoryDao.selectById(attrEntity.getCatelogId());
            if (categoryEntity != null) {
                attrRespVo.setCatelogName(categoryEntity.getName());
            }

            return attrRespVo;

        }).collect(Collectors.toList());

        pageResponse.setList(attrRespVoResponse);
        return pageResponse;
    }

    /**
     * @Description:"规格参数"新增页面回显
     */
    @Override
    public AttrRespVo getAttrInfo(Long attrId) {
        AttrRespVo attrRespVo = new AttrRespVo();
        //首先把attrId所对应的基本数据移植到vo中
        AttrEntity attrEntity = this.getById(attrId);
        BeanUtils.copyProperties(attrEntity, attrRespVo);

        //补全AttrGroupId
        /**注意：销售属性不存在分组的  只有基本属性base才会有 AttrType() = 1**/
        if (attrEntity.getAttrType() == ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode()) {
            AttrAttrgroupRelationEntity relationEntity = attrAttrgroupRelationDao.selectOne(
                    new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attrId));
            if (relationEntity != null) {
                attrRespVo.setAttrGroupId(relationEntity.getAttrGroupId());
                AttrGroupEntity attrGroupEntity = attrGroupDao.selectById(relationEntity.getAttrGroupId());
                //补全GroupName
                if (attrGroupEntity != null) {
                    attrRespVo.setGroupName(attrGroupEntity.getAttrGroupName());
                }
            }
        }

        //补全catelogPath
        Long[] catelogPath = categoryService.findCatelogPath(attrEntity.getCatelogId());
        if (!ArrayUtils.isEmpty(catelogPath)) {
            attrRespVo.setCatelogPath(catelogPath);
        }
        //补全CatelogName
        CategoryEntity categoryEntity = categoryDao.selectById(attrEntity.getCatelogId());
        if (categoryEntity != null) {
            attrRespVo.setCatelogName(categoryEntity.getName());
        }

        return attrRespVo;
    }

    /**
     *  @Description:"规格参数"有内容变化时更新表单
     */
    @Transactional
    @Override
    public void updateAttr(AttrVo attrVo) {
        AttrEntity attrEntity = new AttrEntity();
        //把前端传来的VO移植到attrEntity中(updateById方法必须传入entity)
        BeanUtils.copyProperties(attrVo, attrEntity);

        //首先要跟新pms_attr表
        this.updateById(attrEntity);
        //更新pms_attr_attrgroup_relation表
        /**注意：销售属性不存在分组的 所以就不存在跟新relation表  只有基本属性base才会有 AttrType() = 1**/
        if (attrEntity.getAttrType() == ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode()) {
            AttrAttrgroupRelationEntity relationEntity = new AttrAttrgroupRelationEntity();
            relationEntity.setAttrGroupId(attrVo.getAttrGroupId());
            relationEntity.setAttrId(attrVo.getAttrId());
            /**
             * 前端传来的VO到底是跟新还是插入  由attr_id列是否存在决定
             * 这里弹幕表示可能有高并发问题  思考了下确实 先查count后更新的确是会有并发问题，可能重复插两次
             * 虽然有  @Transactional事务注解 但是如下的写法会多查一次数据库
             * 所以不如干净利落的用saveOrUpdate
             *
             * 但是我验证了，跟新也会导致新增
             * 原因：saveOrUpdate不可以的，其是根据是否有主键来判断的
             * 所以会新增1条  而这个pms_attr_attrgroup_relation关系表是一对一的  一个属性在一个属性组
             * 而queryBaseAttrPage方法中用的一直是是eq("attr_id"),如果两个attr_id在一个groupid里就直接导致查到多条，前端不显示
             */
            Integer count = attrAttrgroupRelationDao.selectCount(
                    new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attrVo.getAttrId())
            );
            if (count > 0) {
                //更新即可
                attrAttrgroupRelationDao.update(relationEntity,
                        new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attrVo.getAttrId()));
            } else {
                attrAttrgroupRelationDao.insert(relationEntity);
            }
            //attrAttrgroupRelationService.saveOrUpdate(relationEntity);
        }
    }

    /**
     * @Description: 获取指定分组关联的所有属性
     * pms_attr_attrgroup_relation表中 一个attr_group_id对应多个attr_id  即一个分组下有多个属性
     */
    @Override
    public List<AttrEntity> getAttrsFromGroupId(Long attrgroupId) {
        //先从pms_attr_attrgroup_relation表中 获取多个attr_id
        QueryWrapper<AttrAttrgroupRelationEntity> relationWrapper = new QueryWrapper<>();
        List<AttrAttrgroupRelationEntity> relationEntities = attrAttrgroupRelationDao.selectList(relationWrapper.eq("attr_group_id", attrgroupId));
        List<Long> attrIds = relationEntities.stream().map((entity) -> {
            return entity.getAttrId();
        }).collect(Collectors.toList());
        if (attrIds == null || attrIds.size() == 0) {
            return null;
        }
        Collection<AttrEntity> attrEntities = this.listByIds(attrIds);
        return (List<AttrEntity>) attrEntities;

    }


    /**
     * @Description: 删除属性与分组的关联关系   与attrRelation方法对应
     * 一个attr_group_id对应多个attr_id  即一个分组下有多个属性  删除这组关系
     *
     * 这里有两点注意：
     * 1.虽然该方法是在attrService 但是其处理的其实是relation表 所以下面才会有用 attrAttrgroupRelationDao
     * 2.为了避免多次请求数据库  设定了deleteBatchRelation方法 其中用了or
     * 达成如下的效果：delete from ` pms_attr_attrgroup_relation` where (attr_id=1 and attr_group_id=1) or (attr_id=2 and attr_group_id=3)
     * 不可以用in,因为不是单字段   https://blog.csdn.net/Guanjs2016/article/details/80237490
     * 不确定前端勾选传回来的是哪几个数组
     * 但是实际工作慎用or or 需要查全表，性能大打折扣 or和in都会导致索引失效，也就是全表扫描，可以用union代替or
     */
    @Override
    public void deleteAttrRelation(AttrGroupRelationVo[] attrGroupRelationVos) {

        List<AttrAttrgroupRelationEntity> relationEntities = Arrays.asList(attrGroupRelationVos).stream().map((vo) -> {
            AttrAttrgroupRelationEntity relationEntity = new AttrAttrgroupRelationEntity();
            BeanUtils.copyProperties(vo, relationEntity);
            return relationEntity;
        }).collect(Collectors.toList());
        attrAttrgroupRelationDao.deleteBatchAttrRelation(relationEntities);
    }


    /**
     * @Description: 获取属性分组没有关联的其他属性
     * 在"属性分组"页面的"关联"中的"新增关联"时  需要获取该分组下没有关联的其他属性 而且有分类category的要求
     */
    @Override
    public PageUtils getNoRelationAttr(Map<String, Object> params, Long attrGroupId) {
        /**1.当前group只能关联 所属分类category中的所有属性**/
        //group与category的关系记录在pms_attr_group表中 找到对应的分类
        AttrGroupEntity attrGroupEnity = attrGroupDao.selectById(attrGroupId);
        Long selfCatelogId = attrGroupEnity.getCatelogId();
        /**2.当前group只能关联 别的category没有引用的attr
         有个原则：1group内多attr，但是相当于按人头归类 某一种attr只能归类于1个group
         同理：1category下有多个group  记录在pms_attr_group表中 某一种group只能归类于1个category
         * **/
        //2.1 当前category下的所有group
        List<AttrGroupEntity> allGroupWithSelfCatelogId = attrGroupDao.selectList(new QueryWrapper<AttrGroupEntity>().eq("catelog_id", selfCatelogId));
        List<Long> allGroupIds = allGroupWithSelfCatelogId.stream().map((entity) -> {
            return entity.getAttrGroupId();
        }).collect(Collectors.toList());
        //2.2 这些group的关联的所有attr
        List<AttrAttrgroupRelationEntity> relationEntities = attrAttrgroupRelationDao.selectList(new QueryWrapper<AttrAttrgroupRelationEntity>().in("attr_group_id", allGroupIds));
        List<Long> allAttrIds = relationEntities.stream().map((entity) -> {
            return entity.getAttrId();
        }).collect(Collectors.toList());
        //2.3 从当前catelog的所有atrr中剔除这些属性allAttrIds  同时保证只是base而非sale的属性
        QueryWrapper<AttrEntity> finalWrapper = new QueryWrapper<AttrEntity>().eq("catelog_id", selfCatelogId)
                .eq("attr_type", ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode());
        //真正的剔除前要做非空判断
        if (allAttrIds != null && allAttrIds.size() > 0) {
            finalWrapper.notIn("attr_id", allAttrIds);
        }//最内层处理搜索的模糊查询
        String key = (String)params.get("key");
        if (key != null) {
            finalWrapper.and((obj)->{
                obj.eq("attr_id", key).or().like("attr_name", key);
            });
        }

        //封装
        IPage<AttrEntity> page = this.page(new Query<AttrEntity>().getPage(params), finalWrapper);
        PageUtils pageUtils = new PageUtils(page);
        return pageUtils;

    }

}