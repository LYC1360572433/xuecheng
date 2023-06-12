package com.xuecheng.checkcode.service.impl;

import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.checkcode.model.CheckCodeParamsDto;
import com.xuecheng.checkcode.model.CheckCodeResultDto;
import com.xuecheng.checkcode.service.AbstractCheckCodeService;
import com.xuecheng.checkcode.service.CheckCodeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 根据手机号或邮件地址生成验证码
 */
@Slf4j
@Service("CheckCodeByCellPhoneService")//该服务的名字
public class CheckCodeByCellPhoneServiceImpl extends AbstractCheckCodeService implements CheckCodeService {


    @Resource(name="NumberLetterCheckCodeGenerator")
    @Override
    public void setCheckCodeGenerator(CheckCodeGenerator checkCodeGenerator) {
        this.checkCodeGenerator = checkCodeGenerator;
    }

    @Resource(name="UUIDKeyGenerator")
    @Override
    public void setKeyGenerator(KeyGenerator keyGenerator) {
        this.keyGenerator = keyGenerator;
    }

    @Resource(name="RedisCheckCodeStore")
    @Override
    public void setCheckCodeStore(CheckCodeStore checkCodeStore) {
        this.checkCodeStore = checkCodeStore;
    }

    @Override
    public CheckCodeResultDto generate(CheckCodeParamsDto checkCodeParamsDto) {

        String param1 = checkCodeParamsDto.getParam1();//获取手机号或者邮箱地址
        //不用判断空，因为前端已经判断了
//        if (param1 == null){
//            XueChengPlusException.cast("没有获取到手机号或者邮箱地址");
//        }
        GenerateResult generate = generate(checkCodeParamsDto, 4, "checkcode:", 300);
        String key = generate.getKey();
        CheckCodeResultDto checkCodeResultDto = new CheckCodeResultDto();
        checkCodeResultDto.setKey(key);
        checkCodeResultDto.setCellPhoneOrEmail(param1);
        return checkCodeResultDto;

    }

}
