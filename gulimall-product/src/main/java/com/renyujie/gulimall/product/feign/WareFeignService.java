package com.renyujie.gulimall.product.feign;

import com.renyujie.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;



//告诉spring cloud 这个接口是一个远程客户端 调用远程服务
@FeignClient("gulimall-ware")//这个远程服务
public interface WareFeignService {

    @PostMapping("/ware/waresku/hasstock")
    R getSkuHasStock(@RequestBody List<Long> skuIds);
}
