package com.atguigu.gmall1213.activity.receiver;

import com.atguigu.gmall1213.activity.mapper.SeckillGoodsMapper;
import com.atguigu.gmall1213.activity.service.SeckillGoodsService;
import com.atguigu.gmall1213.activity.util.DateUtil;
import com.atguigu.gmall1213.common.constant.MqConst;
import com.atguigu.gmall1213.common.constant.RedisConst;
import com.atguigu.gmall1213.model.activity.SeckillGoods;
import com.atguigu.gmall1213.model.activity.UserRecode;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;

/*
监听定时任务发送过来的消息    目的是为了将数据库中的秒杀商品放入缓存
 */
@Component
public class SeckillReceiver {

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private SeckillGoodsService seckillGoodsService;

    @SneakyThrows
    @RabbitListener(bindings =
    @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_TASK_1),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_TASK),
            key = {MqConst.ROUTING_TASK_1}
    ))
    public void importData(Message message, Channel channel) {
        //查询数据库中的秒杀商品，并将其查出来后放入缓存
        //秒杀商品：内存中状态位为"1"
        QueryWrapper<SeckillGoods> seckillGoodsQueryWrapper = new QueryWrapper<>();
        seckillGoodsQueryWrapper.eq("status", 1);
        //要保证参加秒杀的商品库存一定要大于0
        seckillGoodsQueryWrapper.gt("stock_count", 0);
        //查询当天的秒杀商品    DATE_FORMAT:对时间的格式做一个格式化
        seckillGoodsQueryWrapper.eq("DATE_FORMAT(start_time,'%Y-%m-%d')", DateUtil.formatDate(new Date()));
        List<SeckillGoods> list = seckillGoodsMapper.selectList(seckillGoodsQueryWrapper);
        if (!CollectionUtils.isEmpty(list)) {
            //将获取到的秒杀商品放入缓存
            for (SeckillGoods seckillGoods : list) {
                //判断缓存中是否已经存在当前秒杀商品的key,如果存在，跳过，继续下一个判断
                if (redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).hasKey(seckillGoods.getSkuId().toString())) {
                    //说明缓存中已经存在该秒杀商品
                    continue;
                }
                //缓存中不存在当前秒杀商品   将其放入缓存
                redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).put(seckillGoods.getSkuId().toString(), seckillGoods);
                //如何控制库存不超卖，将秒杀商品的数量放入redis的list这个数据类型中，lpush,pop具有原子性
                //即将秒杀商品放入队列中，排队，用户一个一个取   redis是单线程的，一次只能操作一个
                for (Integer i = 0; i < seckillGoods.getStockCount(); i++) {
                    /*往队列中放入数据
                    key=seckill:stock:skuId
                    value=skuId
                     */
                    //用户进入，然后出队，队列中商品数量为空了，就说明商品卖完了
                    redisTemplate.boundListOps(RedisConst.SECKILL_STOCK_PREFIX + seckillGoods.getSkuId().toString()).leftPush(seckillGoods.getSkuId().toString());
                }
                //利用redis的发布与订阅  实现内存中状态位在集群中的统一更新
                /*
                channel:表示要发送的频道
                message:表示发送的内容  skuId:1---表示这个商品可以秒杀    skuId:0---不可以秒杀
                 */
                //商品能放入缓存，初始值就是能秒杀       seckillpush：订阅主题
                redisTemplate.convertAndSend("seckillpush", seckillGoods.getSkuId() + ":1");
            }
            //手动确认
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        }
    }

    // 监听秒杀下单时发送过来的消息
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_SECKILL_USER, durable = "true"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_SECKILL_USER),
            key = {MqConst.ROUTING_SECKILL_USER}
    ))
    public void seckill(UserRecode userRecode, Message message, Channel channel) {
        // 判断
        if (null != userRecode) {
            // 预下单   看看是不是能秒到
            seckillGoodsService.seckillOrder(userRecode.getSkuId(), userRecode.getUserId());
            // 消息确认
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        }
    }

    // 每天定时清空缓存数据
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_TASK_18),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_TASK),
            key = {MqConst.ROUTING_TASK_18}
    ))
    public void clearRedisData(Message message, Channel channel) {
        // 获取活动结束的商品
        QueryWrapper<SeckillGoods> seckillGoodsQueryWrapper = new QueryWrapper<>();
        seckillGoodsQueryWrapper.eq("status", 1).le("end_time", new Date());
        List<SeckillGoods> seckillGoodsList = seckillGoodsMapper.selectList(seckillGoodsQueryWrapper);

        // 清空缓存
        for (SeckillGoods seckillGoods : seckillGoodsList) {
            // 商品库存数量
            redisTemplate.delete(RedisConst.SECKILL_STOCK_PREFIX + seckillGoods.getSkuId());
        }
        redisTemplate.delete(RedisConst.SECKILL_GOODS);
        redisTemplate.delete(RedisConst.SECKILL_ORDERS);
        redisTemplate.delete(RedisConst.SECKILL_ORDERS_USERS);
        // 将审核状态更新一下
        SeckillGoods seckillGoods = new SeckillGoods();
        seckillGoods.setStatus("2");
        seckillGoodsMapper.update(seckillGoods, seckillGoodsQueryWrapper);
        // 手动确认
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }
}
