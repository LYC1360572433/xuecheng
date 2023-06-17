package com.xuecheng.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.learning.feignclient.ContentServiceClient;
import com.xuecheng.learning.mapper.XcChooseCourseMapper;
import com.xuecheng.learning.mapper.XcCourseTablesMapper;
import com.xuecheng.learning.model.dto.MyCourseTableParams;
import com.xuecheng.learning.model.dto.XcChooseCourseDto;
import com.xuecheng.learning.model.dto.XcCourseTablesDto;
import com.xuecheng.learning.model.po.XcChooseCourse;
import com.xuecheng.learning.model.po.XcCourseTables;
import com.xuecheng.learning.service.MyCourseTablesService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @description 选课相关接口实现
 */
@Slf4j
@Service
public class MyCourseTablesServiceImpl implements MyCourseTablesService {

    @Autowired
    XcChooseCourseMapper xcChooseCourseMapper;

    @Autowired
    XcCourseTablesMapper xcCourseTablesMapper;

    @Autowired
    ContentServiceClient contentServiceClient;

    @Autowired
    MyCourseTablesService myCourseTablesService;

    @Autowired
    MyCourseTablesServiceImpl currentProxy;

    @Transactional
    @Override
    public XcChooseCourseDto addChooseCourse(String userId, Long courseId) {
        //选课调用内容管理查询课程的收费规则
        //查询课程信息
        CoursePublish coursepublish = contentServiceClient.getCoursepublish(courseId);
        if (coursepublish == null) {
            XueChengPlusException.cast("课程不存在");
        }
        //查询课程收费规则
        String charge = coursepublish.getCharge();
        //选课记录
        XcChooseCourse chooseCourse = null;
        if ("201000".equals(charge)) {
            //如果课程免费，会向选课记录表、我的课程表写数据
            //添加免费课程
            chooseCourse = addFreeCourse(userId, coursepublish);//向选课记录表写
            //添加到我的课程表
            XcCourseTables xcCourseTables = addCourseTable(chooseCourse);
        } else {
            //如果是收费课程，会向选课记录表写数据
            chooseCourse = addChargeCourse(userId, coursepublish);
        }
        //判断学生的学习资格 前端要拿到这个状态 才知道当前用户的学习资格 此次拿到的是包含学习资格的dto
        XcCourseTablesDto xcCourseTablesDto = getLearningStatus(userId, courseId);
        //构造返回值
        XcChooseCourseDto xcChooseCourseDto = new XcChooseCourseDto();
        BeanUtils.copyProperties(chooseCourse, xcChooseCourseDto);
        //设置学习资格状态
        xcChooseCourseDto.setLearnStatus(xcCourseTablesDto.getLearnStatus());
        return xcChooseCourseDto;
    }

    /**
     * @param userId
     * @param courseId
     * @return XcCourseTablesDto 学习资格状态 [{"code":"702001","desc":"正常学习"},{"code":"702002","desc":"没有选课或选课后没有支付"},{"code":"702003","desc":"已过期需要申请续期或重新支付"}]
     * @description 判断学习资格
     */
    public XcCourseTablesDto getLearningStatus(String userId, Long courseId) {
        //不管如何 最终要返回对象 里面包含学习资格
        XcCourseTablesDto xcCourseTablesDto = new XcCourseTablesDto();
        //查询我的课程表,如果查不到，说明没有选课
        XcCourseTables xcCourseTables = getXcCourseTables(userId, courseId);
        if (xcCourseTables == null) {
            //没有选课或选课后没有支付
            xcCourseTablesDto.setLearnStatus("702002");
            return xcCourseTablesDto;
        }
        //如果查到了，判断是否过期，如果过期不能继续学习，没有过期可以继续学习
        BeanUtils.copyProperties(xcCourseTables, xcCourseTablesDto);
        //是否过期,true过期，false未过期
        boolean isExpires = xcCourseTables.getValidtimeEnd().isBefore(LocalDateTime.now());
        if (!isExpires) {
            //正常学习
            xcCourseTablesDto.setLearnStatus("702001");
            return xcCourseTablesDto;

        } else {
            //已过期
            xcCourseTablesDto.setLearnStatus("702003");
            return xcCourseTablesDto;
        }

    }

