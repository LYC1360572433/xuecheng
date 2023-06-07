package com.xuecheng.content.api;


import com.baomidou.mybatisplus.extension.api.R;
import com.xuecheng.base.exception.ValidationGroups;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.model.dto.AddCourseDto;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.EditCourseDto;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.service.CourseBaseInfoService;
import com.xuecheng.content.util.SecurityUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Api(value = "课程信息管理接口",tags = "课程信息管理接口")
@RestController//相当于@Controller和@ResponseBody的组合 响应json      @RequestBody:传过来的json参数转为java对象
public class CourseBaseInfoController {

    @Autowired
    CourseBaseInfoService courseBaseInfoService;

    @ApiOperation("课程查询接口")//swagger提供的api 方法注释
    @PostMapping("/course/list")//@RequestBody(required = false) 参数可以不必填
    public PageResult<CourseBase> list(PageParams pageParams,@RequestBody(required = false) QueryCourseParamsDto queryCourseParamsDto){
        //测试接口的硬编码
        /*CourseBase courseBase = new CourseBase();
        courseBase.setName("测试名称");
        courseBase.setCreateDate(LocalDateTime.now());
        List<CourseBase> courseBases = new ArrayList();
        courseBases.add(courseBase);
        PageResult pageResult = new PageResult<CourseBase>(courseBases,10,1,10);*/

        PageResult<CourseBase> courseBasePageResult = courseBaseInfoService.queryCourseBaseList(pageParams, queryCourseParamsDto);

        return courseBasePageResult;
    }

    @ApiOperation("新增课程")
    @PostMapping("/course")
    //@Validated 校验参数      @Validated(ValidationGroups.Insert.class) 组别类型
    public CourseBaseInfoDto createCourseBase(@RequestBody @Validated(ValidationGroups.Insert.class) AddCourseDto addCourseDto){

        //获取到用户所属机构的ID
        Long companyId = 1232141425L;
        CourseBaseInfoDto courseBase = courseBaseInfoService.createCourseBase(companyId, addCourseDto);
        return courseBase;
    }

    @ApiOperation("根据课程id查询接口")
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
    @PutMapping("/course")
    public CourseBaseInfoDto modifyCourseBase(@RequestBody @Validated(ValidationGroups.Update.class) EditCourseDto editCourseDto){
        //获取到用户所属机构的ID
        Long companyId = 1232141425L;
        CourseBaseInfoDto courseBaseInfoDto = courseBaseInfoService.updateCourseBase(companyId, editCourseDto);
        return courseBaseInfoDto;
    }

    /**
     * 删除课程信息(包括课程相关的基本信息、营销信息、课程计划、课程教师信息)
     * @param courseId 课程id
     */
    @ApiOperation("删除课程信息接口")
    @DeleteMapping ("/course/{courseId}")
    public void deleteCourseBaseInfo(@PathVariable Long courseId){
        courseBaseInfoService.deleteCourseBaseInfo(courseId);
    }
}
