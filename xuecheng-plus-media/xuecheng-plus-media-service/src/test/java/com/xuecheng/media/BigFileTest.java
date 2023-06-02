package com.xuecheng.media;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.*;

/**
 * 测试大文件上传方法
 */
public class BigFileTest {

    //分块测试
    @Test
    public void testChunk() throws IOException {
        //源文件
        File sourceFile = new File("D:\\soft\\minio\\upload\\test.mp4");
        //分块文件存储路径
        String chunkFilePath = "D:\\soft\\minio\\upload\\chunk\\";
        //分块文件大小
        int chunkSize = 1024 * 1024 * 5;//5M
        //分块文件个数
        //源文件大小/分块文件大小
        //因为有可能是小数 因此先 * 1.0相当于转为double,然后得出的结果向上取整(结果大于或等于该数值的最小整数)
        int chunkNum = (int) Math.ceil(sourceFile.length() * 1.0 / chunkSize);

        //使用流从源文件读数据，向分块文件中写数据
        //随机流：RandomAccessFile，具有读和写的功能
        //参数一:文件 参数二:指定读写规则
        RandomAccessFile raf_r = new RandomAccessFile(sourceFile, "r");
        //缓冲区
        byte[] bytes = new byte[1024];

        for (int i = 0; i < chunkNum; i++) {
            File chunkFile = new File(chunkFilePath + i);
            //分块文件写入流
            RandomAccessFile raf_rw = new RandomAccessFile(chunkFile, "rw");
            int len = -1;
            while ((len = raf_r.read(bytes)) != -1){
                raf_rw.write(bytes,0,len);
                if (chunkFile.length() >= chunkSize){
                    break;
                }
            }
            raf_rw.close();
        }
        raf_r.close();
    }

    //将分块进行合并
    @Test
    public void testMerge() throws IOException {

        //源文件
        File sourceFile = new File("D:\\soft\\minio\\upload\\test.mp4");
        //分块文件存储路径
        File chunkFilePath = new File("D:\\soft\\minio\\upload\\chunk\\");
        //合并后的文件
        File mergeFile = new File("D:\\soft\\minio\\upload\\mergeTest.mp4");
        //取出所有分块文件   可能是无序的 数据是有顺序的
        File[] files = chunkFilePath.listFiles();

        //排序
        //先将数组转为list
        List<File> filesList = Arrays.asList(files);
        //对分块文件进行排序
        Collections.sort(filesList, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                //返回值大于或等于0，不做出改变;返回值小于0,做出改变
                return Integer.parseInt(o1.getName()) - Integer.parseInt(o2.getName());
            }
        });
        //向合并文件写的流
        RandomAccessFile raf_rw = new RandomAccessFile(mergeFile, "rw");
        //缓存区
        byte[] bytes = new byte[1024];
        //遍历分块文件，向合并的文件写入
        for (File file: filesList) {
            RandomAccessFile raf_r = new RandomAccessFile(file, "r");
            int len = -1;
            while ((len = raf_r.read(bytes)) != -1){
                raf_rw.write(bytes,0,len);
            }
            raf_r.close();
        }
        raf_rw.close();
        //合并文件完成后对合并的文件校验
        FileInputStream fileInputStream_merge = new FileInputStream(mergeFile);
        FileInputStream fileInputStream_source = new FileInputStream(sourceFile);
        String md5_merge = DigestUtils.md5Hex(fileInputStream_merge);
        String md5_source = DigestUtils.md5Hex(fileInputStream_source);
        if (md5_merge.equals(md5_source)){
            System.out.println("文件合并成功");
        }
    }
}
