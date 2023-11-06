package com.xuecheng.media.api;

import com.alibaba.cloud.commons.lang.StringUtils;
import com.baomidou.mybatisplus.extension.api.R;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.RestResponse;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.service.MediaFileService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Api(value = "媒资文件管理接口",tags = "媒资文件管理接口")
@RestController
@RequestMapping("/open")
public class MediaOpenController {

    @Autowired
    MediaFileService mediaFileService;

    @ApiOperation(value = "预览文件")
    @GetMapping("/preview/{mediaId}")
    public RestResponse<String> getPlayUrlByMediaId(@PathVariable String mediaId){

        //查询媒资文件信息
        MediaFiles mediaFiles = mediaFileService.getFileById(mediaId);
        if(mediaFiles == null){
            return RestResponse.validfail("找不到视频");
        }
        //取出视频播放地址
        String url = mediaFiles.getUrl();
        if (StringUtils.isEmpty(url)){
            return RestResponse.validfail("该视频正在处理中");
        }
        return RestResponse.success(url);
    }

    @ApiOperation(value = "删除媒资信息")
    @DeleteMapping("/{mediaId}")
    public RestResponse<String> deleteMediaByMediaId(@PathVariable String mediaId){

        //查询媒资文件信息
        MediaFiles mediaFiles = mediaFileService.getFileById(mediaId);
        String bucket = mediaFiles.getBucket();
        String filePath = mediaFiles.getFilePath();
        if(mediaFiles == null){
            return RestResponse.validfail("找不到媒资信息");
        }
        //删除信息
        mediaFileService.removeById(mediaId);
        //删除minio里面对应的文件
        mediaFileService.removeFileFromMinio(bucket,filePath);
        return RestResponse.success();
    }
}