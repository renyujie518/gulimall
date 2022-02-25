package com.renyujie.gulimall.product.service.impl;

import com.renyujie.gulimall.product.entity.SkuImagesEntity;
import com.renyujie.gulimall.product.entity.SpuInfoDescEntity;
import com.renyujie.gulimall.product.service.*;
import com.renyujie.gulimall.product.vo.SkuItemSaleAttrVo;
import com.renyujie.gulimall.product.vo.SkuItemVo;
import com.renyujie.gulimall.product.vo.SpuItemAttrGroupVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.renyujie.common.utils.PageUtils;
import com.renyujie.common.utils.Query;

import com.renyujie.gulimall.product.dao.SkuInfoDao;
import com.renyujie.gulimall.product.entity.SkuInfoEntity;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;


@Service("skuInfoService")
public class SkuInfoServiceImpl extends ServiceImpl<SkuInfoDao, SkuInfoEntity> implements SkuInfoService {
    @Resource
    SkuImagesService skuImagesService;
    @Resource
    SpuInfoDescService spuInfoDescService;
    @Resource
    AttrGroupService attrGroupService;
    @Resource
    SkuSaleAttrValueService skuSaleAttrValueService;
    //注入线程池  因为已经在MyThreadConfig中new ThreadPoolExecutor 的时候@Component注入容器了，所以可以直接用
    @Autowired
    ThreadPoolExecutor executor;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SkuInfoEntity> page = this.page(
                new Query<SkuInfoEntity>().getPage(params),
                new QueryWrapper<SkuInfoEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * "商品管理"页面获取详情  带模糊检索
     */
    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {

        QueryWrapper<SkuInfoEntity> wrapper = new QueryWrapper<>();
        /**
         * key:  搜索框填入
         * catelogId: 0  前端传来的分类id
         * brandId: 0  前端传来的品牌id
         * min: 0  价格低区间
         * max: 0  价格高区间
         */
        String key = (String) params.get("key");
        if (!StringUtils.isEmpty(key)) {
            wrapper.and((obj) -> {
                obj.eq("sku_id", key).or().like("sku_name", key);
            });
        }

        String catelogId = (String) params.get("catelogId");
        if (!StringUtils.isEmpty(catelogId) && !"0".equalsIgnoreCase(catelogId)) {

            wrapper.eq("catalog_id", catelogId);
        }

        String brandId = (String) params.get("brandId");
        if (!StringUtils.isEmpty(brandId) && !"0".equalsIgnoreCase(brandId)) {
            wrapper.eq("brand_id", brandId);
        }

        String min = (String) params.get("min");
        if (!StringUtils.isEmpty(min)) {
            //ge  >=   相当于 and price >= ?
            wrapper.ge("price", min);
        }

        String max = (String) params.get("max");
        if (!StringUtils.isEmpty(max)) {
            try {
                //try-catch是防止前端传如字母这样不对的值  有问题不抛出但不管
                BigDecimal max2BigDecimal = new BigDecimal(max);
                //相当于 max的值>0
                if (max2BigDecimal.compareTo(new BigDecimal("0")) == 1) {
                    //le <=  相当于 and price <= ?
                    wrapper.le("price", max);
                }
            } catch (Exception e) {

            }

        }


        IPage<SkuInfoEntity> page = this.page(
                new Query<SkuInfoEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

    /**
     * @Description:  通过spuId获得对应的所有sku
     */
    @Override
    public List<SkuInfoEntity> getSkusById(Long spuId) {

        List<SkuInfoEntity> list = this.list(new QueryWrapper<SkuInfoEntity>().eq("spu_id", spuId));
        return list;

    }

    /**
     * @Description: 返回sku详细信息（sku->spu 再通过spu得到旗下所有的sku,再来得到这些所有sku的attr_name:attr_value）
     * 1 sku基本信息获取 pms_sku_info
     * 2 sku图片信息 pms_sku_images
     * 3 spu旗下的所有销售属性组合
     * 4 spu的介绍图片 pms_spu_info_desc
     * 5 spu的规格参数信息
     *
     * 本方法需要异步编排处理
     * 1 2 没关系 查谁都行
     * 3 4 5 必须依赖 1（1 -> 3、4、5）
     * 3 4 5 是并列的，得等 1 完成
     * 2 又和 1 没关系
     */
    @Override
    public SkuItemVo item(Long skuId) throws ExecutionException, InterruptedException {
        //要返回给前端的大对象
        SkuItemVo skuItemVo = new SkuItemVo();
        CompletableFuture<SkuInfoEntity> infoFuture = CompletableFuture.supplyAsync(() -> {
            /** 1 sku基本信息获取 pms_sku_info  直接通过主键查询*/
            SkuInfoEntity skuInfoEntity = this.getById(skuId);
            skuItemVo.setInfo(skuInfoEntity);
            return skuInfoEntity;
        }, executor);

        CompletableFuture<Void> saleFuture = infoFuture.thenAcceptAsync((result) -> {
            /** 3 spu旗下的所有销售属性组合*/
            List<SkuItemSaleAttrVo> saleAttrVos = skuSaleAttrValueService.getSaleAttrBySpuId(result.getSpuId());
            skuItemVo.setSaleAttr(saleAttrVos);
        }, executor);

        CompletableFuture<Void> descFuture = infoFuture.thenAcceptAsync((result) -> {
            /** 4 spu的介绍图片 pms_spu_info_desc  直接通过主键查询*/
            SpuInfoDescEntity spuInfoDescEntity = spuInfoDescService.getById(result.getSpuId());
            skuItemVo.setDesc(spuInfoDescEntity);
        }, executor);

        CompletableFuture<Void> attrGroupFuture = infoFuture.thenAcceptAsync((result) -> {
            /**  5 spu的规格参数信息*/
            List<SpuItemAttrGroupVo> spuItemAttrGroup =
                    attrGroupService.getAttrGroupWithAttrsBySpuIdAndCatalogId(result.getSpuId(), result.getCatalogId());
            skuItemVo.setGroupAttrs(spuItemAttrGroup);
        }, executor);


        CompletableFuture<Void> imgFuture = CompletableFuture.runAsync(() -> {
            /** 2 sku图片信息 pms_sku_images  直接通过主键查询*/
            List<SkuImagesEntity> skuImages = skuImagesService.getImagsesBuSkuId(skuId);
            skuItemVo.setImages(skuImages);
        }, executor);

        //异步编排 - 阻塞等结果
        //等待所有1 2 3 4 5任务都完成 infoFuture可以省略(因为345都完成了，1肯定完成了)
        CompletableFuture.allOf(saleFuture, descFuture, attrGroupFuture, imgFuture).get();

        return skuItemVo;
    }

}