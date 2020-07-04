package com.atguigu.gmall1213.list.repository;

import com.atguigu.gmall1213.model.list.Goods;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;


//操作Elasticsearch保存数据
public interface GoodsRepository extends ElasticsearchRepository<Goods,Long> {
}
