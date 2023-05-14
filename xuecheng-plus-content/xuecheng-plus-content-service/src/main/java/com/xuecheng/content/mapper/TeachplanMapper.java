package com.xuecheng.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.Teachplan;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 课程计划 Mapper 接口
 * </p>
 *
 * @author itcast
 */
public interface TeachplanMapper extends BaseMapper<Teachplan> {

    //课程计划查询
    public List<TeachplanDto> selectTreeNodes(Long courseId);

    //删除小章节下的媒资信息
    public void deleteTeachplanMedia(Long teachplanId);

    //根据排序号查课程计划
    public Teachplan selectTeachplanByOrderby(@Param("orderby")Integer orderby,@Param("grade")Integer grade,@Param("parentid")Long parentid,@Param("courseId")Long courseId);

}
