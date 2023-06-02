package com.xuecheng.content.feignclient;

import org.springframework.web.multipart.MultipartFile;

//第一种降级方法 无法取到异常
public class MediaServiceClientFallback implements MediaServiceClient{
    @Override
    public String uploadFile(MultipartFile upload, String objectName) {
        return null;
    }
}
