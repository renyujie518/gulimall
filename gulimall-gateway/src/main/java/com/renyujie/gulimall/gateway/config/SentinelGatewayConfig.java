//package com.renyujie.gulimall.gateway.config;
//
//import com.alibaba.csp.sentinel.adapter.spring.webflux.callback.BlockRequestHandler;
//import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.BlockRequestHandler;
//import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
//import com.alibaba.fastjson.JSON;
//import com.renyujie.common.exception.BizCodeEnum;
//import com.renyujie.common.utils.R;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.web.reactive.function.server.ServerResponse;
//import org.springframework.web.server.ServerWebExchange;
//import reactor.core.publisher.Mono;
//
///**
// * @author renyujie518
// * @version 1.0.0
// * @ClassName SentinelGatewayConfig.java
// * @description sentinel网关层 返回我们自己的东西错误代码
// *      TODO 响应式编程 - 天然支持大并发系统
// * @createTime 2022年03月08日 21:47:00
// */
//@Configuration
//public class SentinelGatewayConfig {
//    public SentinelGatewayConfig() {
//        GatewayCallbackManager.setBlockHandler(new BlockRequestHandler() {
//
//            /**
//             * 网关限流了请求，就会掉用此方法 Mono Flux
//             */
//            @Override
//            public Mono<ServerResponse> handleRequest(ServerWebExchange serverWebExchange, Throwable throwable) {
//                R error = R.error(BizCodeEnum.TO_MANY_REQUEST.getCode(), BizCodeEnum.TO_MANY_REQUEST.getMsg());
//                String s = JSON.toJSONString(error);
//                Mono<ServerResponse> body = ServerResponse.ok().body(Mono.just(s), String.class);
//                return body;
//            }
//        });
//    }
//}
