package com.xuecheng.media.service.jobhandler;

import com.xuecheng.base.utils.Mp4VideoUtil;
import com.xuecheng.media.mapper.MediaProcessMapper;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.service.MediaFileProcessService;
import com.xuecheng.media.service.MediaFileService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 视频任务处理类
 */
@Component
@Slf4j
public class VideoTask {

    @Autowired
    MediaFileProcessService mediaFileProcessService;

    //注入nacos里面的环境变量
    //ffmpeg的路径
    @Value("${videoprocess.ffmpegpath}")
    private String ffmpegpath;

    @Autowired
    MediaFileService mediaFileService;

    /**
     * 视频处理任务
     */
    @XxlJob("videoJobHandler")
    public void videoJobHandler() throws Exception {

        // 分片参数
        int shardIndex = XxlJobHelper.getShardIndex();//执行器的序号,从0开始
        int shardTotal = XxlJobHelper.getShardTotal();//执行器总数

        //确定cpu的核心数
        int processors = Runtime.getRuntime().availableProcessors();
        //查询每个执行器分配的待处理的任务(拿出任务)   因为处理视频非常耗cpu,所以一般你有多少核，就并行处理多少任务。
        List<MediaProcess> mediaProcessList = mediaFileProcessService.getMediaProcessList(shardIndex, shardTotal, processors);
        //任务数量
        int size = mediaProcessList.size();
        log.debug("取到视频处理任务数:" + size);
        if (size <= 0) {
            return;
        }
        //创建一个线程池  线程池里面有十二个线程(12核)足够用，因为太耗cpu，所以任务数量小于等于cpu核数，也是线程数(最多是cpu核数)
        //newFixedThreadPool该工具类 1.线程数不多 2.只要线程池在,线程都活着 3.让线程和线程池处理任务,任务处理完,线程池就销毁
        //根据任务数量创建线程数
        ExecutorService executorService = Executors.newFixedThreadPool(size);
        //多线程 使用的计数器
        CountDownLatch countDownLatch = new CountDownLatch(size);
        //遍历多个任务 每遍历一个任务 就加入线程池 就让线程执行逻辑代码
        mediaProcessList.forEach(mediaProcess -> {
            //将任务加入线程池
            executorService.execute(() -> {
                try {
                    //任务执行逻辑
                    //任务id 任务表的主键
                    Long taskId = mediaProcess.getId();
                    //开启任务
                    boolean b = mediaFileProcessService.startTask(taskId);
                    if (!b) {
                        log.debug("抢占任务失败,任务id:{}", taskId);
                        return;
                    }
                    //抢占成功，继续执行下面的代码
                    //ffmpeg的路径
                    String ffmpeg_path = ffmpegpath;//ffmpeg的安装位置
                    //文件id就是md5值
                    String fileId = mediaProcess.getFileId();
                    //桶
                    String bucket = mediaProcess.getBucket();
                    //objectName
                    String objectName = mediaProcess.getFilePath();
                    //下载minio视频到本地
                    File file = mediaFileService.downloadFileFromMinIO(bucket, objectName);
                    //看了该方法的返回值,有可能下载为null,所以要进行判断
                    if (file == null) {
                        log.debug("下载视频出错,任务id:{},bucket:{},objectName:{}", taskId, bucket, objectName);
                        //保存任务处理失败的结果
                        mediaFileProcessService.saveProcessFinishStatus(taskId, "3", fileId, null, "下载视频到本地失败");
                        return;
                    }
                    //下载视频成功
                    //源avi视频的路径
                    String video_path = file.getAbsolutePath();
                    //转换后mp4文件的名称
                    String mp4_name = fileId + ".mp4";
                    //转换后mp4文件的路径
                    //先创建一个临时文件,作用就是来放转换后的文件
                    File mp4File = null;
                    try {
                        mp4File = File.createTempFile("minio", ".mp4");
                    } catch (IOException e) {
                        //假如磁盘满了,创建临时文件的时候出现问题了,捕获异常
                        log.debug("创建临时文件异常,{}", e.getMessage());
                        mediaFileProcessService.saveProcessFinishStatus(taskId, "3", fileId, null, "创建临时文件异常");
                        return;
                    }
                    String mp4_path = mp4File.getAbsolutePath();
                    //创建工具类对象
                    Mp4VideoUtil videoUtil = new Mp4VideoUtil(ffmpeg_path, video_path, mp4_name, mp4_path);
                    //开始视频转换，成功将返回success,失败返回失败原因
                    String result = videoUtil.generateMp4();
                    if (!result.equals("success")) {
                        log.debug("视频转码失败,原因:{},bucket:{},objectName:{}", result, bucket, objectName);
                        mediaFileProcessService.saveProcessFinishStatus(taskId, "3", fileId, null, "视频转码失败" + result);
                        return;
                    }
                    //视频转码成功
                    //上传到minio
                    //这里上传到minio，不应该用objectName,因为上面的objectName还是avi为后缀
                    //应该再定义一个objectName1 以mp4为后缀
                    String objectName1 = getFilePath(fileId, ".mp4");
                    boolean b1 = mediaFileService.addMediaFilesToMinIo(mp4_path, "video/mp4", bucket, objectName1);
                    if (!b1) {
                        //上传到minio失败
                        log.debug("上传mp4到minio失败,taskId:{}", taskId);
                        mediaFileProcessService.saveProcessFinishStatus(taskId, "3", fileId, null, "上传mp4到minio失败");
                        return;
                    }
                    //上传到minio成功,将minio中avi视频删除
                    boolean b2 = mediaFileService.removeFileFromMinio(bucket, objectName);
                    if (!b2) {
                        // minio中avi视频删除失败
                        log.debug("minio中avi视频删除失败,taskId:{}", taskId);
                        mediaFileProcessService.saveProcessFinishStatus(taskId, "3", fileId, null, "minio中avi视频删除失败");
                        return;
                    }
                    MediaFiles mediaFiles = mediaFileService.getFileById(fileId);
                    mediaFiles.setFilePath(objectName1);
                    mediaFileService.updateById(mediaFiles);
                    //mp4文件的url
                    String url = "/" + bucket + "/" + getFilePath(fileId, ".mp4");
                    //更新任务的状态为成功
                    mediaFileProcessService.saveProcessFinishStatus(taskId, "2", fileId, url, "下载视频到本地成功");
                } finally {
                    //此时的try/catch作用是:只有执行try里面的方法，不管是return的，还是正常执行，都会finally减1
                    //计数器减去1  直到减为0,阻塞才放行
                    countDownLatch.countDown();
                }

            });
        });

        //阻塞 十六个线程都阻塞在这里
        //保底策略 正常情况下 计数器减到0放行，如果突发不正常的情况，最多等30分钟就放行(指定最大限度的等待时间,阻塞最多等待一定的时间后就解除阻塞)
        countDownLatch.await(30, TimeUnit.MINUTES);
    }

    //objectName与file_path是一样的，因为在minio里面就是这么命名的
    private String getFilePath(String fileMd5, String fileExt) {
        return fileMd5.substring(0, 1) + "/" + fileMd5.substring(1, 2) + "/" + fileMd5 + "/" + fileMd5 + fileExt;
    }
}