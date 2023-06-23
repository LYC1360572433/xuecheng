package com.xuecheng.ucenter.service;

import com.xuecheng.base.model.RestResponse;
import com.xuecheng.ucenter.model.dto.AuthParamsDto;

public interface RegisterAuthService {

    /**
     * 注册服务
     * @param authParamsDto
     * @return
     */
    public RestResponse registerAuth(AuthParamsDto authParamsDto);
}
