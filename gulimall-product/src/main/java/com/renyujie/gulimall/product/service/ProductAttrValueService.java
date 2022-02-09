package com.renyujie.gulimall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.renyujie.common.utils.PageUtils;
import com.renyujie.gulimall.product.entity.ProductAttrValueEntity;

import java.util.List;
import java.util.Map;

/**
 * spu属性值
 *
 * @author renyujie518
 * @email renyujie518@gmail.com
 * @date 2022-01-20 17:33:10
 */
public interface ProductAttrValueService extends IService<ProductAttrValueEntity> {

    PageUtils queryPage(Map<String, Object> params);

    /**
     * @Description: 获取spu规格  用于"spu管理"点击"规格维护"后的回显
     */
    List<ProductAttrValueEntity> baseListForSpu(Long spuId);

    /**
     * @Description: 修改商品规格  用于"spu管理"点击"规格维护"后 修改后跟新
     */
    void updateSpuAttr(Long spuId, List<ProductAttrValueEntity> entities);
}

