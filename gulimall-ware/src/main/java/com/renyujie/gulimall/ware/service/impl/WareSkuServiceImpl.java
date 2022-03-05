package com.renyujie.gulimall.ware.service.impl;

import com.renyujie.common.exception.NoStockException;
import com.renyujie.common.utils.R;
import com.renyujie.gulimall.ware.feign.ProductFeignService;
import com.renyujie.gulimall.ware.vo.OrderItemVo;
import com.renyujie.gulimall.ware.vo.SkuHasStockVo;
import com.renyujie.gulimall.ware.vo.WareSkuLockVo;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.renyujie.common.utils.PageUtils;
import com.renyujie.common.utils.Query;

import com.renyujie.gulimall.ware.dao.WareSkuDao;
import com.renyujie.gulimall.ware.entity.WareSkuEntity;
import com.renyujie.gulimall.ware.service.WareSkuService;
import org.springframework.transaction.annotation.Transactional;
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

    /**
     * @Description: 批量查询sku是否有库存
     */
    @Override
    public List<SkuHasStockVo> getSkuHasStock(List<Long> skuIds) {
        List<SkuHasStockVo> collect = skuIds.stream().map((skuId) -> {
            SkuHasStockVo vo = new SkuHasStockVo();
            //select sum(stock-stock_locked) from wms_ware_sku where sku_id=1
            Long count = baseMapper.getSkuStock(skuId);
            vo.setSkuId(skuId);
            vo.setHasStock(count == null ? false : count > 0);
            //查询当前sku的总库存量
            return vo;
        }).collect(Collectors.toList());

        return collect;

    }

    /**
     * 为某个订单锁定库存
     * rollbackFor 代表由于此异常引起的一定要回滚的
     * 默认只要是RuntimeException 都回滚  由于NoStockException继承了运行时异常 所以这里的rollbackFor加与不加的效果一样
     * 但是根据阿里手册 最好指定上
     */
    @Transactional(rollbackFor = NoStockException.class)
    @Override
    public Boolean orderLockStock(WareSkuLockVo vo) {

        /**1、 本应该按照下单的收货地址，找到一个最近的仓库，锁定库存
         * 但此处不做这么复杂的逻辑 找到每个商品在哪个仓库都有库存（一个商品可能在多个仓库都有库存）
         * 将该需要锁定的sku的有用信息封装到SkuWareHasStock    而且订单传来的是一组待结账的sku  **/
        List<OrderItemVo> locks = vo.getLocks();
        //将vo转化为SkuWareHasStock
        List<SkuWareHasStock> skuWareHasStocks = locks.stream().map(lockItem -> {
            SkuWareHasStock skuWareHasStock = new SkuWareHasStock();
            skuWareHasStock.setSkuId(lockItem.getSkuId());
            //查询出这个sku在那个仓库中有库存（即已有-锁定>0）
            List<Long> wareIds = this.baseMapper.wareIdsHasSkuStock(lockItem.getSkuId());
            skuWareHasStock.setWareId(wareIds);
            skuWareHasStock.setNum(lockItem.getCount());
            return skuWareHasStock;
        }).collect(Collectors.toList());

        /**  2. 对每一个待结账的sku 依据这个人订单中该sku的数量做锁定库存（结账完成前这些商品lock,不能被被人选中）**/
        for (SkuWareHasStock sku : skuWareHasStocks) {
            Boolean skuHasStockedFlag = false;
            Integer needLockNum = sku.getNum();
            Long skuId = sku.getSkuId();
            List<Long> wareIds = sku.getWareId();
            if (wareIds == null || wareIds.size() == 0) {
                //没有任何仓库有该sku的库存
                throw new NoStockException(skuId);
            }
            //一个sku可能在多个仓库都有库存  遍历所有有该sku的仓库
            for (Long wareId : wareIds) {
                //成功 代表影响行数为1  否则相当于update失败  返回0
                Long count = this.baseMapper.lockSkuStockInThisWare(skuId, wareId, needLockNum);
                if (count == 1) {
                    skuHasStockedFlag = true;
                    //TODO 告诉MQ锁定成功 发消息

                    //验证该仓库下是否就有足够的的库存就把sku.num干掉了，也不必再查别的仓库,跳出针对仓库的循环
                    break;
                } else {
                    //count == 0  当前仓库没有解决needLockNum  这里什么都不做 等待下一个wareId去验证
                }
            }
            //执行到这里  所有的仓库遍历完毕（中途break代表解决了needLockNum）再看看skuHasStockedFlag
            if (skuHasStockedFlag == false) {
                //当前商品所有仓库都没有锁住
                throw new NoStockException(skuId);
            }
        }
        //代码能够到这里说明中途没有抛出NoStockException，全部sku都去尝试解决needLockNum 成功
        return true;
    }

    @Data
    class SkuWareHasStock {
        private Long skuId;
        //待结账sku的数量(被锁的数量)
        private Integer num;
        //哪些仓库有库存
        private List<Long> wareId;
    }


}