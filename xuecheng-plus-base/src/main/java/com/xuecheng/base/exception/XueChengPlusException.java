package com.xuecheng.base.exception;

/**
 * 本项目自定义异常类型
 */
public class XueChengPlusException extends RuntimeException {

    private String errMessage;

    private String errCode;

    public XueChengPlusException(String errCode, String errMessage) {
        this.errMessage = errMessage;
        this.errCode = errCode;
    }

    public XueChengPlusException(String errMessage) {
        this.errMessage = errMessage;
    }


    public XueChengPlusException() {
        super();
    }

    public String getErrCode() {
        return errCode;
    }

    public String getErrMessage() {
        return errMessage;
    }

    public static void cast(CommonError commonError) {
        throw new XueChengPlusException(commonError.getErrMessage());
    }

    public static void cast(String errMessage) {
        throw new XueChengPlusException(errMessage);
    }

    public static void cast(String errCode, String errMessage) {
        throw new XueChengPlusException(errCode, errMessage);
    }
}