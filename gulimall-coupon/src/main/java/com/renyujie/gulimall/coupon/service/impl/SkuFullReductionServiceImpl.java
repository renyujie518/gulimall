package com.renyujie.gulimall.coupon.service.impl;

import com.renyujie.common.dto.MemberPrice;
import com.renyujie.common.dto.SkuReductionTo;
import com.renyujie.gulimall.coupon.entity.MemberPriceEntity;
import com.renyujie.gulimall.coupon.entity.SkuLadderEntity;
import com.renyujie.gulimall.coupon.service.MemberPriceService;
import com.renyujie.gulimall.coupon.service.SkuLadderService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.renyujie.common.utils.PageUtils;
import com.renyujie.common.utils.Query;

import com.renyujie.gulimall.coupon.dao.SkuFullReductionDao;
import com.renyujie.gulimall.coupon.entity.SkuFullReductionEntity;
import com.renyujie.gulimall.coupon.service.SkuFullReductionService;


@Service("skuFullReductionService")
public class SkuFullReductionServiceImpl extends ServiceImpl<SkuFullReductionDao, SkuFullReductionEntity> implements SkuFullReductionService {

    @Autowired
    SkuLadderService skuLadderService;

    @Autowired
    MemberPriceService memberPriceService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SkuFullReductionEntity> page = this.page(
                new Query<SkuFullReductionEntity>().getPage(params),
                new QueryWrapper<SkuFullReductionEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * @Description: "发布商品"的最终大保存会远程调用此方法用于  保存sku的优惠、满减，会员价格等信息
     * sku的优惠、满减等信息；
     * sms_sku_ladder表;
     * sms_sku_full_reduction表;
     * sms_member_price表
     */
    @Override
    public void saveSkuReduction(SkuReductionTo skuReductionTo) {
        //sms_sku_ladder
        SkuLadderEntity skuLadderEntity = new SkuLadderEntity();
        skuLadderEntity.setSkuId(skuReductionTo.getSkuId());
        skuLadderEntity.setFullCount(skuReductionTo.getFullCount());
        skuLadderEntity.setDiscount(skuReductionTo.getDiscount());
        skuLadderEntity.setAddOther(skuReductionTo.getCountStatus());
        //TODO 价格后续下订单在加
        //有"满几件"代表有打折信息 再存
        if (skuReductionTo.getFullCount() > 0) {
            skuLadderService.save(skuLadderEntity);
        }

        //2、sms_sku_full_reduction
        SkuFullReductionEntity reductionEntity = new SkuFullReductionEntity();
        BeanUtils.copyProperties(skuReductionTo,reductionEntity);
        //有"满多少"代表有优惠信息 再存
        if(reductionEntity.getFullPrice().compareTo(new BigDecimal("0"))==1){
            this.save(reductionEntity);
        }

        //3、sms_member_price
        List<MemberPrice> memberPrice = skuReductionTo.getMemberPrice();
        if (memberPrice != null && memberPrice.size() > 0) {
            List<MemberPriceEntity> collect = memberPrice.stream().map(item -> {
                MemberPriceEntity priceEntity = new MemberPriceEntity();
                priceEntity.setSkuId(skuReductionTo.getSkuId());
                priceEntity.setMemberLevelId(item.getId());
                priceEntity.setMemberLevelName(item.getName());
                priceEntity.setMemberPrice(item.getPrice());
                priceEntity.setAddOther(1);
                return priceEntity;
            }).filter(item->{
                //有"会员价"代表有会员的优惠  保留
                return item.getMemberPrice().compareTo(new BigDecimal("0")) == 1;
            }).collect(Collectors.toList());
            memberPriceService.saveBatch(collect);
        }


    }

}