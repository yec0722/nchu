package com.atguigu.gmall1213.payment.service.impl;

import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradeCloseRequest;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeCloseResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.atguigu.gmall1213.model.enums.PaymentStatus;
import com.atguigu.gmall1213.model.enums.PaymentType;
import com.atguigu.gmall1213.model.order.OrderInfo;
import com.atguigu.gmall1213.model.payment.PaymentInfo;
import com.atguigu.gmall1213.order.client.OrderFeignClient;
import com.atguigu.gmall1213.payment.config.AlipayConfig;
import com.atguigu.gmall1213.payment.service.AlipayService;
import com.atguigu.gmall1213.payment.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;
import springfox.documentation.spring.web.json.Json;

import java.util.HashMap;

@Service
public class AlipayServiceImpl implements AlipayService {

    @Autowired
    //保存交易支付记录
    private PaymentService paymentService;

    @Autowired
    private OrderFeignClient orderFeignClient;

    @Autowired
    private AlipayClient alipayClient;

    // 支付宝下单支付接口（官网接口）
    @Override
    public String aliPay(Long orderId) throws AlipayApiException {
        // 获取订单对象
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderId);
        // 保存交易记录
        paymentService.savePaymentInfo(PaymentType.ALIPAY.name(), orderInfo);
        // 生成二维码
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
        // 同步回调
        alipayRequest.setReturnUrl(AlipayConfig.return_payment_url);
        // 异步回调
        alipayRequest.setNotifyUrl(AlipayConfig.notify_payment_url); //在公共参数中设置回跳和通知地址
        // 声明一个集合
        HashMap<String, Object> map = new HashMap<>();
        map.put("out_trade_no", orderInfo.getOutTradeNo());
        map.put("product_code", "FAST_INSTANT_TRADE_PAY");
        map.put("total_amount", orderInfo.getTotalAmount());
        map.put("subject", "买空调----");
        // 将map 转换为json字符串即可
        alipayRequest.setBizContent(JSON.toJSONString(map));
        // 直接将完整的表单html返回
        return alipayClient.pageExecute(alipayRequest).getBody();
    }

    //退款功能(官网接口)
    @Override
    /*AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do","app_id","your private_key","json","GBK","alipay_public_key","RSA2");
    AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
    request.setBizContent("{" +
        "\"out_trade_no\":\"20150320010101001\"," +
        "\"trade_no\":\"2014112611001004680073956707\"," +
        "\"refund_amount\":200.12," +
        "\"refund_currency\":\"USD\"," +
        "\"refund_reason\":\"正常退款\"," +
        "\"out_request_no\":\"HZ01RF001\"," +
        "\"operator_id\":\"OP001\"," +
        "\"store_id\":\"NJ_S_001\"," +
        "\"terminal_id\":\"NJ_T_001\"," +
        "      \"goods_detail\":[{" +
        "        \"goods_id\":\"apple-01\"," +
        "\"alipay_goods_id\":\"20010001\"," +
        "\"goods_name\":\"ipad\"," +
        "\"quantity\":1," +
        "\"price\":2000," +
        "\"goods_category\":\"34543238\"," +
        "\"categories_tree\":\"124868003|126232002|126252004\"," +
        "\"body\":\"特价手机\"," +
        "\"show_url\":\"http://www.alipay.com/xxx.jpg\"" +
        "        }]," +
        "      \"refund_royalty_parameters\":[{" +
        "        \"royalty_type\":\"transfer\"," +
        "\"trans_out\":\"2088101126765726\"," +
        "\"trans_out_type\":\"userId\"," +
        "\"trans_in_type\":\"userId\"," +
        "\"trans_in\":\"2088101126708402\"," +
        "\"amount\":0.1," +
        "\"amount_percentage\":100," +
        "\"desc\":\"分账给2088101126708402\"" +
        "        }]," +
        "\"org_pid\":\"2088101117952222\"," +
        "      \"query_options\":[" +
        "        \"refund_detail_item_list\"" +
        "      ]" +
        "  }");
     AlipayTradeRefundResponse response = alipayClient.execute(request);
     if(response.isSuccess()){
        System.out.println("调用成功");
       } else {
        System.out.println("调用失败");
     }
     */
    public boolean refund(Long orderId) {
        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
        //out_trade_no:订单支付时传入的商户订单号,不能和 trade_no同时为空。
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderId);
        // 声明一个map 集合来存储数据
        HashMap<String, Object> map = new HashMap<>();
        map.put("out_trade_no", orderInfo.getOutTradeNo());
        map.put("refund_amount", orderInfo.getTotalAmount());
        map.put("refund_reason", "空调不凉快！");
        request.setBizContent(JSON.toJSONString(map));
        AlipayTradeRefundResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if (response.isSuccess()) {
            //退款成功，关闭交易
            PaymentInfo paymentInfo = new PaymentInfo();
            paymentInfo.setPaymentStatus(PaymentStatus.ClOSED.name());
            //根据out_trade_no更新
            paymentService.updatePaymentInfo(orderInfo.getOutTradeNo(), paymentInfo);
            System.out.println("调用成功");
            return true;
        } else {
            System.out.println("调用失败");
            return false;
        }
    }

    //关闭支付宝(官方接口)
    @Override
    public Boolean closePay(Long orderId) {
        // 关闭支付宝交易 https://opendocs.alipay.com/apis/api_1/alipay.trade.close
        // AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do","app_id","your private_key","json","GBK","alipay_public_key","RSA2");
        AlipayTradeCloseRequest request = new AlipayTradeCloseRequest();
        // out_trade_no 是orderInfo中的OutTradeNo 也是PaymentInfo中OutTradeNo
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderId);
        // 创建一个Map
        HashMap<String, Object> map = new HashMap<>();
        map.put("out_trade_no", orderInfo.getOutTradeNo());
        map.put("operator_id", "YX01");
        request.setBizContent(JSON.toJSONString(map));
        AlipayTradeCloseResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if (response.isSuccess()) {
            System.out.println("调用成功");
            return true;
        } else {
            System.out.println("调用失败");
            return false;
        }
    }

    //根据订单查询是否支付成功！(官方接口)
    @Override
    public Boolean checkPayment(Long orderId) {
        // AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do","app_id","your private_key","json","GBK","alipay_public_key","RSA2");
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        // out_trade_no 是orderInfo中的OutTradeNo 也是PaymentInfo中OutTradeNo
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderId);
        HashMap<String, Object> map = new HashMap<>();
        map.put("out_trade_no", orderInfo.getOutTradeNo());
        request.setBizContent(JSON.toJSONString(map));
        // 准备执行
        AlipayTradeQueryResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if (response.isSuccess()) {
            System.out.println("调用成功");
            return true;
        } else {
            System.out.println("调用失败");
            return false;
        }
    }
}
