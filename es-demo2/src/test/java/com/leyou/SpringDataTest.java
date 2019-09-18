package com.leyou;

import com.alibaba.fastjson.JSON;
import com.leyou.pojo.Goods;
import com.leyou.repository.GoodsRepository;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.SearchResultMapper;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest
public class SpringDataTest {

    @Autowired
    private ElasticsearchTemplate esTemplate;

    //根据Goods中的注解的配置生成索引库
    @Test
    public void testAddIndex(){
        esTemplate.createIndex(Goods.class);
    }

    //根据Goods中的注解的配置生成mapping映射
    @Test
    public void testAddMapping(){
        esTemplate.putMapping(Goods.class);
    }

    @Autowired
    private GoodsRepository goodsRepository;

    @Test
    public void testAddDoc(){
        Goods goods = new Goods("1","小米9手机","手机","小米",1199.0,"q3311");
        goodsRepository.save(goods);//有新增和修改的功能
    }

    //批量新增
    @Test
    public void testAddBulDoc(){
        List<Goods> list = new ArrayList<>();
        list.add(new Goods("1", "小米手机7", "手机", "小米", 3299.00,"http://image.leyou.com/13123.jpg"));
        list.add(new Goods("2", "坚果手机R1", "手机", "锤子", 3699.00,"http://image.leyou.com/13123.jpg"));
        list.add(new Goods("3", "华为META10", "手机", "华为", 4499.00,"http://image.leyou.com/13123.jpg"));
        list.add(new Goods("4", "小米Mix2S", "手机", "小米", 4299.00, "http://image.leyou.com/13123.jpg"));
        list.add(new Goods("5", "荣耀V10", "手机", "华为", 2799.00,"http://image.leyou.com/13123.jpg"));
        goodsRepository.saveAll(list);
    }

    @Test
    public void testDeleteDoc(){
//        goodsRepository.deleteAll();//慎用
        goodsRepository.deleteById("1");
    }

    @Test
    public void testSearch(){
//        List<Goods> goodsList = goodsRepository.findByTitle("小米");
//        List<Goods> goodsList = goodsRepository.findByBrand("小米");
//        List<Goods> goodsList = goodsRepository.findByBrandAndPriceBetween("小米",2000.0,5000.0);
        List<Goods> goodsList = goodsRepository.findByBrandOrPriceBetween("小米",2000.0,5000.0);
        goodsList.forEach(goods -> {
            System.out.println(goods);
        });
    }

    @Test
    public void testQuery(){
        NativeSearchQueryBuilder nativeSearchQueryBuilder = new NativeSearchQueryBuilder();
//        nativeSearchQueryBuilder.withQuery(QueryBuilders.termQuery("title","华为"));
        nativeSearchQueryBuilder.withQuery(QueryBuilders.matchAllQuery());//查询所有

        nativeSearchQueryBuilder.withPageable(PageRequest.of(0,2));//分页

//        聚合条件
        nativeSearchQueryBuilder.addAggregation(AggregationBuilders.terms("brandCount").field("brand"));

        AggregatedPage<Goods> aggregatedPage = esTemplate.queryForPage(nativeSearchQueryBuilder.build(), Goods.class);

        //获取聚合的结果
        Terms terms = aggregatedPage.getAggregations().get("brandCount");
        List<? extends Terms.Bucket> buckets = terms.getBuckets();
        buckets.forEach(bucket->{
            System.out.println(bucket.getKeyAsString()+":"+bucket.getDocCount());
        });


//        List<Goods> goodsList = aggregatedPage.getContent();//获取数据的集合
//        goodsList.forEach(goods -> {
//            System.out.println(goods);
//        });

    }

    public class SearchResultMapperImpl implements SearchResultMapper{

        @Override
        public <T> AggregatedPage<T> mapResults(SearchResponse response, Class<T> clazz, Pageable pageable) {

            long total = response.getHits().getTotalHits();//返回时需要的参数
            Aggregations aggregations = response.getAggregations();//返回时需要的参数
            String scrollId = response.getScrollId();//返回时需要的参数
            float maxScore = response.getHits().getMaxScore();//返回时需要的参数

//            处理我们想要的高亮结果
            SearchHit[] hits = response.getHits().getHits();
            List<T> content = new ArrayList<>();
            for (SearchHit hit : hits) {
                String jsonString = hit.getSourceAsString();
                T t = JSON.parseObject(jsonString, clazz);

                Map<String, HighlightField> highlightFields = hit.getHighlightFields();
                HighlightField highlightField = highlightFields.get("title");
                Text[] fragments = highlightField.getFragments();
                if (fragments != null && fragments.length>0) {
                    String title = fragments[0].toString();

                    try {
                        //把T对象中的"title"属性的值替换成title
                        BeanUtils.copyProperties(t,"title",title);
                    } catch (Exception e) {
//                        e.printStackTrace();
                        System.out.println("SSSSSS");
                    }
                }
                content.add(t);
            }
            return new AggregatedPageImpl<>(content,pageable,total,aggregations,scrollId,maxScore);
        }
    }
}
