package com.xuecheng.messagesdk.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xuecheng.messagesdk.model.po.MqMessage;

import java.util.List;

/**
 * 消息表处理服务类
 */
public interface MqMessageService extends IService<MqMessage> {

    /**
     * @param shardIndex 分片序号
     * @param shardTotal 分片总数
     * @param count      扫描记录数
     * @return java.util.List 消息记录
     * @description 扫描消息表记录，采用与扫描视频处理表相同的思路
     */
    public List<MqMessage> getMessageList(int shardIndex, int shardTotal, String messageType, int count);

    /**
     * @param businessKey1 业务id1
     * @param businessKey2 业务id2
     * @param businessKey3 业务id3
     * @return com.xuecheng.messagesdk.model.po.MqMessage 消息内容
     * @description 添加消息
     */
    public MqMessage addMessage(String messageType, String businessKey1, String businessKey2, String businessKey3);

    /**
     * @param id 消息id
     * @return int 更新成功：1
     * @description 完成任务
     */
    public int completed(long id);

    /**
     * @param id 消息id
     * @return int 更新成功：1
     * @description 完成阶段任务
     */
    public int completedStageOne(long id);

    public int completedStageTwo(long id);

    public int completedStageThree(long id);

    public int completedStageFour(long id);

    /**
     * @param id
     * @return int
     * @description 查询阶段状态
     */
    public int getStageOne(long id);

    public int getStageTwo(long id);

    public int getStageThree(long id);

    public int getStageFour(long id);

}
