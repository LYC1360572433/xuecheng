package com.xuecheng;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients(basePackages={"com.xuecheng.*.feignclient"})
// DataSourceAutoConfiguration.class会自动查找application.yml或者properties文件里的spring.datasource.*相关属性并自动配置单数据源
// 该注解的作用是，排除自动注入数据源的配置（取消数据库配置）
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class SearchApplication {

    public static void main(String[] args) {
        SpringApplication.run(SearchApplication.class, args);
    }

}
