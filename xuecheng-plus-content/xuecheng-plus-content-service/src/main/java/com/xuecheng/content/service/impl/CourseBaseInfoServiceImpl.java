package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuecheng.base.exception.ValidationGroups;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.mapper.*;
import com.xuecheng.content.model.dto.AddCourseDto;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.EditCourseDto;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.*;
import com.xuecheng.content.service.CourseBaseInfoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class CourseBaseInfoServiceImpl implements CourseBaseInfoService {

    @Autowired
    CourseBaseMapper courseBaseMapper;

    @Autowired
    CourseMarketMapper courseMarketMapper;

    @Autowired
    CourseCategoryMapper courseCategoryMapper;

    @Autowired
    TeachplanMapper teachplanMapper;

    @Autowired
    CourseTeacherMapper courseTeacherMapper;


    @Override
    @PreAuthorize("hasAuthority('xc_teachmanager_course_list')")//指定权限标识符，拥有此权限才可以访问此方法
    public PageResult<CourseBase> queryCourseBaseList(Long companyId,PageParams pageParams, QueryCourseParamsDto courseParamsDto) {

        //拼接查询条件
        LambdaQueryWrapper<CourseBase> queryWrapper = new LambdaQueryWrapper<>();
        //根据名称模糊查询  在sql中拼接 SELECT COUNT(*) FROM course_base WHERE (name LIKE ?)
        queryWrapper.like(StringUtils.isNotEmpty(courseParamsDto.getCourseName()),CourseBase::getName,courseParamsDto.getCourseName());
        //根据课程审核状态查询 精确查询   course_base.audit_status = ?
        queryWrapper.eq(StringUtils.isNotEmpty(courseParamsDto.getAuditStatus()),CourseBase::getAuditStatus,courseParamsDto.getAuditStatus());
        //根据课程发布状态查询
        queryWrapper.eq(StringUtils.isNotEmpty(courseParamsDto.getPublishStatus()),CourseBase::getStatus,courseParamsDto.getPublishStatus());
        //根据培训机构id拼装查询条件
        queryWrapper.eq(CourseBase::getCompanyId,companyId);
        //创建page分页参数对象 参数：当前页码，每页记录数
        Page<CourseBase> page = new Page<>(pageParams.getPageNo(), pageParams.getPageSize());

        //开始进行分页查询
        Page<CourseBase> pageResult = courseBaseMapper.selectPage(page, queryWrapper);
        //数据列表
        List<CourseBase> items = pageResult.getRecords();
        //总记录数
        long total = pageResult.getTotal();

        PageResult<CourseBase> courseBasePageResult = new PageResult<CourseBase>(items,total,pageParams.getPageNo(), pageParams.getPageSize());
        return courseBasePageResult;
    }

    @Transactional//凡是增删改的方法都要加上事务控制注解
    @Override
    @PreAuthorize("hasAuthority('xc_teachmanager_course_list')")//指定权限标识符，拥有此权限才可以访问此方法
    public CourseBaseInfoDto createCourseBase(Long companyId, AddCourseDto dto) {
        //新增信息的顺序 先校验 再组装数据 再调用mapper插入
        //参数的合法性校验
        //合法性校验
//        if (StringUtils.isBlank(dto.getName())) {
//            XueChengPlusException.cast("课程名称为空");
//        }
//
//        if (StringUtils.isBlank(dto.getMt())) {
//            throw new RuntimeException("课程分类为空");
//        }
//
//        if (StringUtils.isBlank(dto.getSt())) {
//            throw new RuntimeException("课程分类为空");
//        }
//
//        if (StringUtils.isBlank(dto.getGrade())) {
//            throw new RuntimeException("课程等级为空");
//        }
//
//        if (StringUtils.isBlank(dto.getTeachmode())) {
//            throw new RuntimeException("教育模式为空");
//        }
//
//        if (StringUtils.isBlank(dto.getUsers())) {
//            throw new RuntimeException("适应人群为空");
//        }
//
//        if (StringUtils.isBlank(dto.getCharge())) {
//            throw new RuntimeException("收费规则为空");
//        }

        //向课程基本信息表course_base写入数据
        CourseBase courseBaseNew = new CourseBase();
        //将传入的页面的参数放到courseBaseNew对象
//        courseBaseNew.setName(dto.getName());
        //上边的从原始对象中get拿数据向新对象set，比较复杂
        BeanUtils.copyProperties(dto,courseBaseNew);//只要属性名称一致就可以拷贝 有可能出现空 覆盖 有值
        courseBaseNew.setCompanyId(companyId);//放到复制后面 因为上面的 空 覆盖 有值 影响
        courseBaseNew.setCreateDate(LocalDateTime.now());//没有传过来的 我们要自己写
        //审核状态默认为未提交       去数据字典里面查code
        courseBaseNew.setAuditStatus("202002");
        //发布状态为未发布
        courseBaseNew.setStatus("203001");
        //插入数据库
        int insert = courseBaseMapper.insert(courseBaseNew);
        if (insert <= 0) {
            throw new RuntimeException("添加课程失败");
        }

        //向课程营销表course_market写入数据
        CourseMarket courseMarketNew = new CourseMarket();
        //将页面输入的数据拷贝到courseMarketNew
        BeanUtils.copyProperties(dto,courseMarketNew);
        //课程的id
        Long courseId = courseBaseNew.getId();
        courseMarketNew.setId(courseId);
        //保存营销信息
        saveCourseMarket(courseMarketNew);
        //从数据库查询课程的详细信息，包括两部分
        CourseBaseInfoDto courseBaseInfo = getCourseBaseInfo(courseId);
        return courseBaseInfo;
    }

    //查询课程信息
    public CourseBaseInfoDto getCourseBaseInfo(Long courseId){
        //从课程基本信息表查询
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        if (courseBase == null){
            return null;
        }
        //从课程营销表查询
        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);

        //组装在一起
        CourseBaseInfoDto courseBaseInfoDto = new CourseBaseInfoDto();
        BeanUtils.copyProperties(courseBase,courseBaseInfoDto);
        if (courseMarket != null){
            BeanUtils.copyProperties(courseMarket,courseBaseInfoDto);
        }
        //通过courseCategoryMapper查询分类信息，将分类名称放在courseBaseInfoDto对象
        //查出大分类
        CourseCategory mtObj = courseCategoryMapper.selectById(courseBase.getMt());
        String mtName = mtObj.getName();
        //将大分类名称放在courseBaseInfoDto对象
        courseBaseInfoDto.setMtName(mtName);
        //查出小分类
        CourseCategory stObj = courseCategoryMapper.selectById(courseBase.getSt());
        String stName = stObj.getName();
        //将小分类名称放在courseBaseInfoDto对象
        courseBaseInfoDto.setStName(stName);

//        courseCategoryMapper.selectTreeNodes()
        return courseBaseInfoDto;
    }


    @Override
    public CourseBaseInfoDto updateCourseBase(Long companyId, EditCourseDto editCourseDto) {

        //拿到课程id
        Long id = editCourseDto.getId();
        //查询课程信息
        CourseBase courseBase = courseBaseMapper.selectById(id);
        //个人觉得 下面的if是多余的
        if (courseBase == null){
            XueChengPlusException.cast("课程不存在");
        }
        //数据合法性校验
        //根据具体的业务逻辑去校验(写在service里面)
        //本机构只能修改本机构的课程
        if (!companyId.equals(courseBase.getCompanyId())){
            XueChengPlusException.cast("本机构只能修改本机构的课程");
        }

        //封装数据
        BeanUtils.copyProperties(editCourseDto,courseBase);
        //修改时间(修改人自己写不了的，需要在这里给它加)
        courseBase.setChangeDate(LocalDateTime.now());
        //更新数据库
        int i = courseBaseMapper.updateById(courseBase);
        if (i <= 0){
            XueChengPlusException.cast("修改课程失败");
        }
        //更新营销信息
        //查询营销信息
        CourseMarket courseMarket = courseMarketMapper.selectById(id);
        //要判断营销信息是否为空 因为在添加的过程中，已经有营销信息，如果没查到的话，可能是数据库不小心删了，不符合实际
        if (courseMarket == null){
            XueChengPlusException.cast("课程营销信息不存在");
        }
        BeanUtils.copyProperties(editCourseDto,courseMarket);
        int i1 = courseMarketMapper.updateById(courseMarket);
        if (i1 <= 0){
            XueChengPlusException.cast("修改课程营销信息失败");
        }
        //查询课程信息
        CourseBaseInfoDto courseBaseInfo = getCourseBaseInfo(id);
        return courseBaseInfo;
    }

    /**
     * 删除课程信息(包括课程相关的基本信息、营销信息、课程计划、课程教师信息)
     * @param courseId 课程id
     */
    @Override
