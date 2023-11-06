package com.xuecheng.content.api;

import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.model.po.Exam;
import com.xuecheng.content.service.ExamService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Api(value = "考试接口", tags = "考试接口")
@RestController
public class ExamController {

    @Autowired
    ExamService examService;

    @ApiOperation("考试接口")
    @GetMapping("/course/exam")
    public PageResult<Exam> list(@RequestParam int pageNo,@RequestParam int pageSize) {
        //当前登录用户
//        SecurityUtil.XcUser user = SecurityUtil.getUser();
//        //用户所属机构id
//        Long companyId = null;
//        if (StringUtils.isNotEmpty(user.getCompanyId())) {
//            companyId = Long.parseLong(user.getCompanyId());
//        }
        Long companyId = 1232141425L;
        PageResult<Exam> examPageResult = examService.queryExamList(companyId, pageNo,pageSize);

        return examPageResult;
    }

}
