package com.xuecheng.content;

import com.xuecheng.content.config.MultipartSupportConfig;
import com.xuecheng.content.feignclient.MediaServiceClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

/**
 * @description 测试远程调用媒资服务
 */
@SpringBootTest
public class FeignUploadTest {

    @Autowired
    MediaServiceClient mediaServiceClient;

    //远程调用，上传文件
    @Test
    public void test() {

        //将file类型转成MultipartFile
        MultipartFile multipartFile = MultipartSupportConfig.getMultipartFile(new File("D:\\upload\\test.html"));
        //远程调用得到返回值
        String upload = mediaServiceClient.uploadFile(multipartFile, "course/test.html");
        //返回值 根据你在降级逻辑里面的返回值确定
        if (upload == null){
            //如果走了降级逻辑，应该怎么样的处理方式
            System.out.println("走了降级逻辑");
        }
    }

}
