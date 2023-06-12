package com.xuecheng.ucenter.feignclient;

import com.xuecheng.ucenter.model.dto.CheckCodeParamsDto;
import com.xuecheng.ucenter.model.dto.CheckCodeResultDto;
import feign.hystrix.FallbackFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 降级策略
 */
@Slf4j
@Component
public class CheckCodeClientFactory implements FallbackFactory<CheckCodeClient> {
    @Override
    public CheckCodeClient create(Throwable throwable) {
        return new CheckCodeClient() {

            @Override
            public Boolean verify(String key, String code) {
                log.debug("调用验证码服务熔断异常:{}", throwable.getMessage());
                return null;
            }

            @Override
            public CheckCodeResultDto generate(CheckCodeParamsDto checkCodeParamsDto) {
                log.debug("调用验证码服务熔断异常:{}", throwable.getMessage());
                return null;
            }

//            @Override
//            public GenerateResult generate(CheckCodeParamsDto checkCodeParamsDto, Integer code_length, String keyPrefix, Integer expire) {
//                return null;
//            }
        };
    }
}
