package com.xuecheng.media;

import com.baomidou.mybatisplus.extension.api.R;
import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import io.minio.*;
import io.minio.errors.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * 测试minio的sdk
 */
public class MinioTest {

    MinioClient minioClient =
            MinioClient.builder()
                    .endpoint("http://192.168.101.65:9000")
                    .credentials("minioadmin", "minioadmin")
                    .build();

    @Test
    public void test_upload() throws Exception {

        //根据扩展名得到媒体资源类型 mimeType
        //根据扩展名取出mimeType
        ContentInfo extensionMatch = ContentInfoUtil.findExtensionMatch(".jpg");
        String mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;//通用mimeType，字节流 默认是未知的mimeType
        if (extensionMatch != null) {
            mimeType = extensionMatch.getMimeType();
        }

        //上传文件的参数信息 minio里面要构造哪个类 就哪个类.builder
        UploadObjectArgs uploadObjectArgs = UploadObjectArgs.builder()
                .bucket("mediafiles")//桶
                .filename("D:\\upload\\2.jpg")//指定本地文件路径
                // 在根目录(桶)下上传 存储该文件 也就是会得到testbucket/test.txt
//              .object("test.txt")//上传到minio中,它的对象名 叫什么
                .object("test/01/2.jpg")
                .contentType(mimeType)// 用来设置媒体文件类型
                .build();

        //上传文件
        minioClient.uploadObject(uploadObjectArgs);

        //判断上传文件是否内容完整
        //首先 先将上传的文件下载下来
        //然后再将其md5比较(个人认为 多此一举。因为本身下载文件也是通过流拷贝，所以。。。)
    }

    //删除文件
    @Test
    public void test_delete() throws Exception {
        //创建类对象
        RemoveObjectArgs removeObjectArgs = RemoveObjectArgs.builder()
                .bucket("mediafiles")
                .object("/2022/09/14/a16da7a132559daf9e1193166b3e7f52.jpg")
                .build();

        minioClient.removeObject(removeObjectArgs);

        System.out.println("删除成功");
    }

    //查询文件 从minio中下载文件
    @Test
    public void test_getFile() throws Exception {
        //创建类对象
        GetObjectArgs getObjectArgs = GetObjectArgs.builder()
                .bucket("testbucket")
                .object("test/01/1.mp4")
                .build();

        //拿到输入流 查询远程服务获取到一个流对象
        FilterInputStream inputStream = minioClient.getObject(getObjectArgs);
        //指定输出流
        FileOutputStream outputStream = new FileOutputStream(new File("D:\\soft\\minio\\upload\\1a.mp4"));

        //流拷贝的方法
        IOUtils.copy(inputStream, outputStream);

        //校验文件的完整性 对文件的内容进行md5
        //不要比较从远程服务拿到的流对象 因为要进行网络传输 可能出现问题
        //String source_md5 = DigestUtils.md5Hex(inputStream);//原始文件(minio中的文件)的md5
        //直接用硬盘里面 一开始上传到minio的那个文件 来进行md5校验
        String source_md5 = DigestUtils.md5Hex(new FileInputStream(new File("D:\\soft\\minio\\upload\\1.mp4")));


        //对下载到本地的文件内容 进行md5(要先将文件转为输入流)
        String local_md5 = DigestUtils.md5Hex(new FileInputStream(new File("D:\\soft\\minio\\upload\\1a.mp4")));

        if (source_md5.equals(local_md5)) {
            System.out.println("下载成功");
        }
    }

    //将分块文件上传到minio
    @Test
    public void uploadChunk() throws Exception {

        for (int i = 0; i < 2; i++) {
            UploadObjectArgs uploadObjectArgs = UploadObjectArgs.builder()
                    .bucket("testbucket")
                    .filename("D:\\soft\\minio\\upload\\chunk\\" + i)
                    .object("chunk/" + i)
                    .build();

            //上传文件
            minioClient.uploadObject(uploadObjectArgs);
            System.out.println("上传分块" + i + "成功");
        }
    }

    //调用minio接口合并分块
    @Test
    public void testMerge()throws Exception{

        /*List<ComposeSource> sources = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            //指定分块文件的信息
            ComposeSource composeSource = ComposeSource.builder().bucket("testbucket").object("chunk/" + i).build();
            sources.add(composeSource);
        }*/

        List<ComposeSource> sources = Stream.iterate(0, i -> ++i)//起始值为0，每次生成一个i+1的数
                .limit(2)//截断流的长度
                .map(i -> ComposeSource.builder().bucket("testbucket").object("chunk/" + i).build()).collect(Collectors.toList());


        //指定合并后的objectName等信息
        ComposeObjectArgs composeObjectArgs = ComposeObjectArgs.builder()
                .bucket("testbucket")
                .object("merge01.mp4")
                .sources(sources)//指定源文件
                .build();

        //合并文件
        //报错:java.lang.IllegalArgumentException: source testbucket/chunk/0: size 1048576 must be greater than 5242880
        //原因:minio默认的分块大小为5M
        minioClient.composeObject(composeObjectArgs);
    }


    //批量清理分块文件
}
