package com.leyou.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Item {
    private String id;     //不分词
    private String title; //标题   分词
    private String category;// 分类   不分词
    private String brand; // 品牌     不分词
    private Double price; // 价格    不分词
    private String images; // 图片地址   不分词
}
