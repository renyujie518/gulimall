package com.renyujie.gulimall.seckill.feign;


import com.renyujie.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 远程商品服务
 */
//告诉spring cloud 这个接口是一个远程客户端 调用远程服务
@FeignClient("gulimall-product")//这个远程服务
public interface ProductFeignService {

    /**
     * 信息（原）
     * 远程服务gulimall-seckill用了一次
     */
    @RequestMapping("/product/skuinfo/info/{skuId}")
    R getSkuInfo(@PathVariable("skuId") Long skuId);
}
