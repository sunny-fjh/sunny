package com.leyou.repository;

import com.leyou.pojo.Goods;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface GoodsRepository extends ElasticsearchRepository<Goods,String> {

    List<Goods> findByTitle(String title);

    List<Goods> findByBrand(String brand);

    List<Goods> findByPriceBetween(double v, double v1);

    List<Goods> findByBrandAndPriceBetween(String brand, double v, double v1);

    List<Goods> findByBrandOrPriceBetween(String brand, double v, double v1);

}
