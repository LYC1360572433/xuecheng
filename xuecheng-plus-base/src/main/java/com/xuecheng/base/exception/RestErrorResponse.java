package com.xuecheng.base.exception;

import java.io.Serializable;

/**
 * 和前端约定返回的异常信息模型
 */

public class RestErrorResponse implements Serializable {
    //将来转为json 名字为errMessage
    private String errMessage;
    private String errCode;

    public RestErrorResponse(String errCode,String errMessage){
        this.errMessage= errMessage;
        this.errCode = errCode;
    }

    public RestErrorResponse(String errMessage){
        this.errMessage = errMessage;
    }

    public String getErrCode() {
        return errCode;
    }

    public void setErrCode(String errCode) {
        this.errCode = errCode;
    }

    public String getErrMessage() {
        return errMessage;
    }

    public void setErrMessage(String errMessage) {
        this.errMessage = errMessage;
    }
}