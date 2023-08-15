package com.xuecheng.search.service;

import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.search.dto.SearchCourseParamDto;
import com.xuecheng.search.dto.SearchPageResultDto;
import com.xuecheng.search.po.CourseIndex;

import java.util.List;

/**
 * @description 课程搜索service
 */
public interface CourseSearchService {


    /**
     * @param pageParams           分页参数
     * @param searchCourseParamDto 搜索条件
     * @return com.xuecheng.base.model.PageResult<com.xuecheng.search.po.CourseIndex> 课程列表
     * @description 搜索课程列表
     */
    SearchPageResultDto<CourseIndex> queryCoursePubIndex(PageParams pageParams, SearchCourseParamDto searchCourseParamDto);

    /**
     * 自动补全
     * @param prefix 前缀
     * @return
     */
    List<String> getSuggestions(String prefix);
}
