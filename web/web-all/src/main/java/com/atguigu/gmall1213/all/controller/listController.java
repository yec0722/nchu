package com.atguigu.gmall1213.all.controller;


import com.atguigu.gmall1213.common.result.Result;
import com.atguigu.gmall1213.list.client.ListFeignClient;
import com.atguigu.gmall1213.model.list.SearchParam;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class listController {
    @Autowired
    private ListFeignClient listFeignClient;

    @GetMapping("list.html")
    public String search(SearchParam searchParam, Model model) {
        Result<Map> result = listFeignClient.getList(searchParam);
        //制作检索条件拼接
        String urlParam = makeUrlParam(searchParam);
        //获取处理品牌的方法
        String trademark = makeTrademark(searchParam.getTrademark());
        //获取处理平台属性的方法
        List<Map<String, String>> list = makeProps(searchParam.getProps());
        //获取排序规则
        Map<String, Object> order = order(searchParam.getOrder());
        //存储数据  交给页面
        model.addAttribute("urlParam", urlParam);
        model.addAttribute("propsParamList", list);
        //页面要用到searchParam
        model.addAttribute("searchParam", searchParam);
        model.addAttribute("trademarkParam", trademark);
        //orderMap 里有type和sort两个属性
        model.addAttribute("orderMap", order);
        // return Result.ok(search);   数据是放在result封装中的  返回给页面
        model.addAllAttributes(result.getData());
        return "list/index";
    }

    //根据页面传过来的参数进行排序处理
    //传入参数的格式  按照综合排序  orderMap.type == '1'  &order=1:asc // &order=1:desc
    //按照价格排序 orderMap.type == '2'  &order=2:asc // &order=2:desc
    private Map<String, Object> order(String order) {
        HashMap<String, Object> hashMap = new HashMap<>();
        if (!StringUtils.isEmpty(order)) {
            String[] split = order.split(":");
            if (null != split && split.length > 0) {
                //指定数据处理type是按照综合还是价格排序
                hashMap.put("type", split[0]);
                //指定数据处理order是按照asc还是desc排序  '&order=1:'+${orderMap.sort == 'asc' ? 'desc' : 'asc'}"
                hashMap.put("sort", split[1]);
            }
        } else {
            //指定一个默认的规则进行排序
            hashMap.put("type", "1");
            hashMap.put("sort", "asc");
        }
        return hashMap;
    }

    //拼接检索条件
    private String makeUrlParam(SearchParam searchParam) {
        StringBuilder urlParam = new StringBuilder();
        //用户检索的条件只有两个，通过分类id检索和关键字keyword()检索
        //通过关键字keyword()检索  http://list.gmall.com/list.html?keyword=小米手机
        if (null != searchParam.getKeyword()) {
            //开始拼接搜索的关键字
            urlParam.append("keyword=").append(searchParam.getKeyword());
        }
        //通过分类三级id检索   http://list.gmall.com/list.html?category3Id=61
        if (null != searchParam.getCategory3Id()) {
            urlParam.append("category3Id=").append(searchParam.getCategory3Id());
        }
        //通过分类二级id检索   http://list.gmall.com/list.html?category2Id=1
        if (null != searchParam.getCategory2Id()) {
            urlParam.append("category2Id=").append(searchParam.getCategory2Id());
        }
        //通过分类一级id检索   http://list.gmall.com/list.html?category1Id=2
        if (null != searchParam.getCategory1Id()) {
            urlParam.append("category1Id=").append(searchParam.getCategory1Id());
        }

        //通过两个检索条件进来后，还可以再点击品牌进行检索
        //http://list.gmall.com/category3Id=61&trademark=2:华为   或者  http://list.gmall.com/keyword=小米手机&trademark=2:华为
        if (null != searchParam.getTrademark()) {
            if (urlParam.length() > 0) {
                urlParam.append("&trademark=").append(searchParam.getTrademark());
            }
        }
        //通过两个检索条件进来后，还可以再点击平台属性值进行检索
        if (null != searchParam.getProps()) {
            if (urlParam.length() > 0) {
                for (String prop : searchParam.getProps()) {
                    //prop里面有多个平台属性值，比如价格，屏幕尺寸等。。
                    //list.html?keyword=小米手机&trademark=4:小米&props=1:1700-2799:价格&props=2:6.45-6.54英寸:屏幕尺寸.......
                    urlParam.append("&props=").append(prop);
                }
            }
        }
        return "list.html?" + urlParam.toString();
    }

    //面包屑  处理品牌:品牌名称
    private String makeTrademark(String trademark) {
        //数据格式  trademark=4:小米
        if (!StringUtils.isEmpty(trademark)) {
            String[] split = trademark.split(":");
            if (null != split && split.length > 0) {
                return "品牌:" + split[1];
            }
        }
        return null;
    }

    //处理平台属性
    private List<Map<String, String>> makeProps(String[] props) {
        //数据格式   props=23:4G:运行内存   23:平台属性Id, 4G:平台属性值名称, 运行内存:平台属性名
        List<Map<String, String>> list = new ArrayList<>();
        if (null != props && props.length > 0) {
            //循环遍历
            for (String prop : props) {
                String[] split = prop.split(":");
                if (null != split && split.length == 3) {
                    HashMap<String, String> map = new HashMap<>();
                    map.put("attrId", split[0]);
                    map.put("attrValue", split[1]);
                    map.put("attrName", split[2]);
                    //这三个map只是一条数据  将所有map 都保存到集合
                    list.add(map);
                }
            }
        }
        return list;
    }
}
