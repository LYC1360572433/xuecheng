package com.xuecheng.media.api;

import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.media.model.dto.QueryMediaParamsDto;
import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.model.dto.UploadFileResultDto;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.service.MediaFileService;
import com.xuecheng.media.util.SecurityUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

/**
 * @description 媒资文件管理接口
 */
@Api(value = "媒资文件管理接口", tags = "媒资文件管理接口")
@RestController
public class MediaFilesController {

    @Autowired
    MediaFileService mediaFileService;

    @ApiOperation("媒资列表查询接口")
    @PreAuthorize("hasAuthority('xc_teachmanager_course_list')")//指定权限标识符，拥有此权限才可以访问此方法
    @PostMapping("/files")
    public PageResult<MediaFiles> list(PageParams pageParams, @RequestBody QueryMediaParamsDto queryMediaParamsDto) {
        //当前登录用户
        SecurityUtil.XcUser user = SecurityUtil.getUser();
        //用户所属机构id
        Long companyId = null;
        if (StringUtils.isNotEmpty(user.getCompanyId())){
            companyId = Long.parseLong(user.getCompanyId());
        }
        return mediaFileService.queryMediaFiels(companyId, pageParams, queryMediaParamsDto);
    }

    @ApiOperation("上传图片")
    //consumes = MediaType.MULTIPART_FORM_DATA_VALUE  拆分
    //consumes:限制方法处理指定Content-Type的http请求 前端传过来东西，后端就属于消费者
    //MediaType.MULTIPART_FORM_DATA_VALUE: 指定mediaType的一个类型
    //请求内容Content-Type: (复杂类型)multipart/form-data:需要在表单中进行文件上传时，就需要使用该格式
    @RequestMapping(value = "/upload/coursefile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    //@RequestPart:指定前端传过来的文件名称
    //用MultipartFile类型去接文件 MultipartFile是自带的类型 里面有前端传过来的一些文件数据
    //如果有objectName，就按这个去传；如果没有，就传到年月日
    public UploadFileResultDto upload(@RequestPart("filedata") MultipartFile filedata,
                                      @RequestParam(value = "objectName", required = false) String objectName) throws IOException {

        //当前登录用户
        SecurityUtil.XcUser user = SecurityUtil.getUser();
        //用户所属机构id
        Long companyId = null;
        if (StringUtils.isNotEmpty(user.getCompanyId())){
            companyId = Long.parseLong(user.getCompanyId());
        }
        //到这里已经从前端拿到文件了，存储在本地内存里面
        //准备上传文件的信息
        UploadFileParamsDto uploadFileParamsDto = new UploadFileParamsDto();
        //原始文件名称
        uploadFileParamsDto.setFilename(filedata.getOriginalFilename());
        //原始文件大小
        uploadFileParamsDto.setFileSize(filedata.getSize());
        //原始文件类型 因为现在上传的是图片 所以文件类型写死 001001
        uploadFileParamsDto.setFileType("001001");
        //创建一个临时文件
        File tempFile = File.createTempFile("minio", ".temp");
        //拷贝到临时文件 这样服务端就有了该文件
        filedata.transferTo(tempFile);
        //取出该文件的绝对路径
        String absolutePath = tempFile.getAbsolutePath();
        //调用service上传图片
        UploadFileResultDto uploadFileResultDto = mediaFileService.uploadFile(companyId, uploadFileParamsDto, absolutePath, objectName);
        return uploadFileResultDto;
    }
}
