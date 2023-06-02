package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.RestResponse;
import com.xuecheng.content.mapper.TeachplanMapper;
import com.xuecheng.content.mapper.TeachplanMediaMapper;
import com.xuecheng.content.model.dto.BindTeachplanMediaDto;
import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.Teachplan;
import com.xuecheng.content.model.po.TeachplanMedia;
import com.xuecheng.content.service.TeachplanService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TeachplanServiceImpl implements TeachplanService {

    @Autowired
    TeachplanMapper teachplanMapper;

    //要操作哪张表就把哪张表对应的mapper注入进来
    @Autowired
    TeachplanMediaMapper teachplanMediaMapper;

    @Override
    public List<TeachplanDto> findTeachplanTree(Long courseId) {
        List<TeachplanDto> teachplanDtos = teachplanMapper.selectTreeNodes(courseId);
        return teachplanDtos;
    }

    private int getTeachplanCount(Long courseId, Long parentId) {
        LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Teachplan::getCourseId, courseId).eq(Teachplan::getParentid, parentId);
        Integer count = teachplanMapper.selectCount(queryWrapper);
        return count + 1;
    }

    @Override
    public void saveTeachplan(SaveTeachplanDto saveTeachplanDto) {
        //通过课程计划id判断是新增还是修改
        Long teachplanId = saveTeachplanDto.getId();
        if (teachplanId == null) {
            //新增
            Teachplan teachplan = new Teachplan();
            BeanUtils.copyProperties(saveTeachplanDto, teachplan);
            //确定排序字段，找到它的同级节点个数，排序字段就是个数加1
            //select count(1) from teachplan where course_id = 117 and parentid = 268
            Long courseId = saveTeachplanDto.getCourseId();
            Long parentid = saveTeachplanDto.getParentid();
            int teachplanCount = getTeachplanCount(courseId, parentid);
            teachplan.setOrderby(teachplanCount);

            teachplanMapper.insert(teachplan);
        } else {
            //修改
            Teachplan teachplan = teachplanMapper.selectById(teachplanId);
            //将参数复制到原来的对象当中去
            BeanUtils.copyProperties(saveTeachplanDto, teachplan);
            teachplanMapper.updateById(teachplan);
        }
    }

    @Override
    public void delete(Long teachplanId) {
        //根据id查询课程计划
        Teachplan teachplan = teachplanMapper.selectById(teachplanId);
        //如果该课程计划的parentId = 0,说明他是大章节
        if (teachplan.getParentid() == 0) {
            //既然是大章节，删除时就应该判断下面还有没有小章节
            LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Teachplan::getParentid, teachplanId);
            Integer count = teachplanMapper.selectCount(queryWrapper);
            //如果小章节数量为 0,可以直接删除
            if (count == 0) {
                teachplanMapper.deleteById(teachplanId);
            } else {
                //小章节数量 不为 0,不可以直接删除,报出提示
                XueChengPlusException.cast("120409","课程计划信息还有子级信息，无法操作");
            }
        }else {
            //parentId != 0,说明是小章节，可以直接删除小章节和关联信息
            teachplanMapper.deleteTeachplanMedia(teachplanId);
            teachplanMapper.deleteById(teachplanId);
        }
    }

    @Override
    public void movedown(Long teachplanId) {
        //根据id查询课程计划
        Teachplan teachplan = teachplanMapper.selectById(teachplanId);
        Long courseId = teachplan.getCourseId();
        //查询等级 判断是大章节下移还是小章节下移
        Integer grade = teachplan.getGrade();
        //查询下移对象的排序号
        Integer orderby = teachplan.getOrderby();
        //查询小章节的parentId
        Long parentid = teachplan.getParentid();
        //判断该排序号是否为最底
        //将排序号 + 1,然后根据这去查是否有该课程对象
        Teachplan teachplan1 = teachplanMapper.selectTeachplanByOrderby(orderby + 1,grade,parentid,courseId);
        //如果查不到 报错
        if (teachplan1 == null){
            XueChengPlusException.cast("已经是最底，无法下移");
        }
        //查到了，与排序号 + 1的互换
        else {
            Integer orderby1 = teachplan1.getOrderby();
            teachplan.setOrderby(orderby1);
            teachplan1.setOrderby(orderby);
            teachplanMapper.updateById(teachplan);
            teachplanMapper.updateById(teachplan1);
        }
    }

    @Override
    public void moveup(Long teachplanId) {
        //根据id查询课程计划
        Teachplan teachplan = teachplanMapper.selectById(teachplanId);
        Long courseId = teachplan.getCourseId();
        //查询等级 判断是大章节上移还是小章节上移
        Integer grade = teachplan.getGrade();
        //查询上移对象的排序号
        Integer orderby = teachplan.getOrderby();
        //查询小章节的parentId
        Long parentid = teachplan.getParentid();
        //判断该排序号是否为最顶
        //将排序号 - 1,然后根据这去查是否有该课程对象
        Teachplan teachplan1 = teachplanMapper.selectTeachplanByOrderby(orderby - 1,grade,parentid,courseId);
        //如果查不到 报错
        if (teachplan1 == null){
            XueChengPlusException.cast("已经是最顶，无法上移");
        }
        //查到了，与排序号 - 1的互换
        else {
            Integer orderby1 = teachplan1.getOrderby();
            teachplan.setOrderby(orderby1);
            teachplan1.setOrderby(orderby);
            teachplanMapper.updateById(teachplan);
            teachplanMapper.updateById(teachplan1);
        }
    }

    @Transactional//操作数据库，需要事务控制
    @Override
    public TeachplanMedia associationMedia(BindTeachplanMediaDto bindTeachplanMediaDto) {

        //教学计划id
        Long teachplanId = bindTeachplanMediaDto.getTeachplanId();
        Teachplan teachplan = teachplanMapper.selectById(teachplanId);
        if(teachplan==null){
            XueChengPlusException.cast("教学计划不存在");
        }

        Integer grade = teachplan.getGrade();
        if(grade!=2){
            XueChengPlusException.cast("只允许第二级教学计划绑定媒资文件");
        }

        //先删除原有记录，根据课程计划的id，删除它所绑定的媒资
        LambdaQueryWrapper<TeachplanMedia> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TeachplanMedia::getTeachplanId,bindTeachplanMediaDto.getTeachplanId());
        int delete = teachplanMediaMapper.delete(queryWrapper);

        //再添加新记录
        //课程id
        Long courseId = teachplan.getCourseId();

        TeachplanMedia teachplanMedia = new TeachplanMedia();
        BeanUtils.copyProperties(bindTeachplanMediaDto,teachplanMedia);

        //因为模型类里面的FileName与teachplanMedia里面的MediaFilename字段名不一样，复制不进去
        teachplanMedia.setMediaFilename(bindTeachplanMediaDto.getFileName());
        //模型类里面没有的，要自己手动添加
        teachplanMedia.setCourseId(courseId);
        teachplanMedia.setCreateDate(LocalDateTime.now());
        teachplanMediaMapper.insert(teachplanMedia);
        return teachplanMedia;
    }

    /**
     * 删除课程计划的媒资信息
     * @param teachPlanId 课程计划id
     * @param mediaId 媒资信息id
     * @return RestResponse
     */
    @Override
    public RestResponse deleteMediaByteachplanId(Long teachPlanId,String mediaId) {
        LambdaQueryWrapper<TeachplanMedia> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TeachplanMedia::getMediaId,mediaId);
        queryWrapper.eq(TeachplanMedia::getTeachplanId,teachPlanId);
        int delete = teachplanMediaMapper.delete(queryWrapper);
        if (delete > 0){
            return new RestResponse(200,"删除课程计划的媒资信息成功");
        }
        return new RestResponse(500,"删除课程计划的媒资信息成功");
    }
}
