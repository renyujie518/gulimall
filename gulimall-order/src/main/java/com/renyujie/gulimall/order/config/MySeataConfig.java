package com.renyujie.gulimall.order.config;

//import io.seata.rm.datasource.DataSourceProxy;


/**
 *seata配置类 所有想要用到分布式事务的微服务使用seata DataSourceProxy代理自己的数据源
 * 本项目没有使用
 */
//@Configuration
public class MySeataConfig {

//    @Autowired
//    DataSourceProperties dataSourceProperties;
//
//    @Bean
//    public DataSource dataSource(DataSourceProperties dataSourceProperties) {
//        HikariDataSource dataSource = dataSourceProperties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
//        if (StringUtils.hasText(dataSourceProperties.getName())) {
//            dataSource.setPoolName(dataSourceProperties.getName());
//        }
//        return new DataSourceProxy(dataSource);
//    }
}
