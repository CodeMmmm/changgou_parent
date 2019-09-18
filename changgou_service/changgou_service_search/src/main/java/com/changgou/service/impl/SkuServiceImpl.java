package com.changgou.service.impl;

import com.alibaba.fastjson.JSON;
import com.changgou.dao.SkuEsMapper;
import com.changgou.goods.feign.SkuFeign;
import com.changgou.goods.pojo.Sku;
import com.changgou.search.pojo.SkuInfo;
import com.changgou.service.SkuService;
import entity.Result;
import io.swagger.util.Json;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.join.query.JoinQueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortMode;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.SearchResultMapper;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;

@Service
public class SkuServiceImpl implements SkuService{
    @Autowired
    private SkuFeign skuFeign;

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    @Autowired
    private SkuEsMapper skuEsMapper;

    /***
     * 导入SKU数据
     */
    @Override
    public void importSku() {
        // 获取list<sku>
        Result<List<Sku>> result = skuFeign.findByStatus("1");
        // 转为list<skuinfo>
        List<SkuInfo> skuInfoList = JSON.parseArray(JSON.toJSONString(result.getData()),SkuInfo.class);
        // 将spec中的数据转为specmap存入
        for (SkuInfo skuInfo : skuInfoList) {
            // Map中的每个key都会作为一个域
            Map<String,Object> specMap = JSON.parseObject(skuInfo.getSpec(),Map.class);
            skuInfo.setSpecMap(specMap);
        }
        // 存入索引中
        skuEsMapper.saveAll(skuInfoList);
    }

    /***
     * 搜索
     * @param searchMap
     * @return
     */
    @Override
    public Map search(Map<String, String> searchMap) {

        //  根据名字搜索
        NativeSearchQueryBuilder nativeSearchQueryBuilder = buildBasicQuery(searchMap);
        // 搜索到的集合
        Map resultMap = searchList(nativeSearchQueryBuilder);

        Map<String, Object> stringObjectMap = searchGroupList(nativeSearchQueryBuilder, searchMap);
        resultMap.putAll(stringObjectMap);

        resultMap.put("pageNum",nativeSearchQueryBuilder.build().getPageable().getPageNumber());
        resultMap.put("pageSize",nativeSearchQueryBuilder.build().getPageable().getPageSize());
        return resultMap;
    }

    /**
     * 搜索所有规格
     * @param specList
     * @return
     */
    private Map<String, Set<String>> specPutAll(List<String> specList) {

        Map<String,Set<String>> specMap = new HashMap<>();
        for (String specStr : specList) {
            // spec的json字符串转为map
            Map<String,String> spec = JSON.parseObject(specStr,Map.class);
            // 遍历map，规格存入
            for (Map.Entry<String, String> entry : spec.entrySet()) {
                String key = entry.getKey();
                Set<String> set = specMap.get(key);
                if (specMap.get(key) == null){
                    // 说明未添加过该参数，
                    set = new HashSet<>();
                }
                set.add(entry.getValue());
                specMap.put(key,set);
            }
        }
        return specMap;
    }

    /**
     * 搜索条件
     * @param searchMap
     * @return
     */
    private NativeSearchQueryBuilder buildBasicQuery(Map<String,String> searchMap){
        NativeSearchQueryBuilder nativeSearchQueryBuilder = new NativeSearchQueryBuilder();

        // 构建布尔查询
        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();

        // 关键字搜索
        if (searchMap != null && searchMap.size() > 0){
            String keyword = searchMap.get("keyword");
            if (!StringUtils.isEmpty(keyword)){
//                nativeSearchQueryBuilder.withQuery(QueryBuilders.matchQuery("name",keyword));
                queryBuilder.must(QueryBuilders.queryStringQuery(keyword).field("name"));
            }
        }

        //输入了分类->category
        if(!StringUtils.isEmpty(searchMap.get("category"))){
            queryBuilder.must(QueryBuilders.termQuery("categoryName",searchMap.get("category")));
        }

        //输入了品牌->brand
        if(!StringUtils.isEmpty(searchMap.get("brand"))){
            queryBuilder.must(QueryBuilders.termQuery("brandName",searchMap.get("brand")));
        }

        // 规格过滤
        for (String key : searchMap.keySet()) {
            if (key.startsWith("spec_")){
                String value = searchMap.get(key).replace("\\", "");
                queryBuilder.must(QueryBuilders.matchQuery("specMap."+key.substring(5)+".keyword",value));
            }
        }

        // 价格过滤，传入的价格区间为 0-500元，500-1000元,1000元以上..
        if (!StringUtils.isEmpty(searchMap.get("price"))){
            // 去掉字符 元，以上
            String price = searchMap.get("price").replace("元","").replace("以上","");
            // 分割价格区间
            String[] priceSplit = price.split("-");
            // 判断是一个价格还是两个
            queryBuilder.must(QueryBuilders.rangeQuery("price").gt(priceSplit[0]));
            if (priceSplit.length == 2){
                queryBuilder.must(QueryBuilders.rangeQuery("price").lt(priceSplit[1]));
            }
        }

        // 排序实现
        String sortRule = searchMap.get("sortRule");//排序规则。ASC or DESC
        String sortField = searchMap.get("sortField");// 需要排序的字段
        if (!StringUtils.isEmpty(sortField)){
            nativeSearchQueryBuilder.withSort(SortBuilders.fieldSort(sortField).order(SortOrder.fromString(sortRule)));
        }

        // 分页
        int pageNo = pageConvert(searchMap);
        int pageSize = 30;
        PageRequest pageRequest = PageRequest.of(pageNo-1,pageSize);
        nativeSearchQueryBuilder.withPageable(pageRequest);



        // 添加过滤
        nativeSearchQueryBuilder.withQuery(queryBuilder);

        return nativeSearchQueryBuilder;
    }

