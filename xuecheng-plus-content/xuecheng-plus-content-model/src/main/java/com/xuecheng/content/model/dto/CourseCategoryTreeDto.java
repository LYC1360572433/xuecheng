package com.xuecheng.content.model.dto;

import com.xuecheng.content.model.po.CourseCategory;
import lombok.Data;

import java.util.List;

//java.io.Serializable 序列化接口
@Data
public class CourseCategoryTreeDto extends CourseCategory implements java.io.Serializable{

    List<CourseCategoryTreeDto> childrenTreeNodes;//(子节点)下级节点：该属性里面就是他本身 也就是说节点里面包含子节点
}
