package com.xuecheng.ucenter.service;

import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.dto.XcUserExt;

/**
 * @description 认证service,统一的认证接口
 */
public interface AuthService {

    /**
     * @description 认证方法
     * @param authParamsDto 认证参数
     * @return com.xuecheng.ucenter.model.po.XcUser 用户信息
     */
    //判断账号存不存在，密码对不对
    XcUserExt execute(AuthParamsDto authParamsDto);

}
