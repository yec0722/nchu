package com.atguigu.gmall1213.payment.controller;

import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.atguigu.gmall1213.common.result.Result;
import com.atguigu.gmall1213.model.enums.PaymentStatus;
import com.atguigu.gmall1213.model.enums.PaymentType;
import com.atguigu.gmall1213.model.payment.PaymentInfo;
import com.atguigu.gmall1213.payment.config.AlipayConfig;
import com.atguigu.gmall1213.payment.service.AlipayService;
import com.atguigu.gmall1213.payment.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/api/payment/alipay")
public class AlipayController {
    @Autowired
    private AlipayService alipayService;
    @Autowired
    private PaymentService paymentService;

    @RequestMapping("/submit/{orderId}")
    @ResponseBody // 将信息直接输出到页面
    public String submitAlipay(@PathVariable Long orderId) {
        String from = "";
        try {
            // 直接调用返回html
            from = alipayService.aliPay(orderId);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }

        return from;
    }

    //同步回调地址 http://api.gmall.com/api/payment/alipay/callback/return
    @RequestMapping("/callback/return")
    public String callbackReturn() {
        //重定向到展示订单的页面
        //当用户获取到回调地址之后，调整到支付成功页面。
        return "redirect:" + AlipayConfig.return_order_url;
    }

    //异步回调地址 http://7fhfqw.natappfree.cc/api/payment/alipay/callback/notify
    @RequestMapping("/callback/notify")
    public String callbackNotify(@RequestParam Map<String, String> paramMap) {
        //paramMap:PaymentInfo 中的callback_content属性值
        System.out.println("异步回调=========================================================================");
       /*
       异步返回结果的验签:   以下这些参数都封装到了paramMap中
        https: //商家网站通知地址?voucher_detail_list=[{"amount":"0.20","merchantContribute":"0.00","name":"5折券",
        "otherContribute":"0.20","type":"ALIPAY_DISCOUNT_VOUCHER","voucherId":"2016101200073002586200003BQ4"}]
        &fund_bill_list=[{"amount":"0.80","fundChannel":"ALIPAYACCOUNT"},{"amount":"0.20","fundChannel":"MDISCOUNT"}]
        &subject=PC网站支付交易&trade_no=2016101221001004580200203978&gmt_create=2016-10-12 21:36:12
        &notify_type=trade_status_sync&total_amount=1.00&out_trade_no=mobile_rdm862016-10-12213600&invoice_amount=0.80
        &seller_id=2088201909970555&notify_time=2016-10-12 21:41:23&trade_status=TRADE_SUCCESS&gmt_payment=2016-10-12 21:37:19
        &receipt_amount=0.80&passback_params=passback_params123&buyer_id=2088102114562585
        &app_id=2016092101248425&notify_id=7676a2e1e4e737cff30015c4b7b55e3kh6& sign_type=RSA2
        &buyer_pay_amount=0.80&sign=***&point_amount=0.00
        */
        //异步通知验签
        // Map<String, String> paramsMap = ... //将异步通知中收到的所有参数都存放到map中
        // boolean signVerified = AlipaySignature.rsaCheckV1(paramsMap, ALIPAY_PUBLIC_KEY, CHARSET, SIGN_TYPE) //调用SDK验证签名
        String tradeStatus = paramMap.get("trade_status");
        String outTradeNo = paramMap.get("out_trade_no");
        boolean signVerified = false;
        try {
            signVerified = AlipaySignature.rsaCheckV1(paramMap, AlipayConfig.alipay_public_key, AlipayConfig.charset, AlipayConfig.sign_type); //调用SDK验证签名
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if (signVerified) {
            // TODO 验签成功后，按照支付结果异步通知中的描述，对支付结果中的业务内容进行二次校验，校验成功后在response中返回success并继续商户自身业务处理，校验失败返回failure
            //在支付宝的业务通知中，只有交易通知状态为 TRADE_SUCCESS 或 TRADE_FINISHED 时，支付宝才会认定为买家付款成功。
            if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                //支付状态判断完以后、还要查一下交易记录  如果交易记录中交易状态是PAID或者CLOSED，那么就不需要验签了  避免重复付款
                //查询交易记录
                PaymentInfo paymentInfo = paymentService.getPaymentInfo(outTradeNo, PaymentType.ALIPAY.name());
                if (paymentInfo.getPaymentStatus().equals(PaymentStatus.PAID.name()) ||
                        paymentInfo.getPaymentStatus().equals(PaymentStatus.ClOSED.name())) {
                    return "failure";
                }
                //用户还需判断通知中的数据out_trade_no 是否是商户自己创建的订单号
                //支付成功 更新交易记录的支付状态
                //传paramMap 是为了更新内容
                paymentService.paySuccess(outTradeNo, PaymentType.ALIPAY.name(), paramMap);
                return "success";
            }
        } else {
            // TODO 验签失败则记录异常日志，并在response中返回failure.
            return "failure";
        }
        return "failure";
    }

    //发起退款
    // http://localhost:8205/api/payment/alipay/refund/158
    @RequestMapping("/refund/{orderId}")
    @ResponseBody
    public Result refund(@PathVariable Long orderId) {
        // 根据orderId 来退款 ,调用服务层方法
        boolean flag = alipayService.refund(orderId);
        return Result.ok(flag);
    }

    // 关闭支付宝交易！
    // localhost:8205/api/payment/alipay/closePay/168
    @GetMapping("/closePay/{orderId}")
    @ResponseBody
    public Boolean closePay(@PathVariable Long orderId) {
        Boolean falg = alipayService.closePay(orderId);
        return falg;
    }

    // 查看是否有交易记录
    //localhost:8205/api/payment/alipay/checkPayment/182
    @RequestMapping("/checkPayment/{orderId}")
    @ResponseBody
    public Boolean checkPayment(@PathVariable Long orderId) {
        // 调用退款接口
        boolean flag = alipayService.checkPayment(orderId);
        return flag;
    }

    /*
       整合关闭过期订单
            1.	通过交易编号，与支付方式查询paymentInfo
            2.	创建service-payment-client 工厂PaymentFeignClient
                    因为：在订单模块中要用到上述三个方法。
            3.	整合关闭过期订单代码
     */
    // 通过OutTradeNo 查询paymentInfo
    @GetMapping("/getPaymentInfo/{outTradeNo}")
    @ResponseBody
    public PaymentInfo getPaymentInfo(@PathVariable String outTradeNo) {
        // 通过交易编号，与支付方式查询paymentInfo
        PaymentInfo paymentInfo = paymentService.getPaymentInfo(outTradeNo, PaymentType.ALIPAY.name());
        if (null != paymentInfo) {
            return paymentInfo;
        }
        return null;
    }
}
