package com.renyujie.gulimall.ware.service.impl;

import com.renyujie.common.utils.R;
import com.renyujie.gulimall.ware.feign.ProductFeignService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.renyujie.common.utils.PageUtils;
import com.renyujie.common.utils.Query;

import com.renyujie.gulimall.ware.dao.WareSkuDao;
import com.renyujie.gulimall.ware.entity.WareSkuEntity;
import com.renyujie.gulimall.ware.service.WareSkuService;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;


@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {

    @Autowired
    ProductFeignService productFeignService;


    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        // wareId: 123,//仓库id
        // skuId: 123//商品id
        QueryWrapper<WareSkuEntity> wrapper = new QueryWrapper<>();

        String wareId = (String) params.get("wareId");
        if (!StringUtils.isEmpty(wareId)) {
            wrapper.eq("ware_id", wareId);
        }

        String skuId = (String) params.get("skuId");
        if (!StringUtils.isEmpty(skuId)) {
            wrapper.eq("sku_id", skuId);
        }

        IPage<WareSkuEntity> page = this.page(
                new Query<WareSkuEntity>().getPage(params), wrapper
        );

        return new PageUtils(page);
    }

    /**
     * @Description: 采购成功的sku更新入库
     * skuId 对应 sku_id
     * wareId 对应 ware_id
     * skuNum 对应 stock
     *
     * sku_name  通过feign去查
     * stock_locked  锁定库存默认为0
     */
    @Override
    public void addStock(Long skuId, Long wareId, Integer skuNum) {
        //依据skuid和仓库id精准查
        List<WareSkuEntity> wareSkuEntities = baseMapper.selectList(
                new QueryWrapper<WareSkuEntity>().eq("sku_id", skuId).eq("ware_id", wareId));

        //如果还没有这个库存记录"insert"  否则是"update"
        if (wareSkuEntities == null || wareSkuEntities.size() == 0) {
            WareSkuEntity skuEntity = new WareSkuEntity();
            skuEntity.setSkuId(skuId);
            skuEntity.setStock(skuNum);
            skuEntity.setWareId(wareId);
            skuEntity.setStockLocked(0);
            //远程查询sku的名字，如果失败，整个事务无需回滚(用trt_catch抓到异常不抛出，因为此导致的异常@Transactional注解失效)
            //TODO 让异常出现以后不回滚？高级 sential
            try {
                R info = productFeignService.info(skuId);
                Map<String, Object> data = (Map<String, Object>) info.get("skuInfo");
                if (info.getCode() == 0) {
                    //code代表无异常  具体 为什么这么取去看product.controller.SkuInfoController.info方法
                    skuEntity.setSkuName((String) data.get("skuName"));
                }
            } catch (Exception e) {
            }

            baseMapper.insert(skuEntity);
        } else {
            baseMapper.addStock(skuId, wareId, skuNum);
        }
    }

}