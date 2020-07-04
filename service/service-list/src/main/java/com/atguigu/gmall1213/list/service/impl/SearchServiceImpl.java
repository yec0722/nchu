package com.atguigu.gmall1213.list.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall1213.list.repository.GoodsRepository;
import com.atguigu.gmall1213.list.service.SearchService;
import com.atguigu.gmall1213.model.list.*;
import com.atguigu.gmall1213.model.product.*;
import com.atguigu.gmall1213.product.client.ProductFeignClient;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchServiceImpl implements SearchService {

    @Autowired
    private ProductFeignClient productFeignClient;
    @Autowired
    private GoodsRepository goodsRepository;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RestHighLevelClient restHighLevelClient;

    //商品上架
    @Override
    public void upperGoods(Long skuId) {
        //声明一个被操作的实体类
        Goods goods = new Goods();
        //商品的基本信息
        SkuInfo skuInfo = productFeignClient.getSkuInfoById(skuId);
        if (null != skuId) {
            goods.setId(skuId);
            goods.setDefaultImg(skuInfo.getSkuDefaultImg());
            goods.setPrice(skuInfo.getPrice().doubleValue());
            goods.setTitle(skuInfo.getSkuName());
            goods.setCreateTime(new Date());
            //根据三级分类id直接查询一级、二级、三级的分类id和名称
            BaseCategoryView categoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id());
            if (null != categoryView) {
                goods.setCategory1Id(categoryView.getCategory1Id());
                goods.setCategory1Name(categoryView.getCategory1Name());
                goods.setCategory2Id(categoryView.getCategory2Id());
                goods.setCategory2Name(categoryView.getCategory2Name());
                goods.setCategory3Id(categoryView.getCategory3Id());
                goods.setCategory3Name(categoryView.getCategory3Name());
            }
            //平台属性 总共5个平台属性
            List<BaseAttrInfo> attrList = productFeignClient.getAttrList(skuId);
            List<SearchAttr> searchAttrList = attrList.stream().map(baseAttrInfo -> {
                //通过baseAttrInfo获得平台属性id
                SearchAttr searchAttr = new SearchAttr();
                searchAttr.setAttrId(baseAttrInfo.getId());
                searchAttr.setAttrName(baseAttrInfo.getAttrName());
                // 赋值平台属性值名称
                // 获取了平台属性值的集合
                List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
                //取出每个平台属性的第一个平台属性值
                searchAttr.setAttrValue(attrValueList.get(0).getValueName());
                // 将每个平台属性对象searchAttr 返回去
                return searchAttr;
            }).collect(Collectors.toList());
            //存储平台属性
            if (null != searchAttrList) {
                goods.setAttrs(searchAttrList);
            }
            //存储品牌信息
            BaseTrademark trademark = productFeignClient.getTrademark(skuInfo.getTmId());
            if (null != trademark) {
                goods.setTmId(trademark.getId());
                goods.setTmName(trademark.getTmName());
                goods.setTmLogoUrl(trademark.getLogoUrl());
            }
        }
        //将数据保存到Elasticsearch中  即上架
        goodsRepository.save(goods);
    }

    //商品下架
    @Override
    public void lowerGoods(Long skuId) {
        goodsRepository.deleteById(skuId);
    }

    //同时上架多个skuId.
    @Override
    public void upperGoods() {
        // 读一个一个excel 表格 ,所有要上传的skuId.
    }

    //更新热点访问次数  做商品热度排名
    @Override
    public void incrHotScore(Long skuId) {
        String key = "hotScore";
        //用户每访问一次，key代表的这个数据+1 每个skuId都会被访问
        Double hotScore = redisTemplate.opsForZSet().incrementScore(key, "sku:" + skuId, 1);
        //按照规则来更新热点的访问次数   每访问10次就更新一下
        if (hotScore % 10 == 0) {
            //从es中根据skuId获取商品对象信息
            Optional<Goods> optional = goodsRepository.findById(skuId);
            Goods goods = optional.get();
            //更新热点
            goods.setHotScore(Math.round(hotScore));
            //保存到es中，上架
            goodsRepository.save(goods);
        }
    }

    //根据SearchParam中封装好的条件进行全文检索
    @Override
    public SearchResponseVo search(SearchParam searchParam) throws Exception {
        //构建dsl语句，利用Java代码实现一个动态SQL语句
        SearchRequest searchRequest = buildQueryDsl(searchParam);
        //执行dsl语句  客户端执行search
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        //查询之后的结果集显示在页面上
        SearchResponseVo responseVo = parseSearchResult(searchResponse);
        //赋值分页信息     设置每页的数据取默认值
        responseVo.setPageSize(searchParam.getPageSize());
        //设置当前页
        responseVo.setPageNo(searchParam.getPageNo());
        //计算总页数(公式)   responseVo.getTotal():总记录数
        long totalPages = (responseVo.getTotal() + searchParam.getPageSize() - 1) / searchParam.getPageSize();
        //设置总页数
        responseVo.setTotalPages(totalPages);

        return responseVo;
    }

    //获取返回的结果集
    private SearchResponseVo parseSearchResult(SearchResponse searchResponse) {
        SearchResponseVo searchResponseVo = new SearchResponseVo();
        //获取到聚合后aggregations中的品牌信息   {aggregations -- tmIdAgg}
        Map<String, Aggregation> aggregationMap = searchResponse.getAggregations().asMap();
        //需要获取tmIdAgg中的buckets，但是Aggregation对象中并没有能获取到buckets的方法   因此需要做类型转换ParsedLongTerms
        ParsedLongTerms tmIdAgg = (ParsedLongTerms) aggregationMap.get("tmIdAgg");
        //{tmId -- { tmNameAgg 并列 tmLogoUrlAgg}}
        List<SearchResponseTmVo> responseTmVoList = tmIdAgg.getBuckets().stream().map(bucket -> {
            //声明一个品牌对象
            SearchResponseTmVo searchResponseTmVo = new SearchResponseTmVo();
            //获取品牌id
            String tmId = ((Terms.Bucket) bucket).getKeyAsString();
            //给品牌id赋值
            searchResponseTmVo.setTmId(Long.parseLong(tmId));

            //给品牌名称赋值   {tmId -- { tmNameAgg 并列 tmLogoUrlAgg}}
            Map<String, Aggregation> tmIdAggregationMap = ((Terms.Bucket) bucket).getAggregations().asMap();
            ParsedStringTerms tmNameAgg = (ParsedStringTerms) tmIdAggregationMap.get("tmNameAgg");
            //buckets" : [{"key" : "华为"}  只有一条数据
            String tmName = tmNameAgg.getBuckets().get(0).getKeyAsString();
            searchResponseTmVo.setTmName(tmName);

            //给品牌tmLogoUrl赋值   {tmId -- { tmNameAgg 并列 tmLogoUrlAgg}}
            ParsedStringTerms tmLogoUrlAgg = (ParsedStringTerms) tmIdAggregationMap.get("tmLogoUrlAgg");
            //"buckets" : [{ "key" : "http://192.168.200.128:8080/group1/M00/00/00/wKjIgF5r9PiES9u1AAAAAG725o8986.png"}  里面同样也只有一条数据
            String tmLogoUrl = tmLogoUrlAgg.getBuckets().get(0).getKeyAsString();
            searchResponseTmVo.setTmLogoUrl(tmLogoUrl);
            //返回品牌对象
            return searchResponseTmVo;
            //将之变成集合   collect(Collectors.toList())
        }).collect(Collectors.toList());
        //给trademarkList赋值       private List<SearchResponseTmVo> trademarkList;
        searchResponseVo.setTrademarkList(responseTmVoList);
        //平台属性     private List<SearchResponseAttrVo> attrsList = new ArrayList<>();
        ParsedNested attrAgg = (ParsedNested) aggregationMap.get("attrAgg");
        //{ attrAgg -- attrIdAgg}
        ParsedLongTerms attrIdAgg = attrAgg.getAggregations().get("attrIdAgg");
        List<? extends Terms.Bucket> buckets = attrIdAgg.getBuckets();
        //判断集合中是否有数据
        if (!CollectionUtils.isEmpty(buckets)) {
            List<SearchResponseAttrVo> responseAttrVoList = buckets.stream().map(bucket -> {
                //声明一个SearchResponseAttrVo对象  里面存放的是平台属性和平台属性值
                SearchResponseAttrVo searchResponseAttrVo = new SearchResponseAttrVo();
                //赋值平台属性id
                searchResponseAttrVo.setAttrId(((Terms.Bucket) bucket).getKeyAsNumber().longValue());

                //赋值平台属性值集合
                ParsedStringTerms attrValueAgg = ((Terms.Bucket) bucket).getAggregations().get("attrValueAgg");
                //平台属性值有多个
                List<? extends Terms.Bucket> valueAggBucketsList = attrValueAgg.getBuckets();
                //获取到集合中的每个平台属性值数据
                List<String> valueList = valueAggBucketsList.stream().map(Terms.Bucket::getKeyAsString).collect(Collectors.toList());
                //将获取到的属性值放入集合中
                searchResponseAttrVo.setAttrValueList(valueList);

                //赋值平台属性名
                ParsedStringTerms attrNameAgg = ((Terms.Bucket) bucket).getAggregations().get("attrNameAgg");
                //"buckets" : [{"key" : "价格","doc_count" : 5 }]  里面同样也只有一条数据
                String attrName = attrNameAgg.getBuckets().get(0).getKeyAsString();
                searchResponseAttrVo.setAttrName(attrName);

                return searchResponseAttrVo;
            }).collect(Collectors.toList());
            searchResponseVo.setAttrsList(responseAttrVoList);
        }
        //给商品赋值       private List<Goods> goodsList = new ArrayList<>();
        //hits 里面又嵌套了一个 subHits
        SearchHits hits = searchResponse.getHits();
        SearchHit[] subHits = hits.getHits();
        //声明一个商品对象集合
        ArrayList<Goods> goodsList = new ArrayList<>();
        if (null != subHits && subHits.length > 0) {
            for (SearchHit subHit : subHits) {
                //{subHits -- _source}  source里面是商品数据
                // subHit.getSourceAsString()  就已经将source里的商品信息全查了出来
                String sourceAsString = subHit.getSourceAsString();
                //将json转为object对象
                Goods goods = JSON.parseObject(sourceAsString, Goods.class);
                //获取goods中被高亮的title
                if (null != subHit.getHighlightFields().get("title")) {
                    //说明title不为空，获取title中的高亮字段    里面只有一条数据
                    Text title = subHit.getHighlightFields().get("title").getFragments()[0];
                    //将goods中的title替换为高亮后的相同字段
                    goods.setTitle(title.toString());
                }
                //将对象添加到集合中
                goodsList.add(goods);
            }
        }
        //给商品信息(goodsList)赋值
        searchResponseVo.setGoodsList(goodsList);

        //private Long total;//总记录数
        searchResponseVo.setTotal(hits.getTotalHits());
        return searchResponseVo;
    }

    //利用Java代码实现一个动态dsl语句
    private SearchRequest buildQueryDsl(SearchParam searchParam) {
        //定义查询器
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //构建queryBuilder  {bool}
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        //判断输入的检索词是否为空  不为空，构建查询语句
        if (!StringUtils.isEmpty(searchParam.getKeyword())) {
            //must 下面有match   searchParam.getKeyword():所有检索输入的词都在这里面
            //Operator.AND 表示拆分的词语在title中同时存在才会查询  比如：查询小米手机 -----title里面必须同时存在“小米手机”才会查询
            MatchQueryBuilder title = QueryBuilders.matchQuery("title", searchParam.getKeyword()).operator(Operator.AND);
            //根bool 下面有must
            boolQueryBuilder.must(title);
        }
        //根据用户点击的分类id查询
        if (null != searchParam.getCategory1Id()) {
            //filter 下面是term  按照一级分类id过滤条件
            TermQueryBuilder category1Id = QueryBuilders.termQuery("category1Id", searchParam.getCategory1Id());
            //bool 下面是filter
            boolQueryBuilder.filter(category1Id);
        }
        if (null != searchParam.getCategory2Id()) {
            //  按照二级分类id过滤条件
            TermQueryBuilder category2Id = QueryBuilders.termQuery("category2Id", searchParam.getCategory2Id());
            //  {bool -- filter }
            boolQueryBuilder.filter(category2Id);
        }
        if (null != searchParam.getCategory3Id()) {
            //  按照三级分类id过滤条件
            TermQueryBuilder category3Id = QueryBuilders.termQuery("category3Id", searchParam.getCategory3Id());
            //  {bool -- filter }
            boolQueryBuilder.filter(category3Id);
        }
        //根据用户点击的品牌查询  判断用户是否输入了品牌查询条件  查询参数应该是这样的：【trademark=2:华为】
        //获取要查询的品牌数据
        String trademark = searchParam.getTrademark();
        if (!StringUtils.isEmpty(trademark)) {
            //走了品牌查询  通过key获取值 2:华为 并且将value进行分割
            //split[0]=2   split[1]=华为
            String[] split = trademark.split(":");
            //判断数据格式是否正确
            if (null != split && split.length == 2) {
                //{filter -- term  "tmId":"4"}
                TermQueryBuilder tmId = QueryBuilders.termQuery("tmId", split[0]);
                //{bool -- filter }
                boolQueryBuilder.filter(tmId);
            }
        }
        //根据用户点击的平台属性值进行查询
        String[] props = searchParam.getProps();
        if (null != props && props.length > 0) {
            //用户进行了品牌查询  getProps()中数据的样子 props=23:4G:运行内存  23:平台属性Id, 4G:平台属性值名称, 运行内存:平台属性名
            //对当前数据进行分割   循环遍历props
            for (String prop : props) {
                String[] split = prop.split(":");
                if (null != split && split.length == 3) {
                    //bool(boolQuery) 里面又嵌套了一个bool(subBoolQuery)  要创建两个bool
                    BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
                    BoolQueryBuilder subBoolQuery = QueryBuilders.boolQuery();
                    //{subBoolQuery -- must -- term}
                    subBoolQuery.must(QueryBuilders.termQuery("attrs.attrId", split[0]));
                    //根据平台属性值过滤
                    subBoolQuery.must(QueryBuilders.termQuery("attrs.attrValue", split[1]));

                    //开始嵌套
                    boolQuery.must(QueryBuilders.nestedQuery("attrs", subBoolQuery, ScoreMode.None));
                    //整合{根bool -- filter -- boolQuery}
                    boolQueryBuilder.filter(boolQuery);
                }
            }
        }
        //  {query}
        searchSourceBuilder.query(boolQueryBuilder);
        //分页设置   "from","size"与query是并列关系，因此要在query外执行
        //计算每页开始的起始条数 比如：每页显示两条数据，第一页：0,1  第二页：2,3 。。。。。
        int from = (searchParam.getPageNo() - 1) * searchParam.getPageSize();
        //当前页数据信息
        searchSourceBuilder.from(from);
        //每页显示多少条数据
        searchSourceBuilder.size(searchParam.getPageSize());
        //设置高亮
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        //设置要高亮的属性字段
        highlightBuilder.field("title");
        //设置高亮的格式
        highlightBuilder.postTags("</span>");
        highlightBuilder.preTags("<span style=color:red>");
        //将设置好的高亮对象放入方法中
        searchSourceBuilder.highlighter(highlightBuilder);

        //做排序
        String order = searchParam.getOrder();
        //用户点了排序
        if (!StringUtils.isEmpty(order)) {
            //排序数据格式 1:hotScore 2:price       对数据进行分割
            String[] split = order.split(":");
            if (null != split && split.length == 2) {
                //声明一个字段field记录看用户点击了哪个属性进行的排序
                String field = null;
                switch (split[0]) {
                    //如果split[0]=1  证明用户点击了hotScore做的排序
                    case "1":
                        field = "hotScore";
                        break;
                    case "2":
                        field = "price";
                        break;
                }
                // 设置排序规则   sort与query平级，因此要放在查询器中
                /*searchSourceBuilder.sort(field, "price".equals(split[1]) ? SortOrder.ASC : SortOrder.DESC);
                  这里排序规则已经写死了  我们应该按照从页面用户点击的排序方式进行排序
                 */
                //&order=1:asc || &order=1:desc   这里的排序是从页面传过来的
                searchSourceBuilder.sort(field, "asc".equals(split[1]) ? SortOrder.ASC : SortOrder.DESC);
            } else {
                //order里面的数据格式并不是 1:hotScore 2:price这样的排序格式 给一个默认的排序方式，比如按照hotScore排序
                searchSourceBuilder.sort("hotScore", SortOrder.DESC);
            }
        }
        //设置聚合   {tmIdAgg -- { tmNameAgg 并列 tmLogoUrlAgg}}
        TermsAggregationBuilder termsAggregationBuilder =
                AggregationBuilders.terms("tmIdAgg").field("tmId")
                        .subAggregation(AggregationBuilders.terms("tmNameAgg").field("tmName"))
                        .subAggregation(AggregationBuilders.terms("tmLogoUrlAgg").field("tmLogoUrl"));
        //将品牌Agg放入查询器
        searchSourceBuilder.aggregation(termsAggregationBuilder);

        //设置平台属性聚合  attrAgg并列tmIdAgg   attrAgg是nested聚合
        NestedAggregationBuilder nestedAggregationBuilder = AggregationBuilders.nested("attrAgg", "attrs")
                .subAggregation(AggregationBuilders.terms("attrIdAgg").field("attrs.attrId")
                        .subAggregation(AggregationBuilders.terms("attrNameAgg").field("attrs.attrName"))
                        .subAggregation(AggregationBuilders.terms("attrValueAgg").field("attrs.attrValue")));
        //将平台属性放入查询器
        searchSourceBuilder.aggregation(nestedAggregationBuilder);
        //对执行_search之后得到的数据结果集进行过滤    结果集中只显示"id", "defaultImg", "title", "price"字段值
        searchSourceBuilder.fetchSource(new String[]{"id", "defaultImg", "title", "price"}, null);
        //指定index，type        GET /goods/info/_search {}
        SearchRequest searchRequest = new SearchRequest("goods");
        searchRequest.types("info");
        searchRequest.source(searchSourceBuilder);
        //打印dsl语句
        System.out.println("dsl语句:" + searchSourceBuilder.toString());
        return searchRequest;
    }
}
