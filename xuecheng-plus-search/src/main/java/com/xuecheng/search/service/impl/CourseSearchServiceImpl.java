package com.xuecheng.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.search.dto.SearchCourseParamDto;
import com.xuecheng.search.dto.SearchPageResultDto;
import com.xuecheng.search.po.CourseIndex;
import com.xuecheng.search.service.CourseSearchService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.TotalHits;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @description 课程搜索service实现类
 */
@Slf4j
@Service
public class CourseSearchServiceImpl implements CourseSearchService {

    @Value("${elasticsearch.course.index}")
    private String courseIndexStore;
    @Value("${elasticsearch.course.source_fields}")
    private String sourceFields;

    @Autowired
    RestHighLevelClient client;

    @Override
    public SearchPageResultDto<CourseIndex> queryCoursePubIndex(PageParams pageParams, SearchCourseParamDto courseSearchParam) {

        //设置索引
        SearchRequest searchRequest = new SearchRequest(courseIndexStore);

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        //source源字段过虑
        String[] sourceFieldsArray = sourceFields.split(",");
        //设置搜索的结果中只包含sourceFieldsArray数组中指定的字段，不排除任何字段。
        //第一个参数是包含哪些字段，第二个参数是排除哪些字段
        searchSourceBuilder.fetchSource(sourceFieldsArray, new String[]{});
        if (courseSearchParam == null) {
            courseSearchParam = new SearchCourseParamDto();
        }
        //关键字匹配
        if (StringUtils.isNotEmpty(courseSearchParam.getKeywords())) {
            //匹配关键字 构建多字段的匹配查询 fieldName是要匹配的字段
            MultiMatchQueryBuilder multiMatchQueryBuilder = QueryBuilders.multiMatchQuery(courseSearchParam.getKeywords(), "name", "description");
            //设置匹配占比
            multiMatchQueryBuilder.minimumShouldMatch("70%");
            //提升另个字段的Boost值 相关性评分 在原评分的基础上*boots
//            multiMatchQueryBuilder.field("name", 10);
            //布尔查询 must：必须匹配每个子查询
            boolQueryBuilder.must(multiMatchQueryBuilder);
        }
        //过虑 filter部分，符合该条件的文档才会重新算分
        if (StringUtils.isNotEmpty(courseSearchParam.getMt())) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("mtName", courseSearchParam.getMt()));
        }
        if (StringUtils.isNotEmpty(courseSearchParam.getSt())) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("stName", courseSearchParam.getSt()));
        }
        if (StringUtils.isNotEmpty(courseSearchParam.getGrade())) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("grade", courseSearchParam.getGrade()));
        }
        //分页
        Long pageNo = pageParams.getPageNo();
        Long pageSize = pageParams.getPageSize();
        int start = (int) ((pageNo - 1) * pageSize);
        //指从多少行开始，并不是多少页 默认为0
        searchSourceBuilder.from(start);
        //指定每页多少个数据 Math.toIntExact(pageSize)：long值转化为int值
        //注意：不能超过int值的范围，并且from和size不能相差过大
        searchSourceBuilder.size(Math.toIntExact(pageSize));
        //排序条件 (代码需要优化)
        if (StringUtils.isNotEmpty(courseSearchParam.getSortType())) {
            if ("1".equals(courseSearchParam.getSortType())) {
                searchSourceBuilder.sort("price", SortOrder.ASC);
            } else {
                searchSourceBuilder.sort("price", SortOrder.DESC);
            }
        }

