package com.xuecheng.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.api.R;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.base.model.RestResponse;
import com.xuecheng.media.mapper.MediaFilesMapper;
import com.xuecheng.media.mapper.MediaProcessMapper;
import com.xuecheng.media.model.dto.QueryMediaParamsDto;
import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.model.dto.UploadFileResultDto;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.service.MediaFileService;
import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import io.minio.messages.DeletedObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
//folder:分块  iterate:迭代  concat:拼接，连接

/**
 *
 */

@Slf4j
@Service
public class MediaFileServiceImpl extends ServiceImpl<MediaFilesMapper, MediaFiles> implements MediaFileService {

    @Autowired
    MediaFilesMapper mediaFilesMapper;

    @Autowired
    MinioClient minioClient;

    @Autowired
    MediaProcessMapper mediaProcessMapper;

    //判断该方法是否可以事务控制必须保证是通过代理对象调用此方法，且此方法上添加了@Transactional注解
    //解决非事务方法调用事务方法 出现的事务回滚失效的问题
    //下面的注入 作用是 把自己注入给自己 本来自己是原始类调用方法 现在是代理对象调用方法 就可以实现缩短事务控制的代码块
    //事务控制成功的两个因素:1.代理对象调用方法(注入自己后，自己就是代理对象) 2.在此方法上添加了@Transactional注解
    @Autowired
    MediaFileService currentProxy;

    //存储视频
    @Value("${minio.bucket.videofiles}")
    private String bucket_video;

    //存储普通文件
    @Value("${minio.bucket.files}")
    private String bucket_mediafiles;

    @Override
    public MediaFiles getFileById(String mediaId) {
        MediaFiles mediaFiles = mediaFilesMapper.selectById(mediaId);
        return mediaFiles;
    }

    @Override
    public PageResult<MediaFiles> queryMediaFiels(Long companyId, PageParams pageParams, QueryMediaParamsDto queryMediaParamsDto) {
        //构建查询条件对象
        LambdaQueryWrapper<MediaFiles> queryWrapper = new LambdaQueryWrapper<>();
        if (!"".equals(queryMediaParamsDto.getFilename())) {
            queryWrapper.like(MediaFiles::getFilename, queryMediaParamsDto.getFilename());
        }
        if (!"".equals(queryMediaParamsDto.getFileType())) {
            queryWrapper.eq(MediaFiles::getFileType, queryMediaParamsDto.getFileType());
        }
        //分页对象
        Page<MediaFiles> page = new Page<>(pageParams.getPageNo(), pageParams.getPageSize());
        // 查询数据内容获得结果
        Page<MediaFiles> pageResult = mediaFilesMapper.selectPage(page, queryWrapper);
        // 获取数据列表
        List<MediaFiles> list = pageResult.getRecords();
        // 获取数据总数
        long total = pageResult.getTotal();
        // 构建结果集
        PageResult<MediaFiles> mediaListResult = new PageResult<>(list, total, pageParams.getPageNo(), pageParams.getPageSize());
        return mediaListResult;

    }

    //根据扩展名获取mimeType  抽取成一个方法
    private String getMimeType(String extension) {
        //因为是调用ContentInfoUtil.findExtensionMatch(extension)该方法去获取的，如果extension为空，会报空指针
        if (extension == null) {
            extension = "";
        }
        //根据扩展名得到媒体资源类型 mimeType
        //根据扩展名取出mimeType
        ContentInfo extensionMatch = ContentInfoUtil.findExtensionMatch(extension);
        String mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;//通用mimeType，字节流 默认是未知的mimeType
        if (extensionMatch != null) {
            mimeType = extensionMatch.getMimeType();
        }
        //如果为空的话，返回未知类型
        return mimeType;
    }

    /**
     * 将文件上传到minio
     *
     * @param localFilePath 文件本地路径
     * @param mimeType      媒体类型
     * @param bucket        桶
     * @param objectName    对象名
     * @return 布尔值
     */
    public boolean addMediaFilesToMinIo(String localFilePath, String mimeType, String bucket, String objectName) {
        //如果minio里面已经有该图片文件，还是会往minio里面传 因为objectName
        try {
            //上传文件的参数信息 minio里面要构造哪个类 就哪个类.builder
            UploadObjectArgs uploadObjectArgs = UploadObjectArgs.builder()
                    .bucket(bucket)//桶
                    .filename(localFilePath)//指定本地文件路径
                    // 在根目录(桶)下上传 存储该文件 也就是会得到testbucket/test.txt
                    // .object("test.txt")//上传到minio中,它的对象名 叫什么
                    .object(objectName)
                    .contentType(mimeType)// 用来设置媒体文件类型 重点注意，
                    // 用application/octet-stream在浏览器打开文件会下载，而不是打开
                    //这样才可以minio播放视频
//                    .contentType("video/mp4")
                    .build();
            //上传文件
            minioClient.uploadObject(uploadObjectArgs);
            log.debug("上传文件到minio成功,bucket:{},objectName:{}", bucket, objectName);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("上传文件出错,bucket:{},objectName:{},错误信息:{}", bucket, objectName, e.getMessage());
        }
        return false;
    }

