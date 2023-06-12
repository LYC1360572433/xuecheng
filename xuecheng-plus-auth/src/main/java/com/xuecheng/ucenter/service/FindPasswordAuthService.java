package com.xuecheng.ucenter.service;

import com.xuecheng.base.model.RestResponse;
import com.xuecheng.ucenter.model.dto.AuthParamsDto;

public interface FindPasswordAuthService {

    /**
     * 找回密码服务
     * @param authParamsDto
     * @return
     */
    public void findPasswordAuth(AuthParamsDto authParamsDto);
}
