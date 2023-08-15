package com.xuecheng.orders.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.utils.IdWorkerUtils;
import com.xuecheng.base.utils.QRCodeUtil;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MqMessageService;
import com.xuecheng.orders.config.AlipayConfig;
import com.xuecheng.orders.config.PayNotifyConfig;
import com.xuecheng.orders.mapper.XcOrdersGoodsMapper;
import com.xuecheng.orders.mapper.XcOrdersMapper;
import com.xuecheng.orders.mapper.XcPayRecordMapper;
import com.xuecheng.orders.model.dto.AddOrderDto;
import com.xuecheng.orders.model.dto.PayRecordDto;
import com.xuecheng.orders.model.dto.PayStatusDto;
import com.xuecheng.orders.model.po.XcOrders;
import com.xuecheng.orders.model.po.XcOrdersGoods;
import com.xuecheng.orders.model.po.XcPayRecord;
import com.xuecheng.orders.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 订单相关接口的实现类
 */
@Slf4j
@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    XcOrdersMapper ordersMapper;
    @Autowired
    XcOrdersGoodsMapper ordersGoodsMapper;
    @Autowired
    XcPayRecordMapper payRecordMapper;
    @Autowired
    MqMessageService mqMessageService;
    @Autowired
    RabbitTemplate rabbitTemplate;

    @Value("${pay.qrcodeurl}")
    String qrcodeurl;
    @Value("${pay.alipay.APP_ID}")
    String APP_ID;
    @Value("${pay.alipay.APP_PRIVATE_KEY}")
    String APP_PRIVATE_KEY;
    @Value("${pay.alipay.ALIPAY_PUBLIC_KEY}")
    String ALIPAY_PUBLIC_KEY;

    @Autowired
    OrderServiceImpl currentProxy;

    @Transactional
    @Override
    public PayRecordDto createOrder(String userId, AddOrderDto addOrderDto) {

        //添加商品订单(插入订单表)：插入订单主表，订单明细表
        XcOrders xcOrders = saveXcOrders(userId, addOrderDto);
        if (xcOrders == null) {
            XueChengPlusException.cast("订单创建失败");
        }
        if (xcOrders.getStatus().equals("600002")) {
            XueChengPlusException.cast("订单已支付");
        }
        //添加支付交易记录(插入支付记录)
        XcPayRecord payRecord = createPayRecord(xcOrders);
        //生成二维码
        String qrCode = null;
        try {
            //url要可以被模拟器访问到，url为下单接口(稍后定义)
            //将%s用PayNo替换
            String url = String.format(qrcodeurl, payRecord.getPayNo());
            qrCode = new QRCodeUtil().createQRCode(url, 200, 200);
        } catch (IOException e) {
            XueChengPlusException.cast("生成二维码出错");
        }
        PayRecordDto payRecordDto = new PayRecordDto();
        BeanUtils.copyProperties(payRecord, payRecordDto);
        payRecordDto.setQrcode(qrCode);
        return payRecordDto;
    }

    /**
     * 插入订单表和订单明细表
     * @param userId      用户id
     * @param addOrderDto 订单扩展类
     * @return
     */
    @Transactional
    public XcOrders saveXcOrders(String userId, AddOrderDto addOrderDto) {
        //幂等性判断  订单同一选课只能有一条记录 进行幂等性处理判断(重点)
        XcOrders order = getOrderByBusinessId(addOrderDto.getOutBusinessId());
        if (order != null) {
            //存在，直接返回订单 不再生成订单，意味着不会生成新的二维码
            return order;
        }
        //不存在，创建订单 插入订单表
        order = new XcOrders();
        //使用雪花算法生成订单号
        long orderId = IdWorkerUtils.getInstance().nextId();
        order.setId(orderId);
        order.setTotalPrice(addOrderDto.getTotalPrice());//总价
        order.setCreateDate(LocalDateTime.now());
        order.setStatus("600001");//未支付
        order.setUserId(userId);
        order.setOrderType(addOrderDto.getOrderType());//订单类型 购买课程 60201
        order.setOrderName(addOrderDto.getOrderName());
        order.setOrderDetail(addOrderDto.getOrderDetail());//订单明细 不可为空 json格式
        order.setOrderDescrip(addOrderDto.getOrderDescrip());
        order.setOutBusinessId(addOrderDto.getOutBusinessId());//如果是选课，这里记录选课表主键
        int insert = ordersMapper.insert(order);
        if (insert < 0) {
            XueChengPlusException.cast("添加订单失败");
        }
        //插入订单明细表 将前端传入的订单明细json串转成List
        //拿到订单明细
        String orderDetailJson = addOrderDto.getOrderDetail();
        //json转成订单明细表po类  因为订单明细表指多个小订单 所以是List
        List<XcOrdersGoods> xcOrdersGoodsList = JSON.parseArray(orderDetailJson, XcOrdersGoods.class);
        //每次遍历都生成订单明细表 将遍历到的小订单插入订单明细表里面
        xcOrdersGoodsList.forEach(goods -> {
            XcOrdersGoods xcOrdersGoods = new XcOrdersGoods();
            BeanUtils.copyProperties(goods, xcOrdersGoods);
            //addOrderDto里面的订单明细没有订单号 所以要自己添加
            xcOrdersGoods.setOrderId(orderId);//订单号 因为每个小订单的订单号都一样
            ordersGoodsMapper.insert(xcOrdersGoods);
        });
        return order;
    }

    //根据业务id查询订单 业务id就是选课记录表中的主键
    public XcOrders getOrderByBusinessId(String outBusinessId) {
        //为什么selectOne 因为数据库约束了，不可能有多条记录
        XcOrders orders = ordersMapper.selectOne(new LambdaQueryWrapper<XcOrders>().eq(XcOrders::getOutBusinessId, outBusinessId));
        return orders;
    }

    /**
     * 保存支付记录
     *
     * @param orders
     * @return
     */
    public XcPayRecord createPayRecord(XcOrders orders) {
        //如果此订单不存在 不能添加支付记录
        if (orders == null) {
            XueChengPlusException.cast("订单不存在");
        }
        //如果此订单支付结果为成功，不再添加支付记录，避免重复支付
        if (orders.getStatus().equals("601002")) {
            XueChengPlusException.cast("订单已支付");
        }
        //添加支付记录(在这里不需要添加支付宝订单号 支付成功才生成)
        XcPayRecord payRecord = new XcPayRecord();
        //雪花算法生成支付交易流水号
        long payNo = IdWorkerUtils.getInstance().nextId();
        payRecord.setPayNo(payNo);
        payRecord.setOrderId(orders.getId());//商品订单号
        payRecord.setOrderName(orders.getOrderName());
        payRecord.setTotalPrice(orders.getTotalPrice());
        payRecord.setCurrency("CNY");
        payRecord.setCreateDate(LocalDateTime.now());
        payRecord.setStatus("601001");//未支付
        payRecord.setUserId(orders.getUserId());
        int insert = payRecordMapper.insert(payRecord);
        if (insert < 0) {
            XueChengPlusException.cast("插入支付记录失败");
        }
        return payRecord;

    }

    @Override
    public XcPayRecord getPayRecordByPayno(String payNo) {
        XcPayRecord xcPayRecord = payRecordMapper.selectOne(new LambdaQueryWrapper<XcPayRecord>().eq(XcPayRecord::getPayNo, payNo));
        return xcPayRecord;
    }


    @Override
    public PayRecordDto queryPayResult(String payNo) {
        //根据系统交易号判断是否有该支付记录 如果有，说明支付成功
        XcPayRecord payRecord = getPayRecordByPayno(payNo);
        if (payRecord == null) {
            XueChengPlusException.cast("请重新点击支付获取二维码");
        }
        //再检查支付记录表里面的支付状态
        String status = payRecord.getStatus();
        //如果支付成功直接返回
        if ("601002".equals(status)) {
            PayRecordDto payRecordDto = new PayRecordDto();
            BeanUtils.copyProperties(payRecord, payRecordDto);
            return payRecordDto;
        }
        //调用支付宝接口查询支付结果
        PayStatusDto payStatusDto = queryPayResultFromAlipay(payNo);
        //拿到支付结果更新支付记录表和订单表的支付状态
        currentProxy.saveAliPayStatus(payStatusDto);
        //要返回最新的支付记录信息   重新查询支付记录
        payRecord = getPayRecordByPayno(payNo);
        PayRecordDto payRecordDto = new PayRecordDto();
        BeanUtils.copyProperties(payRecord, payRecordDto);
        return payRecordDto;

    }

    /**
     * 请求支付宝查询支付结果
     * @param payNo 支付交易号
     * @return 支付结果
     */
    public PayStatusDto queryPayResultFromAlipay(String payNo) {
        //========请求支付宝查询支付结果=============
        AlipayClient alipayClient = new DefaultAlipayClient(AlipayConfig.URL, APP_ID, APP_PRIVATE_KEY, "json", AlipayConfig.CHARSET, ALIPAY_PUBLIC_KEY, AlipayConfig.SIGNTYPE); //获得初始化的AlipayClient
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", payNo);
        request.setBizContent(bizContent.toString());
        AlipayTradeQueryResponse response = null;
        try {
            response = alipayClient.execute(request);
            //判断支付结果是否成功
            if (!response.isSuccess()) {
                XueChengPlusException.cast("请求支付查询查询失败");
            }
        } catch (AlipayApiException e) {
            log.error("请求支付宝查询支付结果异常:{}", e.toString(), e);
            XueChengPlusException.cast("请求支付查询支付结果异常");
        }

        //支付宝返回的信息 获取支付结果
        String resultJson = response.getBody();
        //解析支付结果
        //json转map
        Map resultMap = JSON.parseObject(resultJson, Map.class);
        Map alipay_trade_query_response = (Map) resultMap.get("alipay_trade_query_response");
        //支付结果 从map里面去value
        String trade_status = (String) alipay_trade_query_response.get("trade_status");
        String total_amount = (String) alipay_trade_query_response.get("total_amount");
        String trade_no = (String) alipay_trade_query_response.get("trade_no");
        //保存支付结果
        PayStatusDto payStatusDto = new PayStatusDto();
        payStatusDto.setOut_trade_no(payNo);
        payStatusDto.setTrade_status(trade_status);
        payStatusDto.setApp_id(APP_ID);
        payStatusDto.setTrade_no(trade_no);
        payStatusDto.setTotal_amount(total_amount);
        return payStatusDto;
    }

    /**
     * 保存支付宝支付结果
     *
     * @param payStatusDto 支付结果信息 从支付宝查询到的信息
     */
    @Transactional
    @Override
    public void saveAliPayStatus(PayStatusDto payStatusDto) {
        //拿到支付记录号
        String payNo = payStatusDto.getOut_trade_no();
        XcPayRecord payRecord = getPayRecordByPayno(payNo);
        if (payRecord == null) {
            XueChengPlusException.cast("找不到相关的支付记录");
        }
        //从支付宝查询到的支付结果
        String trade_status = payStatusDto.getTrade_status();
        log.debug("收到支付结果:{},支付记录:{}}", payStatusDto.toString(), payRecord.toString());
        if (trade_status.equals("TRADE_SUCCESS")) {

            //支付金额变为分
            Float totalPrice = payRecord.getTotalPrice() * 100;
            Float total_amount = Float.parseFloat(payStatusDto.getTotal_amount()) * 100;
            //校验是否一致
            if (!payStatusDto.getApp_id().equals(APP_ID) || totalPrice.intValue() != total_amount.intValue()) {
                //校验失败
                log.info("校验支付结果失败,支付记录:{},APP_ID:{},totalPrice:{}", payRecord.toString(), payStatusDto.getApp_id(), total_amount.intValue());
                XueChengPlusException.cast("校验支付结果失败");
            }
            log.debug("更新支付结果,支付交易流水号:{},支付结果:{}", payNo, trade_status);
            //更新支付记录表
            XcPayRecord payRecord_u = new XcPayRecord();
            payRecord_u.setStatus("601002");//支付成功
            payRecord_u.setOutPayChannel("Alipay");//第三方支付渠道编号 保存的是支付宝状态
            payRecord_u.setOutPayNo(payStatusDto.getTrade_no());//支付宝订单号
            payRecord_u.setPaySuccessTime(LocalDateTime.now());//支付成功的时间
            //把需要更新的字段 更新在对应的支付记录里面
            int update1 = payRecordMapper.update(payRecord_u, new LambdaQueryWrapper<XcPayRecord>().eq(XcPayRecord::getPayNo, payNo));
            if (update1 > 0) {
                log.info("更新支付记录状态成功:{}", payRecord_u.toString());
            } else {
                log.info("更新支付记录状态失败:{}", payRecord_u.toString());
                XueChengPlusException.cast("更新支付记录状态失败");
            }
            //关联的订单号       更新订单表
            Long orderId = payRecord.getOrderId();
            XcOrders orders = ordersMapper.selectById(orderId);
            if (orders == null) {
                log.info("根据支付记录[{}}]找不到订单", payRecord_u.toString());
                XueChengPlusException.cast("根据支付记录找不到订单");
            }
            XcOrders order_u = new XcOrders();
            order_u.setStatus("600002");//支付成功
            int update = ordersMapper.update(order_u, new LambdaQueryWrapper<XcOrders>().eq(XcOrders::getId, orderId));
            if (update > 0) {
                log.info("更新订单表状态成功,订单号:{}", orderId);
            } else {
                log.info("更新订单表状态失败,订单号:{}", orderId);
                XueChengPlusException.cast("更新订单表状态失败");
            }
            //数据持久化 保存消息记录,参数1：支付结果通知类型(指定消息类型)，
            //放消费者需要的参数
            //                        2: 业务id（选课id，学习中心服务拿到才能更新状态）
            //                        3: 订单类型（因为订单是通用服务，指定订单类型，学习中心拿到后，才知道该消息是它的）
            MqMessage mqMessage = mqMessageService.addMessage("payresult_notify", orders.getOutBusinessId(), orders.getOrderType(), null);
            //通知消息
            notifyPayResult(mqMessage);
        }
    }

    //拿到支付结果后 被调用 发送通知
    @Override
    public void notifyPayResult(MqMessage message) {

        //1、消息对象转json   消息内容
        String msg = JSON.toJSONString(message);
        //创建一个持久化消息         指定字符编码 消息本身要持久化
        Message msgObj = MessageBuilder.withBody(msg.getBytes(StandardCharsets.UTF_8))
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                .build();
        // 2.指定全局唯一的消息ID(消息表自增的主键)，需要封装到CorrelationData中  指定回调方法
        CorrelationData correlationData = new CorrelationData(message.getId().toString());
        // 3.添加callback
        correlationData.getFuture().addCallback(
                result -> {
                    if(result.isAck()){
                        // 3.1.ack，消息成功发送到了交换机
                        log.debug("通知支付结果消息发送成功, ID:{}", correlationData.getId());
                        //删除消息表中的记录   将消息从数据库表删掉
                        mqMessageService.completed(message.getId());
                    }else{
                        // 3.2.nack，消息发送失败  数据依然在 不用操作
                        log.error("通知支付结果消息发送失败, ID:{}, 原因{}",correlationData.getId(), result.getReason());
                    }
                },
                //发送异常了
                ex -> log.error("消息发送异常, ID:{}, 原因{}",correlationData.getId(),ex.getMessage())
        );
        // 发送消息 广播模式（所有都发） 指定交换机 不用路由key  消息本身 确认机制
        rabbitTemplate.convertAndSend(PayNotifyConfig.PAYNOTIFY_EXCHANGE_FANOUT, "", msgObj,correlationData);

    }
}