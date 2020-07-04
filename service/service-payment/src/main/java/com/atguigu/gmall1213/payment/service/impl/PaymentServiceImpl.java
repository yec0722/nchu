package com.atguigu.gmall1213.payment.service.impl;

import com.atguigu.gmall1213.common.constant.MqConst;
import com.atguigu.gmall1213.common.service.RabbitService;
import com.atguigu.gmall1213.model.enums.PaymentStatus;
import com.atguigu.gmall1213.model.order.OrderInfo;
import com.atguigu.gmall1213.model.payment.PaymentInfo;
import com.atguigu.gmall1213.payment.mapper.PaymentInfoMapper;
import com.atguigu.gmall1213.payment.service.PaymentService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;

@Service
public class PaymentServiceImpl implements PaymentService {
    @Autowired
    private PaymentInfoMapper paymentInfoMapper;
    @Autowired
    private RabbitService rabbitService;

    // 保存支付记录
    @Override
    public void savePaymentInfo(String paymentType, OrderInfo orderInfo) {
        // 交易记录中如果有当前对应的订单Id 时，那么还能否继续插入当前数据。
        //先查一下交易记录中是否有当前对应的订单Id
        QueryWrapper<PaymentInfo> paymentInfoQueryWrapper = new QueryWrapper<>();
        paymentInfoQueryWrapper.eq("order_id", orderInfo.getId());
        //同一个订单id支付类型可能是微信，也可能是支付宝
        paymentInfoQueryWrapper.eq("payment_type", paymentType);
        Integer count = paymentInfoMapper.selectCount(paymentInfoQueryWrapper);
        if (count > 0) {
            //就不走下面代码了
            return;
        }
        PaymentInfo paymentInfo = new PaymentInfo();
        //对象赋值
        paymentInfo.setCreateTime(new Date());
        paymentInfo.setOrderId(orderInfo.getId());
        paymentInfo.setPaymentType(paymentType);
        paymentInfo.setOutTradeNo(orderInfo.getOutTradeNo());
        paymentInfo.setPaymentStatus(PaymentStatus.UNPAID.name());
        paymentInfo.setSubject(orderInfo.getTradeBody());
        paymentInfo.setTotalAmount(orderInfo.getTotalAmount());
        // 保存支付记录
        paymentInfoMapper.insert(paymentInfo);
    }

    //查询交易记录
    @Override
    public PaymentInfo getPaymentInfo(String outTradeNo, String name) {
        //name: 支付方式
        QueryWrapper<PaymentInfo> paymentInfoQueryWrapper = new QueryWrapper<>();
        paymentInfoQueryWrapper.eq("out_trade_no", outTradeNo);
        paymentInfoQueryWrapper.eq("payment_type", name);
        PaymentInfo paymentInfo = paymentInfoMapper.selectOne(paymentInfoQueryWrapper);
        return paymentInfo;
    }

    //支付成功 更新交易记录的支付状态
    @Override
    public void paySuccess(String outTradeNo, String name, Map<String, String> paramMap) {
        PaymentInfo paymentInfo = this.getPaymentInfo(outTradeNo, name);
        // 如果当前订单交易记录 已经是付款完成的，或者是交易关闭的。则后续业务不会执行！
        if (paymentInfo.getPaymentStatus().equals(PaymentStatus.PAID.name()) || paymentInfo.getPaymentStatus().equals(PaymentStatus.ClOSED.name())) {
            return;
        }
        // 第一个参数更新的内容，第二个参数更新的条件
        PaymentInfo paymentInfoUPD = new PaymentInfo();
        paymentInfoUPD.setPaymentStatus(PaymentStatus.PAID.name());
        paymentInfoUPD.setCallbackTime(new Date());
        // 更新支付宝的交易号，交易号在map 中
        paymentInfoUPD.setTradeNo(paramMap.get("trade_no"));
        paymentInfoUPD.setCallbackContent(paramMap.toString());

        // 构造更新条件
        // update payment_info set trade_no = ？，payment_status=？ ... where out_trade_no = outTradeNo and payment_type = name
        QueryWrapper<PaymentInfo> paymentInfoQueryWrapper = new QueryWrapper<>();
        paymentInfoQueryWrapper.eq("out_trade_no", outTradeNo).eq("payment_type", name);
        paymentInfoMapper.update(paymentInfoUPD, paymentInfoQueryWrapper);
        //发送消息通知订单更新订单状态
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_PAYMENT_PAY,
                MqConst.ROUTING_PAYMENT_PAY, paymentInfo.getOrderId());
    }

    //根据out_trade_no更新退款后交易的关闭状态
    @Override
    public void updatePaymentInfo(String outTradeNo, PaymentInfo paymentInfo) {
        QueryWrapper<PaymentInfo> paymentInfoQueryWrapper = new QueryWrapper<>();
        paymentInfoQueryWrapper.eq("out_trade_no", outTradeNo);
        paymentInfoMapper.update(paymentInfo, paymentInfoQueryWrapper);
    }

    //关闭支付宝交易
    @Override
    public void closePayment(Long orderId) {
        // 先查询 paymentInfo 是否有对应的交易记录 select count(*) from payment_info where order_id = orderId
        QueryWrapper<PaymentInfo> paymentInfoQueryWrapper = new QueryWrapper<>();
        paymentInfoQueryWrapper.eq("order_id", orderId);
        Integer count = paymentInfoMapper.selectCount(paymentInfoQueryWrapper);
        if (null == count || count.intValue() == 0) {
            //说明paymentInfo没有该交易记录    说明还未支付  不用走下面的逻辑了
            return;
        }
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setPaymentStatus(PaymentStatus.ClOSED.name());
        //直接操作数据库
        paymentInfoMapper.update(paymentInfo, paymentInfoQueryWrapper);
    }
}
