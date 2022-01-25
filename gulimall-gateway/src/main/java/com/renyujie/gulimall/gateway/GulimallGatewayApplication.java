package com.renyujie.gulimall.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

//网关也要开启服务注册&发现
@EnableDiscoveryClient
//gateway并没有用到数据库相关 但是由于要使用nacos引用了common模块，这里要排除数据源的自动配置，否则会报错"数据库没有放在类路径下"
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class GulimallGatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(GulimallGatewayApplication.class, args);
	}

}
