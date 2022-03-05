package com.renyujie.gulimall.ware.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.renyujie.common.utils.R;
import com.renyujie.gulimall.ware.feign.MemberFeignService;
import com.renyujie.gulimall.ware.vo.FareVo;
import com.renyujie.gulimall.ware.vo.MemberAddressVo;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.renyujie.common.utils.PageUtils;
import com.renyujie.common.utils.Query;

import com.renyujie.gulimall.ware.dao.WareInfoDao;
import com.renyujie.gulimall.ware.entity.WareInfoEntity;
import com.renyujie.gulimall.ware.service.WareInfoService;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;


@Service("wareInfoService")
public class WareInfoServiceImpl extends ServiceImpl<WareInfoDao, WareInfoEntity> implements WareInfoService {
    @Resource
    MemberFeignService memberFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        QueryWrapper<WareInfoEntity> wrapper = new QueryWrapper<>();
        String key = (String) params.get("key");
        //所有项都支持模糊查询  但是从左往右  优先id和name字段
        if (!StringUtils.isEmpty(key)) {
            wrapper.eq("id", key)
                    .or().like("name", key)
                    .or().like("address", key)
                    .or().like("areacode", key);
        }

        IPage<WareInfoEntity> page = this.page(
                new Query<WareInfoEntity>().getPage(params), wrapper
        );

        return new PageUtils(page);
    }

    /**
     * 根据用户的收货地址计算运费
     * 地址信息保存在member服务中
     */
    @Override
    public FareVo getFare(Long addrId) {
        FareVo fareVo = new FareVo();

        //远程查询用户地址信息
        R r = memberFeignService.addrInfo(addrId);
        if (r.getCode() == 0) {
            MemberAddressVo data = r.getData("memberReceiveAddress", new TypeReference<MemberAddressVo>() {});
            if (data != null) {
                //FareVo第1个属性
                fareVo.setAddress(data);
                /**简单处理 手机号末位当作运费**/
                String phone = data.getPhone();
                //123456789 9
                String substring = phone.substring(phone.length() - 1, phone.length());
                BigDecimal fare = new BigDecimal(substring);
                //FareVo第2个属性
                fareVo.setFare(fare);
                return fareVo;
            }
        }
        return null;
    }

}