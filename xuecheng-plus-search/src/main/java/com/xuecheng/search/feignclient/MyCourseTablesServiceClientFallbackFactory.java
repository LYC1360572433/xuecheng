package com.xuecheng.search.feignclient;

import com.xuecheng.base.model.PageResult;
import com.xuecheng.base.model.RestResponse;
import feign.FeignException;
import feign.hystrix.FallbackFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @description 降级类
 */
@Slf4j
@Component
public class MyCourseTablesServiceClientFallbackFactory implements FallbackFactory<MyCourseTablesServiceClient> {
    @Override
    public MyCourseTablesServiceClient create(Throwable throwable) {
        return new MyCourseTablesServiceClient() {
            @Override
            public PageResult<XcCourseTables> mycoursetable(MyCourseTableParams params) {
                FeignException ex = (FeignException) throwable;
                log.error("111111111" + ex);
                log.error("远程调用我的课程表服务熔断异常：{}", throwable.getMessage());
                return null;
            }
        };
    }
}
