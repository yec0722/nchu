package com.atguigu.gmall1213.activity.controller;

import com.atguigu.gmall1213.activity.service.SeckillGoodsService;
import com.atguigu.gmall1213.activity.util.CacheHelper;
import com.atguigu.gmall1213.activity.util.DateUtil;
import com.atguigu.gmall1213.common.constant.MqConst;
import com.atguigu.gmall1213.common.constant.RedisConst;
import com.atguigu.gmall1213.common.result.Result;
import com.atguigu.gmall1213.common.result.ResultCodeEnum;
import com.atguigu.gmall1213.common.service.RabbitService;
import com.atguigu.gmall1213.common.util.AuthContextHolder;
import com.atguigu.gmall1213.common.util.MD5;
import com.atguigu.gmall1213.model.activity.OrderRecode;
import com.atguigu.gmall1213.model.activity.SeckillGoods;
import com.atguigu.gmall1213.model.activity.UserRecode;
import com.atguigu.gmall1213.model.order.OrderDetail;
import com.atguigu.gmall1213.model.order.OrderInfo;
import com.atguigu.gmall1213.model.product.SkuInfo;
import com.atguigu.gmall1213.model.user.UserAddress;
import com.atguigu.gmall1213.order.client.OrderFeignClient;
import com.atguigu.gmall1213.user.client.UserFeignClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

@RestController
@RequestMapping("/api/activity/seckill")
public class SeckillGoodsController {
    @Autowired
    private SeckillGoodsService seckillGoodsService;

    @Autowired
    private RabbitService rabbitService;

    @Autowired
    private UserFeignClient userFeignClient;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private OrderFeignClient orderFeignClient;

    // 查询所有秒杀商品列表
    @GetMapping("/findAll")
    public Result findAll() {
        List<SeckillGoods> list = seckillGoodsService.findAll();
        return Result.ok(list);
    }

    // 根据秒杀商品Id 查看秒杀商品详情
    @GetMapping("/getSeckillGoods/{skuId}")
    public Result getSeckillGoods(@PathVariable Long skuId) {
        SeckillGoods seckillGoods = seckillGoodsService.getSeckillGoodsBySkuId(skuId);
        return Result.ok(seckillGoods);
    }

    /*
    获取下单码
      控制器： http://api.gmall.com/api/activity/seckill/auth/getSeckillSkuIdStr/31
     */
    @GetMapping("/auth/getSeckillSkuIdStr/{skuId}")
    public Result getSeckillSkuIdStr(@PathVariable Long skuId, HttpServletRequest request) {
        // 怎么生成下单码 使用用户Id 来做MD5加密。加密之后的这个字符串就是下单码
        String userId = AuthContextHolder.getUserId(request);
        // 通过当前商品Id查询到正在参加当前秒杀的这个商品对象，看当前的这个商品是否正在秒杀，如果正在秒杀，则获取下单码，否则不能获取！
        SeckillGoods seckillGoods = seckillGoodsService.getSeckillGoodsBySkuId(skuId);
        if (null != seckillGoods) {
            // 当前商品是否正在参与秒杀
            // 判断当前商品是否正在参与秒杀 ，可以通过时间判断,判断获得下单码的时间是不是正在秒杀时间段内
            Date curTime = new Date();
            // 判断当前系统时间是否在秒杀时间范围内
            if (DateUtil.dateCompare(seckillGoods.getStartTime(), curTime) &&
                    DateUtil.dateCompare(curTime, seckillGoods.getEndTime())) {
                //在秒杀时间段内，生成下单码
                if (StringUtils.isNotEmpty(userId)) {
                    String encrypt = MD5.encrypt(userId);
                    return Result.ok(encrypt);
                }
            }
        }
        return Result.fail().message("获取下单码失败！");
    }

