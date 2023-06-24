package com.xuecheng.content.api;


import com.baomidou.mybatisplus.extension.api.R;
import com.xuecheng.base.exception.ValidationGroups;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.base.model.RestResponse;
import com.xuecheng.content.model.dto.AddCourseDto;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.EditCourseDto;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.service.CourseBaseInfoService;
import com.xuecheng.content.util.SecurityUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Api(value = "课程信息管理接口",tags = "课程信息管理接口")
@RestController//相当于@Controller和@ResponseBody的组合 响应json      @RequestBody:传过来的json参数转为java对象
public class CourseBaseInfoController {

    @Autowired
    CourseBaseInfoService courseBaseInfoService;

    @ApiOperation("课程查询接口")//swagger提供的api 方法注释
    @PreAuthorize("hasAuthority('xc_teachmanager_course_list')")//指定权限标识符，拥有此权限才可以访问此方法
    @PostMapping("/course/list")//@RequestBody(required = false) 参数可以不必填
    public PageResult<CourseBase> list(PageParams pageParams,@RequestBody(required = false) QueryCourseParamsDto queryCourseParamsDto){
        //当前登录用户
        SecurityUtil.XcUser user = SecurityUtil.getUser();
        //用户所属机构id
        Long companyId = null;
        if (StringUtils.isNotEmpty(user.getCompanyId())){
            companyId = Long.parseLong(user.getCompanyId());
        }
        PageResult<CourseBase> courseBasePageResult = courseBaseInfoService.queryCourseBaseList(companyId,pageParams, queryCourseParamsDto);

        return courseBasePageResult;
    }

    @ApiOperation("新增课程")
    @PreAuthorize("hasAuthority('xc_teachmanager_course_add')")//指定权限标识符，拥有此权限才可以访问此方法
    @PostMapping("/course")
    //@Validated 校验参数      @Validated(ValidationGroups.Insert.class) 组别类型
    public CourseBaseInfoDto createCourseBase(@RequestBody @Validated(ValidationGroups.Insert.class) AddCourseDto addCourseDto){

        //当前登录用户
        SecurityUtil.XcUser user = SecurityUtil.getUser();
        //用户所属机构id
        Long companyId = null;
        if (StringUtils.isNotEmpty(user.getCompanyId())){
            companyId = Long.parseLong(user.getCompanyId());
        }
        CourseBaseInfoDto courseBase = courseBaseInfoService.createCourseBase(companyId, addCourseDto);
        return courseBase;
    }

    //点了编辑 就进入这个
    @ApiOperation("根据课程id查询接口")
    //这里的权限有点混乱，数据库没设计好
    @PreAuthorize("hasAuthority('xc_teachmanager_course')")//指定权限标识符，拥有此权限才可以访问此方法
    @GetMapping("/course/{courseId}")
    public CourseBaseInfoDto getCourseBaseById(@PathVariable Long courseId){
        //获取当前用户的身份
        //获取上下文 获取认证信息 获取身份
        //底层使用thread local 把用户身份放在线程的变量中 线程里面的所有方法都可以拿到身份信息
//        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
//        System.out.println("--------------" + principal);

        //暂时注释，因为还没实现单点登录
//        SecurityUtil.XcUser user = SecurityUtil.getUser();
//        System.out.println(user.getUsername());

        CourseBaseInfoDto courseBaseInfo = courseBaseInfoService.getCourseBaseInfo(courseId);
        return courseBaseInfo;
    }

    @ApiOperation("修改课程")
    //这里的权限有点混乱，数据库没设计好
    @PreAuthorize("hasAuthority('xc_teachmanager_course')")//指定权限标识符，拥有此权限才可以访问此方法
    @PutMapping("/course")
    public CourseBaseInfoDto modifyCourseBase(@RequestBody @Validated(ValidationGroups.Update.class) EditCourseDto editCourseDto){
        //当前登录用户
        SecurityUtil.XcUser user = SecurityUtil.getUser();
        //用户所属机构id
        Long companyId = null;
        if (StringUtils.isNotEmpty(user.getCompanyId())){
            companyId = Long.parseLong(user.getCompanyId());
        }
        CourseBaseInfoDto courseBaseInfoDto = courseBaseInfoService.updateCourseBase(companyId, editCourseDto);
        return courseBaseInfoDto;
    }

    /**
     * 删除课程信息(包括课程相关的基本信息、营销信息、课程计划、课程教师信息)
     * @param courseId 课程id
     */
    @ApiOperation("删除课程信息接口")
    @PreAuthorize("hasAuthority('xc_teachmanager_course_del')")//指定权限标识符，拥有此权限才可以访问此方法
    @DeleteMapping ("/course/{courseId}")
    public void deleteCourseBaseInfo(@PathVariable Long courseId){
        courseBaseInfoService.deleteCourseBaseInfo(courseId);
    }
}