    @Override
    public boolean saveChooseCourseSuccess(String chooseCourseId) {
        //根据选课id查询选课表（担心是脏数据）
        XcChooseCourse xcChooseCourse = xcChooseCourseMapper.selectById(chooseCourseId);
        if (xcChooseCourse == null) {
            log.debug("接收购买课程的消息，根据选课id从数据库找不到选课记录：{}", chooseCourseId);
            return false;
        }
        //选课状态
        String status = xcChooseCourse.getStatus();
        //只有当未支付时 才更新为已支付
        if ("701002".equals(status)) {
            //更新选课记录的状态为支付成功
            xcChooseCourse.setStatus("701001");
            int i = xcChooseCourseMapper.updateById(xcChooseCourse);
            if (i <= 0) {
                log.debug("添加选课记录失败:{}", xcChooseCourse);
                XueChengPlusException.cast("添加选课记录失败");
            }
            //向我的课程表插入记录
            XcCourseTables xcCourseTables = addCourseTable(xcChooseCourse);
            return true;
        }
        return false;
    }

    public PageResult<XcCourseTables> mycoursetables(MyCourseTableParams params) {
        //页码
        long pageNo = params.getPage();
        //每页记录数,固定为4
        long pageSize = 4;
        //分页条件
        Page<XcCourseTables> page = new Page<>(pageNo, pageSize);
        //根据用户id查询
        String userId = params.getUserId();
        LambdaQueryWrapper<XcCourseTables> lambdaQueryWrapper = new LambdaQueryWrapper<XcCourseTables>().eq(XcCourseTables::getUserId, userId);

        //分页查询
        Page<XcCourseTables> pageResult = xcCourseTablesMapper.selectPage(page, lambdaQueryWrapper);
        //数据列表
        List<XcCourseTables> records = pageResult.getRecords();
        //记录数据总数
        long total = pageResult.getTotal();
        PageResult<XcCourseTables> courseTablesResult = new PageResult<>(records, total, pageNo, pageSize);
        return courseTablesResult;

    }

    //添加免费课程,免费课程加入选课记录表、我的课程表
    public XcChooseCourse addFreeCourse(String userId, CoursePublish coursepublish) {
        //查询选课记录表是否存在免费的且选课成功的订单 已经存在记录 就直接返回 因为主键 索引没有约束 存在同一个人同一门课在里面插两条记录
        //只是一个避免手段 不能完全避免
        LambdaQueryWrapper<XcChooseCourse> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper = queryWrapper.eq(XcChooseCourse::getUserId, userId)
                .eq(XcChooseCourse::getCourseId, coursepublish.getId())
                .eq(XcChooseCourse::getOrderType, "700001")//免费课程
                .eq(XcChooseCourse::getStatus, "701001");//选课成功
        List<XcChooseCourse> xcChooseCourses = xcChooseCourseMapper.selectList(queryWrapper);
        if (xcChooseCourses != null && xcChooseCourses.size() > 0) {
            return xcChooseCourses.get(0);//如果已经存在，则不需要插入，返回一个就行了
        }
        //添加选课记录信息
        XcChooseCourse xcChooseCourse = new XcChooseCourse();
        xcChooseCourse.setCourseId(coursepublish.getId());
        xcChooseCourse.setCourseName(coursepublish.getName());
        xcChooseCourse.setCoursePrice(0f);//免费课程价格为0
        xcChooseCourse.setUserId(userId);
        xcChooseCourse.setCompanyId(coursepublish.getCompanyId());
        xcChooseCourse.setOrderType("700001");//免费课程
        xcChooseCourse.setCreateDate(LocalDateTime.now());
        xcChooseCourse.setStatus("701001");//选课成功

        xcChooseCourse.setValidDays(365);//免费课程默认365
        xcChooseCourse.setValidtimeStart(LocalDateTime.now());
        xcChooseCourse.setValidtimeEnd(LocalDateTime.now().plusDays(365));// +365
        int insert = xcChooseCourseMapper.insert(xcChooseCourse);
        if (insert <= 0) {
            XueChengPlusException.cast("添加选课记录失败");
        }

        return xcChooseCourse;

    }

