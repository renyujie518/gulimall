package com.renyujie.gulimall.product.dao;

import com.renyujie.gulimall.product.entity.SpuInfoEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * spu信息
 * 
 * @author renyujie518
 * @email renyujie518@gmail.com
 * @date 2022-01-20 17:33:09
 */
@Mapper
public interface SpuInfoDao extends BaseMapper<SpuInfoEntity> {

    /**
     * @Description: 修改当前spu的publish_status状态   和跟新时间
     * 0:"新建",  1:"商品上架"     2:"商品下架"
     */
    void updateSpuStatus(@Param("spuId") Long spuId, @Param("code") int code);
}
