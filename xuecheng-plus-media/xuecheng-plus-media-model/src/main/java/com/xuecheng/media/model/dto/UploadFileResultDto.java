package com.xuecheng.media.model.dto;

import com.xuecheng.media.model.po.MediaFiles;
import lombok.Data;
import lombok.ToString;

/**
 * media_files 媒资文件信息 可扩展的
 * 为什么不直接用MediaFiles？
 * 因为MediaFiles对应数据库的media_files表，不可随意直接在MediaFiles实体类改动改动
 */

@Data
@ToString
public class UploadFileResultDto extends MediaFiles {
    //哪天要加数据可以在这里加
}
