package com.atguigu.gmall1213.payment.client.impl;

import com.atguigu.gmall1213.model.payment.PaymentInfo;
import com.atguigu.gmall1213.payment.client.PaymentFeignClient;
import org.springframework.stereotype.Component;

@Component
public class PaymentFeignClientImpl implements PaymentFeignClient {
    @Override
    public Boolean closePay(Long orderId) {
        return null;
    }

    @Override
    public Boolean checkPayment(Long orderId) {
        return null;
    }

    @Override
    public PaymentInfo getPaymentInfo(String outTradeNo) {
        return null;
    }
}
