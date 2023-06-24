package com.xuecheng.content.api;


import com.xuecheng.base.exception.ValidationGroups;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.model.dto.AddCourseDto;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.model.po.CourseTeacher;
import com.xuecheng.content.service.CourseTeacherService;
import com.xuecheng.content.util.SecurityUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 教师信息管理相关的接口
 */
@Api(value = "教师信息编辑接口",tags = "教师信息编辑接口")
@RestController
@PreAuthorize("hasAuthority('xc_teachmanager_course')")//这里应该是编辑教师信息权限 因为数据库没写全，所以。。。
public class CourseTeacherController {
    @Autowired
    CourseTeacherService courseTeacherService;

    @ApiOperation("教师查询接口")
    @GetMapping("/courseTeacher/list/{courseId}")
    /**
     * 根据课程id查教师信息
     */
    public List<CourseTeacher> getCourseTeachersByCourseId(@PathVariable Long courseId){
        List<CourseTeacher> courseTeachers = courseTeacherService.getCourseTeachersByCourseId(courseId);
        return courseTeachers;
    }

    /**
     * 新增教师信息
     * @param courseTeacher 教师信息
     * @return 教师信息
     */
    @ApiOperation("新增教师信息接口")
    @PostMapping("/courseTeacher")
    public CourseTeacher createCourseTeacher(@RequestBody @Validated(ValidationGroups.Insert.class) CourseTeacher courseTeacher){

        //当前登录用户
        SecurityUtil.XcUser user = SecurityUtil.getUser();
        //用户所属机构id
        Long companyId = null;
        if (StringUtils.isNotEmpty(user.getCompanyId())){
            companyId = Long.parseLong(user.getCompanyId());
        }
        CourseTeacher courseTeacherNew = courseTeacherService.createCourseTeacher(companyId, courseTeacher);
        return courseTeacherNew;
    }

    /**
     * 修改教师信息
     * @param courseTeacher 教师信息
     * @return 教师信息
     */
    @ApiOperation("修改教师信息接口")
    @PutMapping("/courseTeacher")
    public CourseTeacher updateCourseTeacher(@RequestBody CourseTeacher courseTeacher){
        CourseTeacher courseTeacherNew = courseTeacherService.updateCourseTeacher(courseTeacher);
        return courseTeacherNew;
    }

    /**
     * 删除教师信息
     * @param courseId 课程id
     * @param teacherId 教师id
     */
    @ApiOperation("删除教师信息接口")
    @DeleteMapping ("/courseTeacher/course/{courseId}/{teacherId}")
    public void deleteCourseTeacher(@PathVariable Long courseId,@PathVariable Long teacherId){
        courseTeacherService.deleteCourseTeacher(courseId,teacherId);
    }
}
