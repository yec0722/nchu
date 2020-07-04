package com.atguigu.gmall1213.activity.service.impl;

import com.atguigu.gmall1213.activity.mapper.SeckillGoodsMapper;
import com.atguigu.gmall1213.activity.service.SeckillGoodsService;
import com.atguigu.gmall1213.activity.util.CacheHelper;
import com.atguigu.gmall1213.common.constant.RedisConst;
import com.atguigu.gmall1213.common.result.Result;
import com.atguigu.gmall1213.common.result.ResultCodeEnum;
import com.atguigu.gmall1213.common.util.MD5;
import com.atguigu.gmall1213.model.activity.OrderRecode;
import com.atguigu.gmall1213.model.activity.SeckillGoods;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class SeckillGoodsServiceImpl implements SeckillGoodsService {
    // 因为秒杀商品在凌晨会将数据加载到缓存中，所以此处查询缓存即可
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;

    // 查询所有秒杀商品列表
    @Override
    public List<SeckillGoods> findAll() {
        // 因为商品保存到缓存Hash   所以直接从缓存中拿取秒杀商品
        //key->{skuId,value}
        List<SeckillGoods> seckillGoodsList = redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).values();
        return seckillGoodsList;
    }

    // 根据秒杀商品Id 查看秒杀商品详情
    @Override
    public SeckillGoods getSeckillGoodsBySkuId(Long skuId) {
        SeckillGoods seckillGoods = (SeckillGoods) redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).get(skuId.toString());
        return seckillGoods;
    }

    //预下单
    @Override
    public void seckillOrder(Long skuId, String userId) {
        // 判断状态位
        String state = (String) CacheHelper.get(skuId.toString());
        // 商品已经售罄
        if ("0".equals(state)) {
            return;
        }
        // 判断用户是否已经下单 ，如何防止用户重复下单 setnx key不存在才生效
        // 如果用户下单成功，我们会将用户下单信息放入缓存,并设置一个过期时间     key = seckill:user:userId value = skuId
        //redisTemplate.opsForValue().setIfAbsent  等价于setnx()
        Boolean isExist = redisTemplate.opsForValue().setIfAbsent(RedisConst.SECKILL_USER + userId, skuId, RedisConst.SECKILL__TIMEOUT, TimeUnit.SECONDS);
        // 继续判断 false ：表示用户已经存在，说明用户第二次下单
        if (!isExist) {
            return;
        }
        // isExist = true 表示用户在缓存中没有存在！说明用户第一次下单！   从缓存中查看当前商品是否还有剩余库存！seckill:stock:skuId
        //存储商品库存的时候使用 redis的list数据类型 - leftPush    rightPop:出队，用户拿取   goodsId：商品id
        String goodsId = (String) redisTemplate.boundListOps(RedisConst.SECKILL_STOCK_PREFIX + skuId).rightPop();
        // 如果goodsId为空！
        if (StringUtils.isEmpty(goodsId)) {
            //  通知其他兄弟节点，更新状态位
            redisTemplate.convertAndSend("seckillpush", skuId + ":0");
            // 没有剩余库存了，商品售罄
            return;
        }
        // 如果goodsId不为空！说明有库存！然后我们需要将信息记录起来。      OrderRecode{有关于订单的信息}即要秒杀下单商品的数据
        OrderRecode orderRecode = new OrderRecode();
        // 在下单的时候，只允许每个用户购买一件商品，因此可以将商品数量写死！
        orderRecode.setNum(1);
        orderRecode.setUserId(userId);
        // 订单码的字符串 setOrderStr 是自己定义。
        orderRecode.setOrderStr(MD5.encrypt(userId + skuId));
        // 根据当前skuId ,来获取秒杀商品详情
        orderRecode.setSeckillGoods(this.getSeckillGoodsBySkuId(skuId));
        // 将预订单的数据放入缓存！
        redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS).put(orderRecode.getUserId(), orderRecode);
        // 更新库存  减库存
        this.updateStockCount(orderRecode.getSeckillGoods().getSkuId());
    }

    //根据商品id与用户ID查看秒杀订单信息
    @Override
    public Result checkOrder(Long skuId, String userId) {
        // 判断参加秒杀的用户在预下单数据的缓存中是否存在。
        Boolean isExist = redisTemplate.hasKey(RedisConst.SECKILL_USER + userId);
        // 如果返回true
        if (isExist) {
            // 判断用户是否预下单！
            // redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS).put(orderRecode.getUserId(),orderRecode);
            // 该用户有预订单生成  说明能够秒杀到
            Boolean flag = redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS).hasKey(userId);
            if (flag) {
                // 说明抢单成功！
                OrderRecode orderRecode = (OrderRecode) redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS).get(userId);
                // 返回对应的code码！秒杀成功！
                return Result.build(orderRecode, ResultCodeEnum.SECKILL_SUCCESS);
            }
        }
        // 判断是否下单
        // 下单成功的话，我们也需要将数据存储在缓存中！ key=seckill:orders:users field=userId value=orderId
        Boolean res = redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS_USERS).hasKey(userId);
        if (res) {
            //下单成功
            String orderId = (String) redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS_USERS).get(userId);
            // 表示下单成功！
            return Result.build(orderId, ResultCodeEnum.SECKILL_ORDER_SUCCESS);
        }
        // 判断我们的商品对应的状态位，
        String state = (String) CacheHelper.get(skuId.toString());
        // 0 说明售罄
        if ("0".equals(state)) {
            return Result.build(null, ResultCodeEnum.SECKILL_FAIL);
        }
        // 可以给一个默认值  排队中
        return Result.build(null, ResultCodeEnum.SECKILL_RUN);
    }

    //参加秒杀的商品被抢到后 准备减库存
    private void updateStockCount(Long skuId) {
        // 库存存储在redis-list 中，还有数据库中一份
        //redis中不需要更新，因为已经rightPop了，吐出来了
        Long count = redisTemplate.boundListOps(RedisConst.SECKILL_STOCK_PREFIX + skuId).size();
        // 为了避免频繁更新数据库，是2的倍数时，则更新一次数据库
        if (count % 2 == 0) {
            // 更新数据库以缓存为基准
            SeckillGoods seckillGood = getSeckillGoodsBySkuId(skuId);
            seckillGood.setStockCount(count.intValue());
            seckillGoodsMapper.updateById(seckillGood);
            // 缓存中秒杀商品对象中的库存数据，是需要更新的！
            redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS).put(seckillGood.getSkuId().toString(), seckillGood);
        }
    }
}
