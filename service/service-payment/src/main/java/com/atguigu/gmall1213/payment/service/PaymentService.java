package com.atguigu.gmall1213.payment.service;

import com.atguigu.gmall1213.model.order.OrderInfo;
import com.atguigu.gmall1213.model.payment.PaymentInfo;

import java.util.Map;

public interface PaymentService {
    // 保存支付记录 数据来源是orderInfo
    //paymentType:支付类型
    void savePaymentInfo(String paymentType, OrderInfo orderInfo);

    //查询交易记录
    PaymentInfo getPaymentInfo(String outTradeNo, String name);

    //支付成功 更新交易记录的支付状态
    void paySuccess(String outTradeNo, String name, Map<String, String> paramMap);

    //根据out_trade_no更新退款后交易的关闭状态
    void updatePaymentInfo(String outTradeNo, PaymentInfo paymentInfo);

    //关闭支付宝交易
    void closePayment(Long orderId);
}
