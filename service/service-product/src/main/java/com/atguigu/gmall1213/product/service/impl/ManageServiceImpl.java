package com.atguigu.gmall1213.product.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall1213.common.cache.GmallCache;
import com.atguigu.gmall1213.common.constant.MqConst;
import com.atguigu.gmall1213.common.constant.RedisConst;
import com.atguigu.gmall1213.common.service.RabbitService;
import com.atguigu.gmall1213.model.product.*;
import com.atguigu.gmall1213.product.mapper.*;
import com.atguigu.gmall1213.product.service.ManageService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.Key;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class ManageServiceImpl implements ManageService {

    @Autowired
    private BaseCategory1Mapper baseCategory1Mapper;
    @Autowired
    private BaseCategory2Mapper baseCategory2Mapper;
    @Autowired
    private BaseCategory3Mapper baseCategory3Mapper;
    @Autowired
    private BaseAttrInfoMapper baseAttrInfoMapper;
    @Autowired
    private BaseAttrValueMapper baseAttrValueMapper;
    @Autowired
    private SpuInfoMapper spuInfoMapper;
    @Autowired
    private BaseSaleAttrMapper baseSaleAttrMapper;
    @Autowired
    private SpuImageMapper spuImageMapper;
    @Autowired
    private SpuSaleAttrValueMapper spuSaleAttrValueMapper;
    @Autowired
    private SpuSaleAttrMapper spuSaleAttrMapper;
    @Autowired
    private SkuInfoMapper skuInfoMapper;
    @Autowired
    private SkuImageMapper skuImageMapper;
    @Autowired
    private SkuSaleAttrValueMapper skuSaleAttrValueMapper;
    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;
    @Autowired
    private BaseCategoryViewMapper baseCategoryViewMapper;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private BaseTrademarkMapper baseTrademarkMapper;
    @Autowired
    private RabbitService rabbitService;

    @Override
    public List<BaseCategory1> getCategory1() {
        //查询一级分类，即查询所有
        return baseCategory1Mapper.selectList(null);

    }

    @Override
    public List<BaseCategory2> getCategory2(Long category1Id) {
        //查询二级分类
        QueryWrapper<BaseCategory2> baseCategory2QueryWrapper = new QueryWrapper<>();
        baseCategory2QueryWrapper.eq("category1_id", category1Id);
        return baseCategory2Mapper.selectList(baseCategory2QueryWrapper);
    }

    @Override
    public List<BaseCategory3> getCategory3(Long category2Id) {
        //查询三级分类
        QueryWrapper<BaseCategory3> baseCategory3QueryWrapper = new QueryWrapper<>();
        baseCategory3QueryWrapper.eq("category2_id", category2Id);
        return baseCategory3Mapper.selectList(baseCategory3QueryWrapper);
    }

    @Override
    public List<BaseAttrInfo> getAttrInfoList(Long category1Id, Long category2Id, Long category3Id) {

        return baseAttrInfoMapper.selectBaseAttrInfoList(category1Id, category2Id, category3Id);
    }

    @Override
    @Transactional
    public void saveAttrInfo(BaseAttrInfo baseAttrInfo) {
        //对于AttrInfo表
        if (baseAttrInfo.getId() != null) {
            baseAttrInfoMapper.updateById(baseAttrInfo);
        } else {
            baseAttrInfoMapper.insert(baseAttrInfo);
        }
        //对于AttrInfoValue表
        //修改的方法:先删后加
        QueryWrapper<BaseAttrValue> baseAttrValueQueryWrapper = new QueryWrapper<>();
        baseAttrValueQueryWrapper.eq("attr_id", baseAttrInfo.getId());
        baseAttrValueMapper.delete(baseAttrValueQueryWrapper);
        List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
        if (null != attrValueList && attrValueList.size() > 0) {
            for (BaseAttrValue baseAttrValue : attrValueList) {
                baseAttrValue.setAttrId(baseAttrInfo.getId());
                baseAttrValueMapper.insert(baseAttrValue);
            }
        }
    }

    @Override
    public BaseAttrInfo getAttrInfo(Long attrId) {
        //查出平台属性
        BaseAttrInfo baseAttrInfo = baseAttrInfoMapper.selectById(attrId);
        if (null != baseAttrInfo) {
            QueryWrapper<BaseAttrValue> baseAttrValueQueryWrapper = new QueryWrapper<>();
            baseAttrValueQueryWrapper.eq("attr_id", attrId);
            //手动赋值
            List<BaseAttrValue> baseAttrValueList = baseAttrValueMapper.selectList(baseAttrValueQueryWrapper);
            baseAttrInfo.setAttrValueList(baseAttrValueList);
        }
        //查询平台属性数据
        //因为数据库中没有AttrValueList字段，故不能直接返回
        return baseAttrInfo;
    }

    @Override
    public IPage<SpuInfo> selectPage(Page<SpuInfo> spuInfoPageParam, SpuInfo spuInfo) {
        QueryWrapper<SpuInfo> spuInfoQueryWrapper = new QueryWrapper<>();
        spuInfoQueryWrapper.eq("category3_id", spuInfo.getCategory3Id());
        // 查询完成之后，可以按照某一种规则进行排序。
        spuInfoQueryWrapper.orderByDesc("id");
        return spuInfoMapper.selectPage(spuInfoPageParam, spuInfoQueryWrapper);
    }

    @Override
    public List<BaseSaleAttr> getBaseSaleAttrList() {
        List<BaseSaleAttr> baseSaleAttrList = baseSaleAttrMapper.selectList(null);
        return baseSaleAttrList;
    }

    @Override
    @Transactional
    public void saveSpuInfo(SpuInfo spuInfo) {
        //spuInfo表
        spuInfoMapper.insert(spuInfo);
        //spuImage列表
        List<SpuImage> spuImageList = spuInfo.getSpuImageList();
        if (null != spuImageList && spuImageList.size() > 0) {
            for (SpuImage spuImage : spuImageList) {
                spuImage.setSpuId(spuInfo.getId());
                spuImageMapper.insert(spuImage);
            }
        }
        //spuSaleAttr销售属性
        List<SpuSaleAttr> spuSaleAttrList = spuInfo.getSpuSaleAttrList();
        if (null != spuSaleAttrList && spuSaleAttrList.size() > 0) {
            for (SpuSaleAttr spuSaleAttr : spuSaleAttrList) {
                spuSaleAttr.setSpuId(spuInfo.getId());
                spuSaleAttrMapper.insert(spuSaleAttr);

                //spuSaleAttrValue销售属性值表
                List<SpuSaleAttrValue> spuSaleAttrValueList = spuSaleAttr.getSpuSaleAttrValueList();
                if (null != spuSaleAttrValueList && spuSaleAttrValueList.size() > 0) {
                    for (SpuSaleAttrValue spuSaleAttrValue : spuSaleAttrValueList) {
                        spuSaleAttrValue.setSpuId(spuInfo.getId());
                        spuSaleAttrValue.setSaleAttrName(spuSaleAttr.getSaleAttrName());
                        spuSaleAttrValueMapper.insert(spuSaleAttrValue);
                    }
                }
            }
        }
    }

    //回显图片列表
    @Override
    public List<SpuImage> getSpuImageList(Long spuId) {
        QueryWrapper<SpuImage> spuImageQueryWrapper = new QueryWrapper<>();
        spuImageQueryWrapper.eq("spu_id", spuId);
        List<SpuImage> spuImageList = spuImageMapper.selectList(spuImageQueryWrapper);
        return spuImageList;
    }

    //回显销售属性和销售属性值
    @Override
    public List<SpuSaleAttr> getSpuSaleAttrList(Long spuId) {

        return spuSaleAttrMapper.selectSpuSaleAttrList(spuId);
    }

    @Override
    @Transactional
    //保存SKU  商品添加
    public void saveSkuInfo(SkuInfo skuInfo) {
        //skuInfo 库存单元表
        skuInfoMapper.insert(skuInfo);
        //skuSaleAttrValue sku与销售属性值的中间表
        List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
        if (null != skuSaleAttrValueList && skuSaleAttrValueList.size() > 0) {
            for (SkuSaleAttrValue skuSaleAttrValue : skuSaleAttrValueList) {
                skuSaleAttrValue.setSkuId(skuInfo.getId());
                skuSaleAttrValue.setSpuId(skuInfo.getSpuId());
                skuSaleAttrValueMapper.insert(skuSaleAttrValue);
            }
        }
        // skuAttrValue 平台属性数据
        List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();
        if (null != skuAttrValueList && skuAttrValueList.size() > 0) {
            for (SkuAttrValue skuAttrValue : skuAttrValueList) {
                skuAttrValue.setSkuId(skuInfo.getId());
                skuAttrValueMapper.insert(skuAttrValue);
            }
        }
        // skuImage 图片列表
        List<SkuImage> skuImageList = skuInfo.getSkuImageList();
        if (null != skuImageList && skuImageList.size() > 0) {
            for (SkuImage skuImage : skuImageList) {
                skuImage.setSkuId(skuInfo.getId());
                skuImageMapper.insert(skuImage);
            }
        }
        //发送一个商品上架的信息
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_GOODS, MqConst.ROUTING_GOODS_UPPER, skuInfo.getId());
    }

    @Override
    //分页查询SKU
    public IPage<SkuInfo> selectPage(Page<SkuInfo> skuInfoPagenfo) {
        QueryWrapper<SkuInfo> skuInfoQueryWrapper = new QueryWrapper<>();
        skuInfoQueryWrapper.orderByDesc("id");
        return skuInfoMapper.selectPage(skuInfoPagenfo, skuInfoQueryWrapper);
    }

    //实现商品上架  is_sale=1 商品上架，=0 商品下架
    @Override
    public void onSale(Long skuId) {
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setIsSale(1);
        QueryWrapper<SkuInfo> skuInfoQueryWrapper = new QueryWrapper<>();
        skuInfoQueryWrapper.eq("id", skuId);
        skuInfoMapper.update(skuInfo, skuInfoQueryWrapper);
        //发送一个商品上架的信息
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_GOODS, MqConst.ROUTING_GOODS_UPPER, skuId);
    }

    @Override
    //实现商品下架
    public void cancelSale(Long skuId) {
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setIsSale(0);
        QueryWrapper<SkuInfo> skuInfoQueryWrapper = new QueryWrapper<>();
        skuInfoQueryWrapper.eq("id", skuId);
        skuInfoMapper.update(skuInfo, skuInfoQueryWrapper);
        //发送一个商品下架的信息
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_GOODS, MqConst.ROUTING_GOODS_LOWER, skuId);
    }

    @Override
    @GmallCache(prefix = "sku")
    public SkuInfo getSkuInfo(Long skuId) {
        return getSkuInfoDB(skuId);
    }

    private SkuInfo getSkuInfoRedisson(Long skuId) {
        //在此获取skuInfo 的时候，先查询缓存，如果缓存中有数据，则查询，没有查询数据库并放入缓存!
        SkuInfo skuInfo = null;
        try {
            // 先判断缓存中是否有数据，查询缓存必须知道缓存的key是什么！
            // 定义缓存的key 商品详情的缓存key=sku:skuId:info
            String skuKey = RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKUKEY_SUFFIX;
            // 根据key 获取缓存中的数据
            // 如果查询一个不存在的数据，那么缓存中应该是一个空对象{这个对象有地址，但是属性Id，price 等没有值}
            skuInfo = (SkuInfo) redisTemplate.opsForValue().get(skuKey);
            // 存储数据为什么使用String ，存储对象的时候建议使用Hash---{hset(skuKey,字段名,字段名所对应的值); 便于对当前对象中属性修改}
            // 对于商品详情来讲：我们只做显示，并没有修改。所以此处可以使用String 来存储!
            if (skuInfo == null) {
                // 缓存中不存在，从数据库中获取数据，防止缓存击穿做分布式锁
                // 定义分布式锁的key lockKey=sku:skuId:lock
                String lockKey = RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKULOCK_SUFFIX;
                // 使用redisson获取锁
                RLock lock = redissonClient.getLock(lockKey);
                // 尝试加锁，最多等待100秒，上锁以后10秒自动解锁
                boolean res = lock.tryLock(RedisConst.SKULOCK_EXPIRE_PX2, RedisConst.SKULOCK_EXPIRE_PX1, TimeUnit.SECONDS);
                if (res) {
                    try {
                        // 从数据库中获取数据
                        skuInfo = getSkuInfoDB(skuId);
                        if (skuInfo == null) {
                            //如果从数据库中没有获取到kuInfo
                            // 为了防止缓存穿透，设置一个空对象放入缓存,这个时间建议不要太长！
                            SkuInfo skuInfo1 = new SkuInfo();
                            // 放入缓存
                            redisTemplate.opsForValue().set(skuKey, skuInfo1, RedisConst.SKUKEY_TEMPORARY_TIMEOUT, TimeUnit.SECONDS);
                            // 返回数据
                            return skuInfo1;
                        }
                        // 从数据库中获取到了数据，放入缓存
                        redisTemplate.opsForValue().set(skuKey, skuInfo, RedisConst.SKUKEY_TIMEOUT, TimeUnit.SECONDS);
                        return skuInfo;
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        // 解锁
                        lock.unlock();
                    }
                } else {
                    // 此时的线程并没有获取到分布式锁，应该等待,
                    Thread.sleep(1000);
                    // 等待完成之后，还需要查询数据！
                    return getSkuInfo(skuId);
                }
            } else {
                return skuInfo; // 情况一：这个对象有地址，但是属性Id，price 等没有值！  情况二：就是既有地址，又有属性值！
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 如何中途发送了异常：数据库挺一下！
        return getSkuInfoDB(skuId);
    }

    private SkuInfo getSkuInfoRedis(Long skuId) {
        //在这里获取skuInfo的时候，先查询缓存，如果缓存中有，就从缓存在中拿数据，如果没有，先查后放
        SkuInfo skuInfo = null;
        try {
            //定义缓存中数据的key,要在缓存中查数据必须知道key是什么
            String skuKey = RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKUKEY_SUFFIX;
            //根据key从缓存中拿数据
            skuInfo = (SkuInfo) redisTemplate.opsForValue().get(skuKey);
            //缓存中没数据，从数据库中取数据，并放入缓存、并且为了防止缓存击穿，需要设置一个分布式锁！
            if (skuInfo == null) {
                //定义分布式锁的key
                String lockKey = RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKULOCK_SUFFIX;
                //定义分布式锁的value
                String uuid = UUID.randomUUID().toString();
                //开始上锁
                Boolean isExist = redisTemplate.opsForValue().setIfAbsent(lockKey, uuid, RedisConst.SKULOCK_EXPIRE_PX1, TimeUnit.SECONDS);
                //如果返回true 则表明获取到了分布式锁
                if (isExist) {
                    System.out.println("获取到了锁");
                    skuInfo = getSkuInfoDB(skuId);
                    if (skuInfo == null) {
                        //如果数据库中不存在这个skuInfo
                        //防止缓存穿透、缓存一个空对象进去作为value，并且这个key的过期时间不能太长
                        SkuInfo skuInfo1 = new SkuInfo();
                        redisTemplate.opsForValue().set(skuKey, skuInfo1, RedisConst.SKUKEY_TEMPORARY_TIMEOUT, TimeUnit.SECONDS);
                        return skuInfo1;
                    }
                    //数据库中查出来不为空，放入缓存
                    redisTemplate.opsForValue().set(skuKey, skuInfo, RedisConst.SKUKEY_TIMEOUT, TimeUnit.SECONDS);
                    //删除锁、使用lua脚本
                    String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
                    // 如何操作：
                    // 构建RedisScript 数据类型需要确定一下，默认情况下返回的Object
                    DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
                    // 指定好返回的数据类型
                    redisScript.setResultType(Long.class);
                    // 指定好lua 脚本
                    redisScript.setScriptText(script);
                    // 第一个参数存储的RedisScript  对象，第二个参数指的锁的key，第三个参数指的key所对应的值
                    redisTemplate.execute(redisScript, Arrays.asList(lockKey), uuid);
                    //返回正常数据
                    return skuInfo;
                } else {
                    //此时其他线程并没有获取到分步式锁，应该等待
                    Thread.sleep(1000);
                    //等待完成之后，还要再从缓存中查询数据
                    return getSkuInfo(skuId);
                }
            } else {
                //缓存中有数据、直接从缓存中取
                return skuInfo;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //出现异常，让数据库兜底
        return getSkuInfoDB(skuId);
    }

    private SkuInfo getSkuInfoDB(Long skuId) {
        //根据SkuId查询SKU基本信息
        SkuInfo skuInfo = skuInfoMapper.selectById(skuId);
        if (null != skuInfo) {
            //查询skuImage图片信息
            List<SkuImage> skuImageList = skuImageMapper.selectList(new QueryWrapper<SkuImage>().eq("sku_id", skuId));
            skuInfo.setSkuImageList(skuImageList);
        }
        return skuInfo;
    }

    /**
     * 根据三级分类Id 来获取分类名称
     */
    @Override
    @GmallCache(prefix = "baseCategoryView")
    public BaseCategoryView getBaseCategoryViewBycategory3Id(Long category3Id) {
        return baseCategoryViewMapper.selectById(category3Id);
    }

    //根据skuId获取商品价格信息
    @Override
    @GmallCache(prefix = "price")
    public BigDecimal getSkuPriceBySkuId(Long skuId) {
        SkuInfo skuInfo = skuInfoMapper.selectById(skuId);
        if (null != skuInfo) {
            return skuInfo.getPrice();
        }
        //否则返回0
        return new BigDecimal(0);
    }

    //根据skuId spuId 查询销售属性及销售属性值集合数据
    @Override
    @GmallCache(prefix = "spuSaleAttr")
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(Long skuId, Long spuId) {
        return spuSaleAttrMapper.selectSpuSaleAttrListCheckBySku(skuId, spuId);
    }

    //根据skuId查询
    @Override
    @GmallCache(prefix = "skuValueIdsMap")
    public Map getSkuValueIdsMap(Long spuId) {
        // 调用mapper 自定义方法获取数据，将数据查询之后直接放入List。
        HashMap<Object, Object> map = new HashMap<>();
        /*
            SELECT
                sv.sku_id,
                group_concat( sv.sale_attr_value_id ORDER BY sp.base_sale_attr_id ASC SEPARATOR '|' ) value_ids
            FROM
                sku_sale_attr_value sv
                INNER JOIN spu_sale_attr_value sp ON sp.id = sv.sale_attr_value_id
            WHERE
                sv.spu_id = 14
            GROUP BY
                sku_id;
            执行出来的结果应该是List<Map>
            map.put("55|57","30") skuSaleAttrValueMapper
         */
        List<Map> mapList = skuSaleAttrValueMapper.getSaleAttrValuesBySpu(spuId);
        // 获取到数据以后。开始循环遍历集合中的每条数据
        if (null != mapList && mapList.size() > 0) {
            for (Map skuMaps : mapList) {
                // map.put("55|57","30")
                //根据"55|57"，就可以得到唯一的“30”
                map.put(skuMaps.get("value_ids"), skuMaps.get("sku_id"));
            }
        }
        return map;
    }

    /**
     * 获取全部分类数据信息
     */
    @Override
    @GmallCache(prefix = "index")
    //    数据显示格式：
    //            [
    //    {
    //        "index": 1,
    //            "categoryChild": [ # 一级分类下的二级分类数据
    //        {
    //            "categoryChild": [ # 二级分类下的三级分类数据
    //            {
    //                "categoryName": "电子书", # 三级分类的name
    //                "categoryId": 1
    //            },
    //            {
    //                "categoryName": "网络原创", # 三级分类的name
    //                "categoryId": 2
    //            },
    //						  ...
    //						],
    //            "categoryName": "电子书刊", #二级分类的name
    //            "categoryId": 1
    //        },
    //					 ...
    //					],
    //        "categoryName": "图书、音像、电子书刊", # 一级分类的name
    //        "categoryId": 1
    //    },
    //            ...
    //}
    //				]
    public List<JSONObject> getBaseCategoryList() {
        List<JSONObject> list = new ArrayList<>();
        //先获取到所有的分类数据
        List<BaseCategoryView> baseCategoryViewList = baseCategoryViewMapper.selectList(null);
        //按照一级分类id进行分组        数据库中有多条一级分类(相同)的数据 进行分组以后在页面上每个一级分类只显示一条数据显示  固定语法
        Map<Long, List<BaseCategoryView>> category1Map = baseCategoryViewList.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory1Id));
        //初始化一个index  构建json字符串中的"index" :1   即表明这是一级分类中的第一条数据
        int index = 1;
        //取一级分类下的数据
        for (Map.Entry<Long, List<BaseCategoryView>> entry : category1Map.entrySet()) {
            //获取一级分类id
            Long categor1Id = entry.getKey();
            //获取一级分类下的所有数据  即一级分类、二级分类、三级分类的所有数据
            List<BaseCategoryView> category2List = entry.getValue();
            //声明一个对象保存一级分类的json字符串   按照格式保存
            JSONObject category1 = new JSONObject();
            category1.put("index", index);
            //保存一级分类的id
            category1.put("categoryId", categor1Id);
            //保存一级分类的name     get(0):分组后的第一条数据
            category1.put("categoryName", category2List.get(0).getCategory1Name());
            //一级分类下的二级(包含三级)分类数据
//            category1.put("categoryChild",)
            //第一条一级分类数据已经搞完了     于是指向一级分类的下一条数据
            index++;
            //根据二级分类id进行分组
            Map<Long, List<BaseCategoryView>> category2Map = category2List.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));
            //分组完成后每条一级分类下有多条二级分类信息  创建一个存储二级分类信息的集合
            List<JSONObject> category2Child = new ArrayList<>();
            for (Map.Entry<Long, List<BaseCategoryView>> entry2 : category2Map.entrySet()) {
                //获取二级分类id
                Long categor2Id = entry2.getKey();
                //获取二级分类下的所有数据信息   即二级分类和三级分类所有数据
                List<BaseCategoryView> category3List = entry2.getValue();
                //声明一个对象保存二级分类的json字符串   按照格式保存
                JSONObject category2 = new JSONObject();
                category2.put("categoryId", categor2Id);
                category2.put("categoryName", category3List.get(0).getCategory2Name());
//                category2.put("categoryChild",);
                //将所有二级分类对象放进集合中
                category2Child.add(category2);

                //处理三级分类数据信息   根据三级分类id进行分组
                Map<Long, List<BaseCategoryView>> category3Map = category3List.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory3Id));
                //分组完成后每条二级分类下有多条三级分类信息  创建一个存储三级分类信息的集合
                List<JSONObject> category3Child = new ArrayList<>();
                for (Map.Entry<Long, List<BaseCategoryView>> entry3 : category3Map.entrySet()) {
                    //获取三级分类id
                    Long categor3Id = entry3.getKey();
                    //获取三级分类下的所有数据信息   即三级分类所有数据信息
                    List<BaseCategoryView> category4List = entry3.getValue();
                    //声明一个对象保存三级分类的json字符串   按照格式保存
                    JSONObject category3 = new JSONObject();
                    category3.put("categoryId", categor3Id);
                    category3.put("categoryName", category4List.get(0).getCategory3Name());
//                category3.put("categoryChild",);
                    //将所有三级分类对象放进集合中
                    category3Child.add(category3);
                }
                //将三级分类数据category3Child放入二级分类的categoryChild
                category2.put("categoryChild", category3Child);
            }
            //将二级分类数据category2Child放入一级分类的categoryChild
            category1.put("categoryChild", category2Child);
            //整个一二三及分类数据全部扔进集合里
            list.add(category1);
        }
        //按照json数据接口方式 分别去封装一级分类、二级分类、三级分类数据信息
        //封装完成之后，将数据返回
        return list;
    }

    //根据品牌id查询品牌数据
    @Override
    public BaseTrademark getBaseTrademarkByTmId(Long tmId) {
        return baseTrademarkMapper.selectById(tmId);
    }

    //根据skuId获取平台属性和平台属性值
    @Override
    public List<BaseAttrInfo> getAttrInfoList(Long skuId) {
        //sku_attr_value这张中间表没有平台属性、平台属性值名称，因此需要进行多表关联查询
        return baseAttrInfoMapper.selectAttrInfoList(skuId);
    }
}
