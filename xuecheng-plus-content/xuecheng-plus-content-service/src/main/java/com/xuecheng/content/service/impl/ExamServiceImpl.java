package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.mapper.ExamMapper;
import com.xuecheng.content.model.po.Exam;
import com.xuecheng.content.service.ExamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
* @author 林英淳
* @description 针对表【exam】的数据库操作Service实现
* @createDate 2023-08-22 11:06:48
*/
@Service
public class ExamServiceImpl extends ServiceImpl<ExamMapper, Exam>
    implements ExamService {

    @Autowired
    ExamMapper examMapper;

    @Override
    public PageResult<Exam> queryExamList(Long companyId, int pageNo,int pageSize) {
        LambdaQueryWrapper<Exam> queryWrapper = new LambdaQueryWrapper<>();

        Page<Exam> page = new Page<>(pageNo,pageSize);
        //开始进行分页查询
        Page<Exam> pageResult = examMapper.selectPage(page, queryWrapper);
        //数据列表
        List<Exam> items = pageResult.getRecords();
        //总记录数
        long total = pageResult.getTotal();

        PageResult<Exam> examPageResult = new PageResult<Exam>(items,total,pageNo,pageSize );
        return examPageResult;
    }
}




