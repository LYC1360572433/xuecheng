package com.xuecheng.learning.api;

import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.learning.model.dto.MyCourseTableParams;
import com.xuecheng.learning.model.dto.XcChooseCourseDto;
import com.xuecheng.learning.model.dto.XcCourseTablesDto;
import com.xuecheng.learning.model.po.XcCourseTables;
import com.xuecheng.learning.service.MyCourseTablesService;
import com.xuecheng.learning.util.SecurityUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


/**
 * @description 我的课程表接口
 */

@Api(value = "我的课程表接口", tags = "我的课程表接口")
@Slf4j
@RestController
public class MyCourseTablesController {

    @Autowired
    MyCourseTablesService myCourseTablesService;

    @ApiOperation("添加选课")
    @PostMapping("/choosecourse/{courseId}")
    public XcChooseCourseDto addChooseCourse(@PathVariable("courseId") Long courseId) {

        //当前登录的用户
        SecurityUtil.XcUser user = SecurityUtil.getUser();
        //正常网关会拦截 只有登录了才能到这里 所以不存在 找不到用户
        //加校验 是因为现在把网关全放开了
        if (user == null) {
            XueChengPlusException.cast("请登录后继续选课");
        }
        //用户id
        String userId = user.getId();
        return myCourseTablesService.addChooseCourse(userId, courseId);
    }

    @ApiOperation("查询学习资格")
    @PostMapping("/choosecourse/learnstatus/{courseId}")
    public XcCourseTablesDto getLearnstatus(@PathVariable("courseId") Long courseId) {
        //当前登录的用户
        SecurityUtil.XcUser user = SecurityUtil.getUser();
        if (user == null) {
            XueChengPlusException.cast("请登录后继续选课");
        }
        //用户id
        String userId = user.getId();
        XcCourseTablesDto learningStatus = myCourseTablesService.getLearningStatus(userId, courseId);
        return learningStatus;

    }

    @ApiOperation("我的课程表")
//    @PreAuthorize("hasAuthority('xc_teachmanager_course_list')")//指定权限标识符，拥有此权限才可以访问此方法
    @GetMapping("/mycoursetable")
    public PageResult<XcCourseTables> mycoursetable(MyCourseTableParams params) {
//        //登录用户
//        SecurityUtil.XcUser user = SecurityUtil.getUser();
//        String userId = user.getId();
//        //设置当前的登录用户
//        params.setUserId(userId);

        return myCourseTablesService.mycoursetables(params);
    }

}
