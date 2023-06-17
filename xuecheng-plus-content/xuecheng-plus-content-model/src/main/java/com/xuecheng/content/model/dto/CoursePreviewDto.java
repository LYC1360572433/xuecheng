package com.xuecheng.content.model.dto;

import com.xuecheng.content.model.po.CourseTeacher;
import com.xuecheng.content.model.po.Teachplan;
import lombok.Data;
import lombok.ToString;

import java.util.List;

/**
 * @description 课程预览数据模型
 */
@Data
@ToString
public class CoursePreviewDto {

    //课程基本信息,课程营销信息
    CourseBaseInfoDto courseBase;


    //课程计划信息
    List<TeachplanDto> teachplans;

    //师资信息暂时不加
    //todo:
    CourseTeacher courseTeacher;



}