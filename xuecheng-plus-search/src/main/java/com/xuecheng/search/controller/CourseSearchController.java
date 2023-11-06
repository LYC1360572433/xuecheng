package com.xuecheng.search.controller;

import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.search.dto.SearchCourseParamDto;
import com.xuecheng.search.dto.SearchPageResultDto;
import com.xuecheng.search.po.CourseIndex;
import com.xuecheng.search.service.CourseSearchService;
import com.xuecheng.search.util.SecurityUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @description 课程搜索接口
 */
@Api(value = "课程搜索接口", tags = "课程搜索接口")
@RestController
@RequestMapping("/course")
public class CourseSearchController {

    @Autowired
    CourseSearchService courseSearchService;


    @ApiOperation("课程搜索列表")
    @GetMapping("/list")
    public SearchPageResultDto<CourseIndex> list(PageParams pageParams, SearchCourseParamDto searchCourseParamDto) {

        return courseSearchService.queryCoursePubIndex(pageParams, searchCourseParamDto,null);

    }

    @ApiOperation("课程搜索列表")
    @GetMapping("/myList")
    public SearchPageResultDto<CourseIndex> myList(PageParams pageParams, SearchCourseParamDto searchCourseParamDto) {

        //当前登录的用户
        SecurityUtil.XcUser user = SecurityUtil.getUser();
        if (user == null) {
            XueChengPlusException.cast("请登录");
        }
        //用户id
        String userId = user.getId();
        return courseSearchService.queryCoursePubIndex(pageParams, searchCourseParamDto,userId);

    }

    @ApiOperation("自动补全")
    @GetMapping("/suggestion")
    public List<String> getSuggestions(@RequestParam("key") String prefix) {

        return courseSearchService.getSuggestions(prefix);

    }
}
