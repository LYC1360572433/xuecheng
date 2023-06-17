package com.xuecheng.orders.service;

import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.orders.model.dto.AddOrderDto;
import com.xuecheng.orders.model.dto.PayRecordDto;
import com.xuecheng.orders.model.dto.PayStatusDto;
import com.xuecheng.orders.model.po.XcPayRecord;

/**
 * 订单相关的service接口
 */
public interface OrderService {


    /**
     * @param addOrderDto 订单信息
     * @return PayRecordDto 支付交易记录(包括二维码)
     * @description 创建商品订单
     */
    public PayRecordDto createOrder(String userId, AddOrderDto addOrderDto);

    /**
     * @param payNo 交易记录号
     * @return com.xuecheng.orders.model.po.XcPayRecord
     * @description 查询支付交易记录
     */
    public XcPayRecord getPayRecordByPayno(String payNo);

    /**
     * 请求支付宝查询支付结果
     * @param payNo 支付记录id
     * @return 支付记录信息
     */
    public PayRecordDto queryPayResult(String payNo);

    /**
     * @description 保存支付宝支付结果 订单表和支付记录表
     * @param payStatusDto  支付结果信息
     * @return void
     */
    public void saveAliPayStatus(PayStatusDto payStatusDto) ;

    /**
     * 发送通知结果
     * @param message
     */
    public void notifyPayResult(MqMessage message);
}
