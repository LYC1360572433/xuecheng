package com.xuecheng.learning.service.impl;

import com.alibaba.cloud.commons.lang.StringUtils;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.RestResponse;
import com.xuecheng.base.utils.JsonUtil;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.content.model.po.Teachplan;
import com.xuecheng.learning.feignclient.ContentServiceClient;
import com.xuecheng.learning.feignclient.MediaServiceClient;
import com.xuecheng.learning.model.dto.XcCourseTablesDto;
import com.xuecheng.learning.service.LearningService;
import com.xuecheng.learning.service.MyCourseTablesService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.After;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 在线学习接口实现类
 */

@Slf4j
@Service
public class LearningServiceImpl implements LearningService {

    @Autowired
    MyCourseTablesService myCourseTablesService;
    @Autowired
    ContentServiceClient contentServiceClient;
    @Autowired
    MediaServiceClient mediaServiceClient;

    @Override
    public RestResponse<String> getVideo(String userId,Long courseId,Long teachplanId, String mediaId) {
        //查询课程信息
        CoursePublish coursepublish = contentServiceClient.getCoursepublish(courseId);
        //判断如果没空 不再继续
        if(coursepublish==null){
            XueChengPlusException.cast("课程信息不存在");
        }
        //远程调用内容管理服务 根据课程id(teachplanId)去查询课程计划信息，如果is_preview的值为1 表示支持试学
        //也可以从coursepublish对象中解析出课程计划信息去判断是否支持试学
        //如果支持试学，调用媒资服务查询视频的播放地址，返回
//        Teachplan teachplan = JsonUtil.jsonToObject(coursepublish.getTeachplan(), Teachplan.class);
//        String isPreview = teachplan.getIsPreview();
//        if (isPreview.equals("1")){
//            return mediaServiceClient.getPlayUrlByMediaId(mediaId);
//        }
        //如果登录 用户已登录
        if(StringUtils.isNotEmpty(userId)){
            //判断是否选课，根据选课情况判断学习资格
            XcCourseTablesDto xcCourseTablesDto = myCourseTablesService.getLearningStatus(userId, courseId);
            //学习资格状态 [{"code":"702001","desc":"正常学习"},{"code":"702002","desc":"没有选课或选课后没有支付"},{"code":"702003","desc":"已过期需要申请续期或重新支付"}]
            String learnStatus = xcCourseTablesDto.getLearnStatus();
            if(learnStatus.equals("702002")){
                RestResponse.validfail("无法学习，因为没有选课或选课后没有支付");
            }else if(learnStatus.equals("702003")){
                RestResponse.validfail("已过期需要申请续期或重新支付");
            }else {
                //有资格学习，要返回视频的播放地址
                //远程调用媒资获取视频播放地址
                return mediaServiceClient.getPlayUrlByMediaId(mediaId);
            }
        }

        //如果用户未登录或未选课 判断是否收费
        //取出课程的收费规则
        String charge = coursepublish.getCharge();
        if(charge.equals("201000")){//免费可以正常学习
            //有资格学习，要返回视频的播放地址
            //远程调用媒资获取视频播放地址
            return mediaServiceClient.getPlayUrlByMediaId(mediaId);
        }
        //没登录 并且该课是收费的
        return RestResponse.validfail("该课程没有选课");
    }
}
