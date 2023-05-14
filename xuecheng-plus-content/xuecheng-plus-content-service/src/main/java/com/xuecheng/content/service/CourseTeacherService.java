package com.xuecheng.content.service;

import com.xuecheng.content.model.po.CourseTeacher;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;


import java.util.List;

public interface CourseTeacherService {

    /**
     * 根据课程id查询教师信息
     * @param courseId 课程id
     * @return 教师信息
     */
    public List<CourseTeacher> getCourseTeachersByCourseId(Long courseId);

    /**
     * 新增教师信息
     * @param companyId 机构id
     * @param courseTeacher 教师信息
     * @return 教师信息
     */
    public CourseTeacher createCourseTeacher(Long companyId,CourseTeacher courseTeacher);

    /**
     * 修改教师信息
     * @param courseTeacher 教师信息
     * @return 教师信息
     */
    public CourseTeacher updateCourseTeacher(CourseTeacher courseTeacher);

    /**
     * 删除教师信息
     * @param courseId 课程id
     * @param teacherId 教师id
     */
    public void deleteCourseTeacher(Long courseId,Long teacherId);
}
