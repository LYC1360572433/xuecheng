package com.xuecheng.search.feignclient;

import com.xuecheng.base.model.PageResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.*;

/**
 * @description 我的课程表服务远程接口
 */
@FeignClient(value = "learning-api", fallbackFactory = MyCourseTablesServiceClientFallbackFactory.class)
public interface MyCourseTablesServiceClient {

     @GetMapping("/learning/mycoursetable")
     PageResult<XcCourseTables> mycoursetable(@SpringQueryMap MyCourseTableParams params);

}
