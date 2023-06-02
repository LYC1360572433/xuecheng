package com.xuecheng;

import com.spring4all.swagger.EnableSwagger2Doc;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 把启动类放到com.xuecheng下面 它就会扫描该包下面的所有东西
 * 内容管理服务启动类
 */
@EnableFeignClients(basePackages={"com.xuecheng.content.feignclient"})//扫到feignclient，才会生成代理对象
@EnableSwagger2Doc//生成swagger接口文档
@SpringBootApplication
public class ContentApplication {
    public static void main(String[] args) {
        SpringApplication.run(ContentApplication.class,args);
    }
}
