package com.xuecheng.content.service;

import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * 课程计划管理相关的接口
 */
public interface TeachplanService {
    /**
     * 根据课程id查询课程计划
     * @param courseId 课程id
     * @return 课程计划
     */
    public List<TeachplanDto> findTeachplanTree(Long courseId);


    /**
     * 新增/修改/保存课程计划
     * @param saveTeachplanDto
     */
    public void saveTeachplan(SaveTeachplanDto saveTeachplanDto);

    /**
     * 删除课程计划
     * @param teachplanId
     */
    public void delete(Long teachplanId);

    /**
     * 同级章节向下移动
     * @param teachplanId
     */
    public void movedown(Long teachplanId);

    /**
     * 同级章节向上移动
     * @param teachplanId
     */
    public void moveup(Long teachplanId);
}