//    @PreAuthorize("hasAuthority('xc_teachmanager_course_del')")//指定权限标识符，拥有此权限才可以访问此方法
    public void deleteCourseBaseInfo(Long courseId) {
        //删除课程基本信息
        int i = courseBaseMapper.deleteById(courseId);
        if (i <= 0){
            XueChengPlusException.cast("删除课程基本信息失败");
        }
        //删除营销信息
        int i1 = courseMarketMapper.deleteById(courseId);
        if (i1 <= 0){
            XueChengPlusException.cast("删除课程营销信息失败");
        }
        //删除课程计划
        //删除基本信息
        LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Teachplan::getCourseId,courseId);
        int i2 = teachplanMapper.delete(queryWrapper);
        if (i2 <= 0){
            XueChengPlusException.cast("删除课程计划失败");
        }
        //删除小章节下的媒资信息
        LambdaQueryWrapper<Teachplan> queryWrapper1 = new LambdaQueryWrapper<>();
        queryWrapper1.eq(Teachplan::getCourseId,courseId);
        queryWrapper1.eq(Teachplan::getGrade,2);
        List<Teachplan> teachplans = teachplanMapper.selectList(queryWrapper1);
        for (Teachplan teachplan : teachplans) {
            teachplanMapper.deleteTeachplanMedia(teachplan.getId());
        }
        //删除课程教师信息
        LambdaQueryWrapper<CourseTeacher> queryWrapper2 = new LambdaQueryWrapper<>();
        queryWrapper2.eq(CourseTeacher::getCourseId,courseId);
        int i3 = courseTeacherMapper.delete(queryWrapper2);
        if (i3 <= 0){
            XueChengPlusException.cast("删除课程教师信息失败");
        }
    }

    //单独写一个方法保存营销信息，逻辑：存在则更新，不存在则添加

    private int saveCourseMarket(CourseMarket courseMarketNew){
        //参数的合法性校验
        String charge = courseMarketNew.getCharge();
        if (StringUtils.isEmpty(charge)){
            throw new RuntimeException("收费规则为空");
        }
        //如果课程收费，价格没有填写也需要抛出异常
        if (charge.equals("201001")){
            if (courseMarketNew.getPrice() == null || courseMarketNew.getPrice().floatValue() <= 0){
//                throw new RuntimeException("课程的价格不能为空并且必须大于0");
                XueChengPlusException.cast("课程的价格不能为空并且必须大于0");
            }
        }
        //从数据库查询营销信息，存在则更新，不存在则添加
        Long id = courseMarketNew.getId();//主键
        CourseMarket courseMarket = courseMarketMapper.selectById(id);
        if (courseMarket == null){
            //插入数据库
            int insert = courseMarketMapper.insert(courseMarketNew);
            return insert;
        }else {
            //将courseMarketNew拷贝到courseMarket
            BeanUtils.copyProperties(courseMarketNew,courseMarket);
            courseMarket.setId(courseMarketNew.getId());//以防courseMarketNew里面的主键为空
            //更新数据库
            int i = courseMarketMapper.updateById(courseMarket);
            return i;
        }
    }
}
