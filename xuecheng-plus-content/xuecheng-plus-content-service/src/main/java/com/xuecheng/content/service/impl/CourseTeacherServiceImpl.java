package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.mapper.CourseTeacherMapper;
import com.xuecheng.content.model.po.CourseTeacher;
import com.xuecheng.content.service.CourseTeacherService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class CourseTeacherServiceImpl implements CourseTeacherService {

    @Autowired
    CourseTeacherMapper courseTeacherMapper;

    /**
     * 根据课程id查询教师信息
     * @param courseId 课程id
     * @return 教师信息
     */
    @Override
    public List<CourseTeacher> getCourseTeachersByCourseId(Long courseId) {
        LambdaQueryWrapper<CourseTeacher> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CourseTeacher::getCourseId,courseId);
        List<CourseTeacher> courseTeachers = courseTeacherMapper.selectList(queryWrapper);
        return courseTeachers;
    }

    /**
     * 新增教师信息
     * @param companyId 机构id
     * @param courseTeacher 教师信息
     * @return 教师信息
     */
    @Override
    public CourseTeacher createCourseTeacher(Long companyId, CourseTeacher courseTeacher) {
        CourseTeacher courseTeacherNew = new CourseTeacher();
        BeanUtils.copyProperties(courseTeacher,courseTeacherNew);
        courseTeacherNew.setCreateDate(LocalDateTime.now());
        int insert = courseTeacherMapper.insert(courseTeacherNew);
        if (insert <= 0) {
            throw new RuntimeException("添加教师信息失败");
        }
        return courseTeacherNew;
    }

    /**
     * 修改教师信息
     * @param courseTeacher 教师信息
     * @return 教师信息
     */
    @Override
    public CourseTeacher updateCourseTeacher(CourseTeacher courseTeacher) {
        int i = courseTeacherMapper.updateById(courseTeacher);
        if (i <= 0){
            XueChengPlusException.cast("修改教师信息失败");
        }
        return courseTeacher;
    }

    /**
     * 删除教师信息
     * @param courseId 课程id
     * @param teacherId 教师id
     */
    @Override
    public void deleteCourseTeacher(Long courseId, Long teacherId) {
        LambdaQueryWrapper<CourseTeacher> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CourseTeacher::getId,teacherId).eq(CourseTeacher::getCourseId,courseId);
        int i = courseTeacherMapper.delete(queryWrapper);
        if (i <= 0){
            XueChengPlusException.cast("删除教师信息失败");
        }
    }
}
