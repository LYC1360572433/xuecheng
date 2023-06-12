package com.xuecheng.ucenter.service;

import com.xuecheng.ucenter.model.dto.AuthParamsDto;

public interface RegisterAuthService {

    /**
     * 注册服务
     * @param authParamsDto
     * @return
     */
    public void registerAuth(AuthParamsDto authParamsDto);
}
