package com.xuecheng.ucenter.feignclient;

import com.xuecheng.ucenter.model.dto.CheckCodeParamsDto;
import com.xuecheng.ucenter.model.dto.CheckCodeResultDto;
import lombok.Data;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @description 远程调用验证码服务
 */
@FeignClient(value = "checkcode", fallbackFactory = CheckCodeClientFactory.class)
@RequestMapping("/checkcode")
public interface CheckCodeClient {

    @PostMapping(value = "/verify")
    public Boolean verify(@RequestParam("key") String key, @RequestParam("code") String code);

    @PostMapping(value = "/generate")
    CheckCodeResultDto generate(@RequestParam("checkCodeParamsDto") CheckCodeParamsDto checkCodeParamsDto);

//    @PostMapping(value = "/generate")
//    public GenerateResult generate(CheckCodeParamsDto checkCodeParamsDto, Integer code_length, String keyPrefix, Integer expire);
//
//    @Data
//    class GenerateResult {
//        String key;
//        String code;
//        String cellPhoneOrEmail;
//    }
}