    private int pageConvert(Map<String, String> searchMap) {
        try {
            return Integer.parseInt(searchMap.get("pageNum"));
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return 1;
    }

    /**
     * 数据搜索
     * @param builder
     * @return
     */
    private Map searchList(NativeSearchQueryBuilder builder){
        Map<String,Object> resultMap = new HashMap<>();
        // 高亮域配置
        HighlightBuilder.Field highlightBuilder = new HighlightBuilder.Field("name");
        highlightBuilder.preTags("<span style=\"color:red\">");
        highlightBuilder.postTags("</span>");
        highlightBuilder.fragmentSize(100); // 碎片长度，展示高亮关键词左右的长度
        // 添加高亮域
        builder.withHighlightFields(highlightBuilder);
        NativeSearchQuery nativeSearchQuery = builder.build();
        // 根据条件搜索得到商品列表
//        AggregatedPage<SkuInfo> skuInfos = elasticsearchTemplate.queryForPage(builder.build(), SkuInfo.class);
        AggregatedPage<SkuInfo> skuInfos = elasticsearchTemplate.queryForPage(nativeSearchQuery, SkuInfo.class,new SearchResultMapper() {
            @Override
            public <T> AggregatedPage<T> mapResults(SearchResponse response, Class<T> clazz, Pageable pageable) {
                List<T> list = new ArrayList<>();
                // 获取结果集数据，
                // 找到需要高亮的数据转化
                for (SearchHit hit : response.getHits()) {
                    // 获取非高亮数据
                    String skuStr = hit.getSourceAsString();
                    SkuInfo skuInfo = JSON.parseObject(skuStr,SkuInfo.class);
                    // 获取高亮数据
                    HighlightField name = hit.getHighlightFields().get("name");
                    if (name != null){
                        StringBuffer buffer = new StringBuffer();
                        for (Text text : name.getFragments()) {
                            buffer.append(text.toString());
                        }
                        skuInfo.setName(buffer.toString());
                    }
                list.add((T) skuInfo);
                }
                // 返回高亮数据
                return new AggregatedPageImpl<T>(list,pageable,response.getHits().getTotalHits());
            }
        });

        // 总记录数
        long totalElements = skuInfos.getTotalElements();
        // 总页数
        int totalPages = skuInfos.getTotalPages();

        // 集合数据
        List<SkuInfo> skuInfoList = skuInfos.getContent();

        resultMap.put("rows",skuInfoList);
        resultMap.put("total",totalElements);
        resultMap.put("totalPages",totalPages);
        return resultMap;
    }

    /***
     * 搜索分组数据
     */
    public Map<String, Object> searchGroupList(NativeSearchQueryBuilder builder,Map<String,String> searchMap){
        // 指定分类域，并根据分类域聚合查询
        if(searchMap==null || StringUtils.isEmpty(searchMap.get("category"))){
            builder.addAggregation(AggregationBuilders.terms("skuCategory").field("categoryName"));
        }
        if(searchMap==null || StringUtils.isEmpty(searchMap.get("brand"))){
            builder.addAggregation(AggregationBuilders.terms("skuBrand").field("brandName"));
        }
        builder.addAggregation(AggregationBuilders.terms("skuSpec").field("spec.keyword"));

        AggregatedPage<SkuInfo> skuInfos = elasticsearchTemplate.queryForPage(builder.build(), SkuInfo.class);

        //定义一个Map，存储所有分组结果
        Map<String,Object> groupMapResult = new HashMap<String,Object>();
        Aggregations aggregations = skuInfos.getAggregations();

        if(searchMap==null || StringUtils.isEmpty(searchMap.get("category"))){
            StringTerms categoryTerms = aggregations.get("skuCategory");
            List<String> categoryList = getSearchList(categoryTerms);
            groupMapResult.put("categoryList",categoryList);
        }
        if(searchMap==null || StringUtils.isEmpty(searchMap.get("brand"))){
            StringTerms brandTerms = aggregations.get("skuBrand");
            List<String> brandList = getSearchList(brandTerms);
            groupMapResult.put("brandList",brandList);
        }
        StringTerms specTerms = aggregations.get("skuSpec");
        List<String> specList = getSearchList(specTerms);
        Map<String, Set<String>> specMap = specPutAll(specList);
        groupMapResult.put("specList",specMap);
        return groupMapResult;
    }

    private List<String> getSearchList(StringTerms terms) {
        List<String> categoryList = new ArrayList<>();
        for (StringTerms.Bucket bucket : terms.getBuckets()) {
            categoryList.add(bucket.getKeyAsString());
        }
        return categoryList;
    }

}