//        searchSourceBuilder.searchAfter(new Object[]{});
        //布尔查询
        searchSourceBuilder.query(boolQueryBuilder);
        //高亮设置
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.preTags("<font class='eslight'>");//前标签
        highlightBuilder.postTags("</font>");//后标签
        //设置高亮字段  是否需要对所有查询字段进行匹配
        highlightBuilder.fields().add(new HighlightBuilder.Field("name").requireFieldMatch(false));
        searchSourceBuilder.highlighter(highlightBuilder);
        //请求搜索
        searchRequest.source(searchSourceBuilder);
        //聚合设置 创建一个聚合操作
        buildAggregation(searchRequest);
        SearchResponse searchResponse = null;
        try {
            searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
            log.error("课程搜索异常：{}", e.getMessage());
            return new SearchPageResultDto<CourseIndex>(new ArrayList(), 0, 0, 0);
        }


        //结果集处理
        SearchHits hits = searchResponse.getHits();
        SearchHit[] searchHits = hits.getHits();
        //记录总数
        TotalHits totalHits = hits.getTotalHits();
        //数据列表
        List<CourseIndex> list = new ArrayList<>();

        for (SearchHit hit : searchHits) {

            String sourceAsString = hit.getSourceAsString();
            CourseIndex courseIndex = JSON.parseObject(sourceAsString, CourseIndex.class);

            //取出source
            Map<String, Object> sourceAsMap = hit.getSourceAsMap();

            //课程id
            Long id = courseIndex.getId();
            //取出名称
            String name = courseIndex.getName();
            //取出高亮字段内容
            Map<String, HighlightField> highlightFields = hit.getHighlightFields();
            if (highlightFields != null) {
                HighlightField nameField = highlightFields.get("name");
                if (nameField != null) {
                    Text[] fragments = nameField.getFragments();
                    StringBuffer stringBuffer = new StringBuffer();
                    for (Text str : fragments) {
                        stringBuffer.append(str.string());
                    }
                    name = stringBuffer.toString();

                }
            }
            courseIndex.setId(id);
            courseIndex.setName(name);

            list.add(courseIndex);

        }
        SearchPageResultDto<CourseIndex> pageResult = new SearchPageResultDto<>(list, totalHits.value, pageNo, pageSize);

        //获取聚合结果
        List<String> mtList = getAggregation(searchResponse.getAggregations(), "mtAgg");
        List<String> stList = getAggregation(searchResponse.getAggregations(), "stAgg");

        pageResult.setMtList(mtList);
        pageResult.setStList(stList);

        return pageResult;
    }


    private void buildAggregation(SearchRequest request) {
        request.source().aggregation(AggregationBuilders
                .terms("mtAgg")//聚合类型 与 聚合名称
                .field("mtName")//聚合字段
                .size(100)//聚合数量
        );
        request.source().aggregation(AggregationBuilders
                .terms("stAgg")
                .field("stName")
                .size(100)
        );

    }

    private List<String> getAggregation(Aggregations aggregations, String aggName) {
        // 4.1.根据聚合名称获取聚合结果
        Terms brandTerms = aggregations.get(aggName);
        // 4.2.获取buckets
        List<? extends Terms.Bucket> buckets = brandTerms.getBuckets();
        // 4.3.遍历
        List<String> brandList = new ArrayList<>();
        for (Terms.Bucket bucket : buckets) {
            // 4.4.获取key
            String key = bucket.getKeyAsString();
            brandList.add(key);
        }
        return brandList;
    }

    @Override
    public List<String> getSuggestions(String prefix) {
        try {
            // 1.准备Request
            SearchRequest request = new SearchRequest(courseIndexStore);
            // 2.准备DSL
            request.source().suggest(new SuggestBuilder().addSuggestion(
                    "suggestions",
                    SuggestBuilders.completionSuggestion("suggestion")
                            .prefix(prefix)
                            .skipDuplicates(true)
                            .size(10)
            ));
            // 3.发起请求
            SearchResponse response = client.search(request, RequestOptions.DEFAULT);
            // 4.解析结果
            Suggest suggest = response.getSuggest();
            // 4.1.根据补全查询名称，获取补全结果
            CompletionSuggestion suggestions = suggest.getSuggestion("suggestions");
            System.out.println(suggestions);
            // 4.2.获取options
            List<CompletionSuggestion.Entry.Option> options = suggestions.getOptions();
            // 4.3.遍历
            List<String> list = new ArrayList<>(options.size());
            for (CompletionSuggestion.Entry.Option option : options) {
                String text = option.getText().toString();
                list.add(text);
            }
            return list;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