    @Override
    public boolean removeFileFromMinio(String bucket, String filePath) {
        RemoveObjectArgs removeObjectArgs = RemoveObjectArgs.builder()
                .bucket(bucket)
                .object(filePath)
                .build();

        try {
            minioClient.removeObject(removeObjectArgs);
            log.debug("删除minio文件成功,bucket:{},filePath:{}", bucket, filePath);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("删除minio文件出错,bucket:{},filePath:{},错误信息:{}", bucket, filePath, e.getMessage());
            return false;
        }
    }

    //获取文件默认存储目录路径 年/月/日
    private String getDefaultFolderPath() {
        //定义格式
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        //将得到的时间格式化
        String folder = sdf.format(new Date()).replace("-", "/") + "/";
        //假如你得到一个日期 2023-05-16 格式化:2023/05/16/
        return folder;
    }

    //获取文件的md5
    private String getFileMd5(File file) {
        //文件变成输入流
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            //输入流变成md5
            String fileMd5 = DigestUtils.md5Hex(fileInputStream);
            return fileMd5;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    //该方法是专门上传文件的通用方法
//    @Transactional//不仅要上传，还要保存数据库，所以要受事务控制
    //因为里面有涉及网络传输 有可能会占用数据库资源很长时间 所以不在这加入事务控制
    @Override
    public UploadFileResultDto uploadFile(Long companyId, UploadFileParamsDto uploadFileParamsDto, String localFilePath, String objectName) {

        //文件名
        String filename = uploadFileParamsDto.getFilename();
        //先得到扩展名
        String extension = filename.substring(filename.lastIndexOf("."));

        //得到mimeType
        String mimeType = getMimeType(extension);

        //生成minio里面的文件名
        //首先，先得到minio里面的文件对象路径 也就是子目录(约定好用年月日来表示)
        String defaultFolderPath = getDefaultFolderPath();

        //根据本地路径生成文件，再根据文件，得到md5
        //其次,再得到上传文件的md5值
        String fileMd5 = getFileMd5(new File(localFilePath));

        //先判断有没有objectName
        if (StringUtils.isEmpty(objectName)) {
            //如果没有，使用默认年月日去存储
            //最后，拼接起来，形成文件名
            objectName = defaultFolderPath + fileMd5 + extension;
        }
        //上传文件到MinIo
        boolean result = addMediaFilesToMinIo(localFilePath, mimeType, bucket_mediafiles, objectName);
        if (!result) {
            XueChengPlusException.cast("上传文件失败");
        }
        //入库文件信息
        MediaFiles mediaFiles = currentProxy.addMediaFilesToDb(companyId, fileMd5, uploadFileParamsDto, bucket_mediafiles, objectName);
        if (mediaFiles == null) {
            XueChengPlusException.cast("文件上传后保存信息失败");
        }
        //准备返回的对象
        UploadFileResultDto uploadFileResultDto = new UploadFileResultDto();
        BeanUtils.copyProperties(mediaFiles, uploadFileResultDto);
        return uploadFileResultDto;
    }


    @Transactional
    //封装入库的代码
    public MediaFiles addMediaFilesToDb(Long companyId, String fileMd5, UploadFileParamsDto uploadFileParamsDto, String bucket, String objectName) {
        //将文件信息保存到数据库
        //先查数据库里面有没有该文件对象
        //有的话，直接返回，没有的话，再创建
        MediaFiles mediaFiles = mediaFilesMapper.selectById(fileMd5);
        //不会重复写入同一张图片
        if (mediaFiles == null) {
            mediaFiles = new MediaFiles();
            BeanUtils.copyProperties(uploadFileParamsDto, mediaFiles);
            //没有的字段需要自己设置
            //文件id
            mediaFiles.setId(fileMd5);
            //机构id
            mediaFiles.setCompanyId(companyId);
            //桶
            mediaFiles.setBucket(bucket);
            //file_path  minio里面的文件对象名
            mediaFiles.setFilePath(objectName);
            //file_id
            mediaFiles.setFileId(fileMd5);
            //url 访问路径
            mediaFiles.setUrl("/" + bucket + "/" + objectName);
            //上传时间
            mediaFiles.setCreateDate(LocalDateTime.now());
            //状态
            mediaFiles.setStatus("1");
            //审核状态
            mediaFiles.setAuditStatus("002003");
            //todo：审核意见
            //插入数据库
            int insert = mediaFilesMapper.insert(mediaFiles);
            if (insert <= 0) {
                log.debug("向数据库保存文件失败,bucket:{},objectName:{}", bucket, objectName);
                return null;
            }
            //记录待处理任务 只要上面的插入数据库操作成功，下面的也会成功 形成一个事务
            addWaitingTask(mediaFiles);
            log.debug("保存文件信息到数据库成功,{}", mediaFiles.toString());
            return mediaFiles;
        }
        return mediaFiles;
    }

    /**
     * 添加待处理任务
     *
     * @param mediaFiles 媒资文件信息
     */
    private void addWaitingTask(MediaFiles mediaFiles) {
        //文件名称
        String filename = mediaFiles.getFilename();
        //文件扩展名
        String exension = filename.substring(filename.lastIndexOf("."));
        //文件mimeType
        String mimeType = getMimeType(exension);
        //如果是avi视频添加到视频待处理表
        if (mimeType.equals("video/x-msvideo")) {
            MediaProcess mediaProcess = new MediaProcess();
            BeanUtils.copyProperties(mediaFiles, mediaProcess);
            mediaProcess.setStatus("1");//未处理
            mediaProcess.setFailCount(0);//失败次数默认为0
            mediaProcess.setUrl(null);
            mediaProcessMapper.insert(mediaProcess);
        }
    }

    /**
     * @param fileMd5 文件的md5
     * @return com.xuecheng.base.model.RestResponse<java.lang.Boolean> false不存在，true存在
     * @description 检查文件是否存在，数据库是否有记录，minio是否存在
     */
    @Override
    public RestResponse<Boolean> checkFile(String fileMd5) {
        //先查询数据库
        MediaFiles mediaFiles = mediaFilesMapper.selectById(fileMd5);
        if (mediaFiles != null) {
            //拿到文件的桶
            String bucket = mediaFiles.getBucket();
            //拿到文件的对象名
            String filePath = mediaFiles.getFilePath();
            //如果数据库存在，再查minio
            //创建类对象
            GetObjectArgs getObjectArgs = GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(filePath)
                    .build();

            //拿到输入流 查询远程服务获取到一个流对象
            try {
                FilterInputStream inputStream = minioClient.getObject(getObjectArgs);
                if (inputStream != null) {
                    return RestResponse.success(true);//返回true，前端读到true的话就知道文件存在了
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return RestResponse.success(false);//返回false，前端读到false的话就知道文件不存在了
        }
        return RestResponse.success(false);//返回false，前端读到false的话就知道文件不存在了
    }

    //检查minio里面的分块文件存不存在
    @Override
    public RestResponse<Boolean> checkChunk(String fileMd5, int chunkIndex) {

        //分块存储路径是:md5前两位为两个子目录，chunk为存储分块文件的目录
        //根据md5得到在minio中分块文件所在目录的路径
        String chunkFileFolderPath = getChunkFileFolderPath(fileMd5);

        //创建类对象
        GetObjectArgs getObjectArgs = GetObjectArgs.builder()
                .bucket(bucket_video)
                .object(chunkFileFolderPath + chunkIndex)
                .build();

        //拿到输入流 查询远程服务获取到一个流对象
        try {
            FilterInputStream inputStream = minioClient.getObject(getObjectArgs);
            if (inputStream != null) {
                return RestResponse.success(true);//返回true，前端读到true的话就知道文件存在了
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return RestResponse.success(false);//返回false，前端读到false的话就知道文件不存在了
    }

    //上传分块文件
    @Override
    public RestResponse uploadChunk(String fileMd5, int chunk, String localChunkFilePath) {
        //获取分块文件的路径
        String chunkFilePath = getChunkFileFolderPath(fileMd5) + chunk;
        //获取mimeType
        //分块文件没有扩展名，但是也需要定义mimeType对象
//        String mimeType = getMimeType(null);
        String mimeType = "video/mp4";
        //将分块文件上传到minio
        boolean b = addMediaFilesToMinIo(localChunkFilePath, mimeType, bucket_video, chunkFilePath);
        if (!b) {
            return RestResponse.validfail(false, "上传分块文件失败");
        }
        //上传成功
        return RestResponse.success(true);

    }

    @Override
    public RestResponse mergechunks(Long companyId, String fileMd5, int chunkTotal, UploadFileParamsDto uploadFileParamsDto) {
        //分块文件所在目录
        String chunkFileFolderPath = getChunkFileFolderPath(fileMd5);
        //源文件的名称
        String filename = uploadFileParamsDto.getFilename();
        //扩展名
        String extension = filename.substring(filename.lastIndexOf("."));
        //合并后文件的objectName
        String objectName = getFilePathByMd5(fileMd5, extension);
        //找到所有的分块文件调用minio的sdk进行文件合并
        //Stream.iterate无限流
        List<ComposeSource> sources = Stream.iterate(0, i -> ++i)//起始值为0，每次生成一个i+1的数
                .limit(chunkTotal)//截断流的长度
                .map(i -> ComposeSource.builder()
                        .bucket(bucket_video)
                        .object(chunkFileFolderPath + i)
                        .build()).collect(Collectors.toList());

        // 设置请求头信息（使合并的文件打开能直接播放视频）
        HashMap<String, String> map = new HashMap<>();
        map.put("Content-Type","video/mp4");
        //指定合并后的objectName等信息
        ComposeObjectArgs composeObjectArgs = ComposeObjectArgs.builder()
                .bucket(bucket_video)
                .object(objectName)//合并后的文件的objectName
                .sources(sources)//指定源文件
                .headers(map)
                .build();
        try {
            minioClient.composeObject(composeObjectArgs);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("合并文件出错,bucket:{},objectName:{},错误信息:{}", bucket_video, objectName, e.getMessage());
            return RestResponse.validfail(false, "合并文件异常");
        }

        //校验合并后的文件和源文件是否一致,视频上传才成功
        //先下载合并后的文件
        File file = downloadFileFromMinIO(bucket_video, objectName);
        //将流放入try括号里面，流使用完，会自动关闭，不用再搞一个final
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            //计算合并后文件的md5
            String mergeFile_md5 = DigestUtils.md5Hex(fileInputStream);
            //比较原始md5和合并后文件的md5
            if (!fileMd5.equals(mergeFile_md5)) {
                log.error("校验合并文件md5值不一致,原始文件:{},合并文件:{}", fileMd5, mergeFile_md5);
                return RestResponse.validfail(false, "文件校验失败");
            }
            //文件大小
            uploadFileParamsDto.setFileSize(file.length());
        } catch (Exception e) {
            return RestResponse.validfail(false, "文件校验失败");
        }
        //合并文件成功
        //将文件信息入库(非事务方法调用事务方法,要用代理对象去调，要不然事务控制失效)
        MediaFiles mediaFiles = currentProxy.addMediaFilesToDb(companyId, fileMd5, uploadFileParamsDto, bucket_video, objectName);
        if (mediaFiles == null) {
            return RestResponse.validfail(false, "文件入库失败");
        }
        //清理分块文件
        clearChunkFiles(chunkFileFolderPath, chunkTotal);

        return RestResponse.success(true);
    }

    /**
     * 清除分块文件
     *
     * @param chunkFileFolderPath 分块文件路径
     * @param chunkTotal          分块文件总数
     */
    private void clearChunkFiles(String chunkFileFolderPath, int chunkTotal) {

        try {
            Iterable<DeleteObject> deleteObjects = Stream.iterate(0, i -> ++i)
                    .limit(chunkTotal)
                    .map(i -> new DeleteObject(chunkFileFolderPath + i))
                    .collect(Collectors.toList());

            RemoveObjectsArgs removeObjectsArgs = RemoveObjectsArgs.builder().bucket(bucket_video).objects(deleteObjects).build();
            Iterable<Result<DeleteError>> results = minioClient.removeObjects(removeObjectsArgs);
            //需要遍历结果才能成功
            results.forEach(r -> {
                DeleteError deleteError = null;
                try {
                    deleteError = r.get();
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("清楚分块文件失败,objectname:{}", deleteError.objectName(), e);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            log.error("清楚分块文件失败,chunkFileFolderPath:{}", chunkFileFolderPath, e);
        }
    }

    //得到分块文件的目录
    private String getChunkFileFolderPath(String fileMd5) {
        //substring该方法第一个参数：从哪开始截 第二个参数：截几个
        return fileMd5.substring(0, 1) + "/" + fileMd5.substring(1, 2) + "/" + fileMd5 + "/" + "chunk" + "/";
    }

    /**
     * 得到合并后的文件的地址
     *
     * @param fileMd5 文件id即md5值
     * @param fileExt 文件扩展名
     * @return
     */
    private String getFilePathByMd5(String fileMd5, String fileExt) {
        return fileMd5.substring(0, 1) + "/" + fileMd5.substring(1, 2) + "/" + fileMd5 + "/" + fileMd5 + fileExt;
    }

    /**
     * 从minio下载文件
     *
     * @param bucket     桶
     * @param objectName 对象名称
     * @return 下载后的文件
     */
    public File downloadFileFromMinIO(String bucket, String objectName) {
        //临时文件
        File minioFile = null;
        FileOutputStream outputStream = null;
        try {
            InputStream stream = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .build());
            //创建临时文件
            minioFile = File.createTempFile("minio", ".merge");
            outputStream = new FileOutputStream(minioFile);
            IOUtils.copy(stream, outputStream);
            return minioFile;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
}