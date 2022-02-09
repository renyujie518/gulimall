package com.renyujie.gulimall.ware.service.impl;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.renyujie.common.utils.PageUtils;
import com.renyujie.common.utils.Query;

import com.renyujie.gulimall.ware.dao.PurchaseDetailDao;
import com.renyujie.gulimall.ware.entity.PurchaseDetailEntity;
import com.renyujie.gulimall.ware.service.PurchaseDetailService;
import org.springframework.util.StringUtils;


@Service("purchaseDetailService")
public class PurchaseDetailServiceImpl extends ServiceImpl<PurchaseDetailDao, PurchaseDetailEntity> implements PurchaseDetailService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {

        QueryWrapper<PurchaseDetailEntity> wrapper = new QueryWrapper<>();

        String key = (String)params.get("key");
        if (!StringUtils.isEmpty(key)) {
            //采购单id  purchase_id
            wrapper.and((obj)->{
                obj.eq("sku_id", key).or().eq("purchase_id", key);
            });
        }

        String status = (String)params.get("status");
        if (!StringUtils.isEmpty(status)) {
            wrapper.eq("status", status);
        }

        String wareId = (String)params.get("wareId");
        if (!StringUtils.isEmpty(wareId)) {
            wrapper.eq("ware_id", wareId);
        }

        IPage<PurchaseDetailEntity> page = this.page(
                new Query<PurchaseDetailEntity>().getPage(params), wrapper
        );

        return new PageUtils(page);
    }

    /**
     * @Description: 由于多个detail可能被merge到一个Purchase下 所以这里查询该purchaseId下所有的detail
     */
    @Override
    public List<PurchaseDetailEntity> DetailListByPurchaseId(Long purchaseId) {
        QueryWrapper<PurchaseDetailEntity> wrapper = new QueryWrapper<PurchaseDetailEntity>().eq("purchase_id", purchaseId);
        List<PurchaseDetailEntity> purchaseDetailEntities = this.list(wrapper);
        return purchaseDetailEntities;
    }

}