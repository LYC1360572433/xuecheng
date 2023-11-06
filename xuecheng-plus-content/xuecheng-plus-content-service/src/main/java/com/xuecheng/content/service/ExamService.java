package com.xuecheng.content.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.model.po.Exam;

/**
* @author 林英淳
* @description 针对表【exam】的数据库操作Service
* @createDate 2023-08-22 11:06:48
*/
public interface ExamService extends IService<Exam> {

    /***
     * 考试接口
     * @return
     */
    PageResult<Exam> queryExamList(Long companyId, int pageNo, int pageSize);

}
