package com.atguigu.gmall1213.payment.client;

import com.atguigu.gmall1213.model.payment.PaymentInfo;
import com.atguigu.gmall1213.payment.client.impl.PaymentFeignClientImpl;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

@FeignClient(value = "service-payment",fallback = PaymentFeignClientImpl.class)
public interface PaymentFeignClient {

    /**
     * 关闭支付宝交易
     * @param orderId
     * @return
     */
    @GetMapping("api/payment/alipay/closePay/{orderId}")
    Boolean closePay(@PathVariable Long orderId);

    /**
     * 检查是否有交易记录
     * @param orderId
     * @return
     */
    @GetMapping("api/payment/alipay/checkPayment/{orderId}")
    Boolean checkPayment(@PathVariable Long orderId);

    /**
     * 查询paymentInfo对象的
     * @param outTradeNo
     * @return
     */
    @GetMapping("api/payment/alipay/getPaymentInfo/{outTradeNo}")
    PaymentInfo getPaymentInfo(@PathVariable String outTradeNo);

}
