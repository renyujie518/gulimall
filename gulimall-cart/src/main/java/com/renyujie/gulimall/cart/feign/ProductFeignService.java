package com.renyujie.gulimall.cart.feign;

import com.renyujie.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
  远程商品接口
 */
//告诉spring cloud 这个接口是一个远程客户端 调用远程服务
@FeignClient("gulimall-product")//这个远程服务
public interface ProductFeignService {

    /**
     * @Description: 依据skuid获取商品详细信息
     */
    @RequestMapping("/product/skuinfo/info/{skuId}")
    R getSkuInfo(@PathVariable("skuId") Long skuId);

    /**
     * @Description: 依据skuid获取sku的销售属性组合（返回值是List<String>） attr_name:attr_value
     */
    @GetMapping("/product/skusaleattrvalue/stringlist/{skuId}")
    List<String> getSkuSaleAttrValues(@PathVariable("skuId") Long skuId);


    /**
     * @Description: 依据skuid获取价格
     */
    @GetMapping("/product/skuinfo/{skuId}/getPrice")
    R getPrice(@PathVariable("skuId") Long skuId);
}