    /**
     * @param xcChooseCourse 选课记录
     * @return com.xuecheng.learning.model.po.XcCourseTables
     * @description 添加到我的课程表
     * 我的课程表 主键自增 用户id和课程id唯一
     */
    public XcCourseTables addCourseTable(XcChooseCourse xcChooseCourse) {
        //选课记录完成且未过期可以添加课程到课程表 选课成功了，才可以向我的课程表添加
        String status = xcChooseCourse.getStatus();
        if (!"701001".equals(status)) {
            XueChengPlusException.cast("选课未成功，无法添加到课程表");
        }
        //查询我的课程表 不能重复添加
        XcCourseTables xcCourseTables = getXcCourseTables(xcChooseCourse.getUserId(), xcChooseCourse.getCourseId());
        if (xcCourseTables != null) {
            return xcCourseTables;
        }
        xcCourseTables = new XcCourseTables();
        BeanUtils.copyProperties(xcChooseCourse, xcCourseTables);
        xcCourseTables.setChooseCourseId(xcChooseCourse.getId());//记录选课表的主键
        xcCourseTables.setCourseType(xcChooseCourse.getOrderType());//选课类型
        xcCourseTables.setCreateDate(LocalDateTime.now());
        int insert = xcCourseTablesMapper.insert(xcCourseTables);
        if (insert <= 0) {
            XueChengPlusException.cast("添加我的课程表失败");
        }

        return xcCourseTables;

    }

    /**
     * @param userId
     * @param courseId
     * @return com.xuecheng.learning.model.po.XcCourseTables
     * @description 根据课程和用户查询我的课程表中某一门课程
     */
    public XcCourseTables getXcCourseTables(String userId, Long courseId) {
        //只能查到一条记录 所以selectOne
        XcCourseTables xcCourseTables = xcCourseTablesMapper.selectOne(new LambdaQueryWrapper<XcCourseTables>().eq(XcCourseTables::getUserId, userId).eq(XcCourseTables::getCourseId, courseId));
        return xcCourseTables;

    }

    //添加收费课程
    public XcChooseCourse addChargeCourse(String userId, CoursePublish coursepublish) {
        //选课记录表没有约束 要判断
        //如果存在收费的选课记录且选课状态为待支付 直接返回
        LambdaQueryWrapper<XcChooseCourse> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper = queryWrapper.eq(XcChooseCourse::getUserId, userId)
                .eq(XcChooseCourse::getCourseId, coursepublish.getId())
                .eq(XcChooseCourse::getOrderType, "700002")//收费订单
                .eq(XcChooseCourse::getStatus, "701002");//待支付
        List<XcChooseCourse> xcChooseCourses = xcChooseCourseMapper.selectList(queryWrapper);
        if (xcChooseCourses != null && xcChooseCourses.size() > 0) {
            return xcChooseCourses.get(0);
        }

        XcChooseCourse xcChooseCourse = new XcChooseCourse();
        xcChooseCourse.setCourseId(coursepublish.getId());
        xcChooseCourse.setCourseName(coursepublish.getName());
        xcChooseCourse.setCoursePrice(coursepublish.getPrice());
        xcChooseCourse.setUserId(userId);
        xcChooseCourse.setCompanyId(coursepublish.getCompanyId());
        xcChooseCourse.setOrderType("700002");//收费课程
        xcChooseCourse.setCreateDate(LocalDateTime.now());
        xcChooseCourse.setStatus("701002");//待支付

        xcChooseCourse.setValidDays(coursepublish.getValidDays());
        xcChooseCourse.setValidtimeStart(LocalDateTime.now());
        xcChooseCourse.setValidtimeEnd(LocalDateTime.now().plusDays(coursepublish.getValidDays()));
        int insert = xcChooseCourseMapper.insert(xcChooseCourse);
        if (insert <= 0) {
            XueChengPlusException.cast("收费课程添加选课记录表失败");
        }
        return xcChooseCourse;
    }
}