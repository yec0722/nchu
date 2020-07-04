package com.atguigu.gmall1213.payment.service;

import com.alipay.api.AlipayApiException;


public interface AlipayService {

    // 支付宝下单支付接口
    String aliPay(Long orderId) throws AlipayApiException;

    //发起退款
    boolean refund(Long orderId);

    //关闭支付宝
    Boolean closePay(Long orderId);

    //根据订单查询是否支付成功！
    Boolean checkPayment(Long orderId);
}