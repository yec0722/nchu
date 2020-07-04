package com.atguigu.gmall1213.order.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall1213.common.constant.MqConst;
import com.atguigu.gmall1213.common.service.RabbitService;
import com.atguigu.gmall1213.common.util.HttpClientUtil;
import com.atguigu.gmall1213.model.enums.OrderStatus;
import com.atguigu.gmall1213.model.enums.ProcessStatus;
import com.atguigu.gmall1213.model.order.OrderDetail;
import com.atguigu.gmall1213.model.order.OrderInfo;
import com.atguigu.gmall1213.order.mapper.OrderDetailMapper;
import com.atguigu.gmall1213.order.mapper.OrderInfoMapper;
import com.atguigu.gmall1213.order.service.OrderService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderService {

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RabbitService rabbitService;

    @Value("${ware.url}")
    private String wareUrl;

    //提交订单
    @Override
    @Transactional
    public Long saveOrderInfo(OrderInfo orderInfo) {
        //总金额
        orderInfo.sumTotalAmount();
        //用户id
        //订单交易编号
        String outTradeNo = "YEC" + System.currentTimeMillis() + new Random().nextInt(1000);
        System.out.println(System.currentTimeMillis());
        orderInfo.setOutTradeNo(outTradeNo);
        //根据订单明细中的商品名称进行拼接
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        StringBuffer sb = new StringBuffer();
        for (OrderDetail orderDetail : orderDetailList) {
            sb.append(orderDetail.getSkuName() + " ");
        }
        if (sb.toString().length() > 100) {
            //商品描述
            orderInfo.setTradeBody(sb.toString().substring(0, 100));
        } else {
            orderInfo.setTradeBody(sb.toString());
        }
        //订单状态
        orderInfo.setOrderStatus(OrderStatus.UNPAID.name());
        //创建时间
        orderInfo.setCreateTime(new Date());
        //过期时间    默认是一天时间后过期
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, 1);
        orderInfo.setExpireTime(calendar.getTime());
        //订单进度状态  目前是未支付
        orderInfo.setProcessStatus(ProcessStatus.UNPAID.name());

        orderInfoMapper.insert(orderInfo);
        if (!CollectionUtils.isEmpty(orderDetailList)) {
            for (OrderDetail orderDetail : orderDetailList) {
                orderDetail.setOrderId(orderInfo.getId());
                orderDetailMapper.insert(orderDetail);
            }
        }
        //订单保存完成之后发送延迟取消订单消息   取消订单应该根据订单id取消
        rabbitService.sendDelayMessage(MqConst.EXCHANGE_DIRECT_ORDER_CANCEL,
                MqConst.ROUTING_ORDER_CANCEL, orderInfo.getId(), MqConst.DELAY_TIME);
        //返回订单编号
        return orderInfo.getId();
    }

    //获取流水号
    @Override
    public String getTradeNo(String userId) {
        //定义一个缓存key
        String tradeNoKey = "user:" + userId + ":tradeNo";
        // 定义一个流水号
        String tradeNo = UUID.randomUUID().toString();
        //将流水号放入缓存
        redisTemplate.opsForValue().set(tradeNoKey, tradeNo);
        return tradeNo;
    }

    //比较流水号   tradeNo:页面提交过来的流水号
    @Override
    public boolean checkTradeNo(String userId, String tradeNo) {
        //获取从页面传过来的流水号
        String tradeNoKey = "user:" + userId + ":tradeNo";
        String tradeNoRedis = (String) redisTemplate.opsForValue().get(tradeNoKey);
        return tradeNo.equals(tradeNoRedis);
    }

    //删除流水号
    @Override
    public void deleteTradeNo(String userId) {
        String tradeNoKey = "user:" + userId + ":tradeNo";
        redisTemplate.delete(tradeNoKey);
    }

    //验证库存
    @Override
    public boolean checkStock(Long skuId, Integer skuNum) {
        // 远程调用      http://localhost:9001/hasStock?skuId=xxx&num=xxx
        String result = HttpClientUtil.doGet(wareUrl + "/hasStock?skuId=" + skuId + "&num=" + skuNum);
        //0:无货     1:有货
        return "1".equals(result);
    }

    //关闭过期订单
    @Override
    public void execExpiredOrder(Long orderId) {
        //更新数据库中orderInfo表的状态
        // update order_info set order_status=CLOSED , process_status=CLOSED where id=orderId
        updateOrderStatus(orderId, ProcessStatus.CLOSED);
        // 发送信息关闭支付宝交易
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_PAYMENT_CLOSE, MqConst.ROUTING_PAYMENT_CLOSE, orderId);
    }

    //关闭过期订单后，从数据库中更新订单状态
    public void updateOrderStatus(Long orderId, ProcessStatus processStatus) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(orderId);
        // 订单的状态，可以通过进度状态来获取
        orderInfo.setOrderStatus(processStatus.getOrderStatus().name());
        orderInfo.setProcessStatus(processStatus.name());
        orderInfoMapper.updateById(orderInfo);
    }

    //根据订单Id 查询订单对象   这个方法可以在里面查询订单明细
    @Override
    public OrderInfo getOrderInfo(Long orderId) {
        OrderInfo orderInfo = orderInfoMapper.selectById(orderId);
        QueryWrapper<OrderDetail> orderDetailQueryWrapper = new QueryWrapper<>();
        orderDetailQueryWrapper.eq("order_id", orderId);
        List<OrderDetail> orderDetails = orderDetailMapper.selectList(orderDetailQueryWrapper);
        orderInfo.setOrderDetailList(orderDetails);
        return orderInfo;
    }

    // 支付成功之后发送消息通知库存，准备减库存！
    @Override
    public void sendOrderStatus(Long orderId) {
        // 更改订单的状态，通知仓库准备发货
        updateOrderStatus(orderId, ProcessStatus.NOTIFIED_WARE);
        // 需要参考库存管理文档 根据管理手册。
        // 发送的数据 是 orderInfo 中的部分属性数据，并非全部属性数据！
        String wareJson = initWareOrder(orderId);
        // 准备发送消息
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_WARE_STOCK, MqConst.ROUTING_WARE_STOCK, wareJson);
    }

    //将map集合转为JSON
    public String initWareOrder(Long orderId) {
        // 要先查询到orderInfo
        OrderInfo orderInfo = getOrderInfo(orderId);
        //将orderInfo 中的部分属性，放入一个map 集合中
        Map map = initWareOrder(orderInfo);
        //返回json字符串
        return JSON.toJSONString(map);
    }

    // 将orderInfo 部分数据组成map
    public Map initWareOrder(OrderInfo orderInfo) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("orderId", orderInfo.getId());
        map.put("consignee", orderInfo.getConsignee());
        map.put("consigneeTel", orderInfo.getConsigneeTel());
        map.put("orderComment", orderInfo.getOrderComment());
        map.put("orderBody", orderInfo.getTradeBody());
        map.put("deliveryAddress", orderInfo.getDeliveryAddress());
        //paymentWay-------------------支付方式：  ‘1’ 为货到付款，‘2’为在线支付。
        map.put("paymentWay", "2");
        map.put("wareId", orderInfo.getWareId());// 仓库Id ，减库存拆单时需要使用！
        /*
            details 对应的是订单明细
            details:[{skuId:101,skuNum:1,skuName:’小米手64G’},
                       {skuId:201,skuNum:1,skuName:’索尼耳机’}]
         */
        // 声明一个list 集合 来存储map
        List<Map> maps = new ArrayList<>();
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        if (!CollectionUtils.isEmpty(orderDetailList)) {
            for (OrderDetail orderDetail : orderDetailList) {
                // 先声明一个map 集合
                HashMap<String, Object> orderDetailMap = new HashMap<>();
                orderDetailMap.put("skuId", orderDetail.getSkuId());
                orderDetailMap.put("skuNum", orderDetail.getSkuNum());
                orderDetailMap.put("skuName", orderDetail.getSkuName());
                maps.add(orderDetailMap);
            }
        }
        //消息数据类型
        map.put("details", maps);
        return map;
    }

    //原始订单拆分后就能得到子订单集合   拆单
    @Override
    public List<OrderInfo> orderSplit(Long orderId, String wareSkuMap) {
        //创建一个装子订单的集合
        List<OrderInfo> subOrderInfoList = new ArrayList<>();
        //先获取原始订单
        OrderInfo orderInfoOrigin = getOrderInfo(orderId);
        //存储的数据是:wareSkuMap    格式是：[{"wareId":"1","skuIds":["2","10"]},{"wareId":"2","skuIds":["3"]}]
        List<Map> mapList = JSON.parseArray(wareSkuMap, Map.class);
        // 子订单根据什么来创建
        for (Map map : mapList) {
            // 获取map 中的仓库Id
            String wareId = (String) map.get("wareId");
            // 获取子订单中仓库Id 对应的商品 Id
            List<String> skuIdList = (List<String>) map.get("skuIds");
            //创建新的子订单
            OrderInfo subOrderInfo = new OrderInfo();
            // 属性拷贝，原始订单的基本数据，都可以给子订单使用
            BeanUtils.copyProperties(orderInfoOrigin, subOrderInfo);
            // id 不能拷贝，会发生主键冲突
            subOrderInfo.setId(null);
            subOrderInfo.setParentOrderId(orderId);
            // 赋值一个仓库Id
            subOrderInfo.setWareId(wareId);
            /*
            计算总金额 在订单的实体类中有sumTotalAmount() 方法
             */
            // 声明一个子订单明细集合
            List<OrderDetail> orderDetails = new ArrayList<>();
            // 需要将子订单的名单明细准备好,添加到子订单中
            // 子订单明细应该来自于原始订单明细。
            List<OrderDetail> orderDetailList = orderInfoOrigin.getOrderDetailList();
            if (!CollectionUtils.isEmpty(orderDetailList)) {
                // 遍历原始的订单明细
                for (OrderDetail orderDetail : orderDetailList) {
                    // 遍历仓库中所对应的商品Id
                    for (String skuId : skuIdList) {
                        // 比较两个商品skuId ，如果相同，则这个商品就是子订单明细需要的商品
                        if (Long.parseLong(skuId) == orderDetail.getSkuId()) {
                            orderDetails.add(orderDetail);
                        }
                    }
                }
            }
            //将子订单的订单明细添加到子订单中
            subOrderInfo.setOrderDetailList(orderDetails);
            // 获取到子订单的总金额
            subOrderInfo.sumTotalAmount();
            //保存子订单
            saveOrderInfo(subOrderInfo);
            // 将新的子订单放入集合中
            subOrderInfoList.add(subOrderInfo);
        }
        //更新原始订单的状态为spilt
        updateOrderStatus(orderId, ProcessStatus.SPLIT);
        return subOrderInfoList;
    }


    @Override
    public void execExpiredOrder(Long orderId, String flag) {
        // 更新订单状态状态
        updateOrderStatus(orderId, ProcessStatus.CLOSED);
        if ("2".equals(flag)) {
            // 发送信息关闭支付宝交易
            rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_PAYMENT_CLOSE, MqConst.ROUTING_PAYMENT_CLOSE, orderId);
        }
    }
}