    //秒杀商品下单          this.api_name + '/auth/seckillOrder/' + skuId + '?skuIdStr=' + skuIdStr
    @PostMapping("auth/seckillOrder/{skuId}")
    public Result seckillOrder(@PathVariable Long skuId, HttpServletRequest request) {
        // 获取下单码   下单码：skuIdStr
        String skuIdStr = request.getParameter("skuIdStr");
        // 验证下单码 组成是由userId
        String userId = AuthContextHolder.getUserId(request);
        if (!skuIdStr.equals(MD5.encrypt(userId))) {
            // 下单码没有验证通过！
            // 请求不合法
            return Result.build(null, ResultCodeEnum.SECKILL_ILLEGAL);
        }
        // 验证状态位 看该商品是否是秒杀商品
        // CacheHelper 本质就是HashMap map.put(key,value)  split[0] =skuId split[1] =状态位
        String state = (String) CacheHelper.get(skuId.toString());
        if (StringUtils.isEmpty(state)) {
            // 请求不合法
            return Result.build(null, ResultCodeEnum.SECKILL_ILLEGAL);
        }
        // 表示能够下单。
        if ("1".equals(state)) {
            //记录用户 秒杀的哪个商品！
            UserRecode userRecode = new UserRecode();
            userRecode.setUserId(userId);
            userRecode.setSkuId(skuId);
            // 将信息放到消息队列  准备下单
            rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_SECKILL_USER, MqConst.ROUTING_SECKILL_USER, userRecode);
        } else {
            // state=="0"   表示没有商品了！
            return Result.build(null, ResultCodeEnum.SECKILL_ILLEGAL);
        }
        return Result.ok();
    }

    // 根据商品id与用户ID查看秒杀订单信息
    @GetMapping("/auth/checkOrder/{skuId}")
    public Result checkOrder(@PathVariable Long skuId, HttpServletRequest request) {
        // 获取用户Id
        String userId = AuthContextHolder.getUserId(request);
        // 调用服务层检查订单方法
        Result result = seckillGoodsService.checkOrder(skuId, userId);
        return result;
    }


    // 秒杀下单数据控制器
    @GetMapping("auth/trade")
    public Result trade(HttpServletRequest request) {
        // 获取用户的收货地址列表。
        String userId = AuthContextHolder.getUserId(request);
        // 调用userFeignClient
        List<UserAddress> userAddressList = userFeignClient.findUserAddressListByUserId(userId);

        // 显示送货清单，本质就是秒杀的商品
        OrderRecode orderRecode = (OrderRecode) redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS).get(userId);
        // 判断
        if (null == orderRecode) {
            return Result.fail().message("非法操作！下单失败！");
        }
        // 获取用户秒杀的商品
        SeckillGoods seckillGoods = orderRecode.getSeckillGoods();
        // 要给数据赋值：显示送货清单OrderDetial.
        List<OrderDetail> orderDetailList = new ArrayList<>();
        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setSkuId(seckillGoods.getSkuId());
        orderDetail.setSkuName(seckillGoods.getSkuName());
        orderDetail.setImgUrl(seckillGoods.getSkuDefaultImg());
        // 秒杀商品的数量     orderRecode.setNum(1);
        orderDetail.setSkuNum(orderRecode.getNum());
        // 给的秒杀价格
        orderDetail.setOrderPrice(seckillGoods.getCostPrice());
        orderDetailList.add(orderDetail);
        // 订单的总金额
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderDetailList(orderDetailList);
        orderInfo.sumTotalAmount();
        // 声明一个map 集合将数据分别存储起来！
        // 因为trade.html 订单页面需要这些key。
        Map<String, Object> map = new HashMap<>();
        // 存储订单明细
        map.put("detailArrayList", orderDetailList);
        // 存储收货地址列表
        map.put("userAddressList", userAddressList);
        // 总金额
        map.put("totalAmount", orderInfo.getTotalAmount());
        return Result.ok(map);
    }

    // 秒杀提交订单
    @PostMapping("auth/submitOrder")
    public Result submitOrder(@RequestBody OrderInfo orderInfo, HttpServletRequest request) {
        // 获取用户Id
        String userId = AuthContextHolder.getUserId(request);
        orderInfo.setUserId(Long.parseLong(userId));
        // 数据都在缓存中
        OrderRecode orderRecode = (OrderRecode) redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS).get(userId);
        if (null == orderRecode) {
            return Result.fail().message("非法操作。。。。。");
        }
        // 调用订单服务中的方法
        Long orderId = orderFeignClient.submitOrder(orderInfo);
        if (null == orderId) {
            return Result.fail().message("下订单失败。。。。。");
        }
        // 删除下单的信息
        redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS).delete(userId);
        // 保存一个下单记录
        redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS_USERS).put(userId, orderId.toString());
        // 返回数据
        return Result.ok(orderId);
    }


}
