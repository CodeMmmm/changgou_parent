package com.changgou.item.service.impl;

import com.alibaba.fastjson.JSON;
import com.changgou.goods.feign.CategoryFeign;
import com.changgou.goods.feign.SkuFeign;
import com.changgou.goods.feign.SpuFeign;
import com.changgou.goods.pojo.Sku;
import com.changgou.goods.pojo.Spu;
import com.changgou.item.service.PageService;
import entity.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PageServiceImpl implements PageService{

    @Autowired
    private SpuFeign spuFeign;

    @Autowired
    private SkuFeign skuFeign;

    @Autowired
    private CategoryFeign categoryFeign;

    @Autowired
    private TemplateEngine templateEngine;

    /*@Value("${pagepath}")
    private String path;*/

    /**
     * 生成静态页
     * @param spuId
     */
    @Override
    public void createPageHtml(Long spuId) {

        // 容器对象context存入数据模型
        Context context = new Context();
        Map<String,Object> dataModel = buildDataModel(spuId);
        context.setVariables(dataModel);

        // 文件writer对象，配置页面保存路径
        String path = PageServiceImpl.class.getResource("/").getPath()+"/templates/items";
        File dir = new File(path);
        if (!dir.exists()){
            dir.mkdirs();
        }
        try (PrintWriter writer = new PrintWriter(new File(dir, spuId + ".html"),"UTF-8")) {
            // 生成页面
            templateEngine.process("item", context, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private Map<String,Object> buildDataModel(Long spuId) {
        Map<String,Object> dataMap = new HashMap<>();
        // 获取spu
        Result<Spu> spuResult = spuFeign.findById(spuId);
        Spu spu = spuResult.getData();
        dataMap.put("spu",spu);

        // 获取分类数据
        dataMap.put("category1",categoryFeign.findById(spu.getCategory1Id()).getData());
        dataMap.put("category2",categoryFeign.findById(spu.getCategory2Id()).getData());
        dataMap.put("category3",categoryFeign.findById(spu.getCategory3Id()).getData());

        // 获取图片集
        if (spu.getImages() != null){
            dataMap.put("imageList",spu.getImages().split(","));
        }

        // 所有可选规格
        if (spu.getSpecItems() != null){
            dataMap.put("specificationList", JSON.parseObject(spu.getSpecItems(),Map.class));
        }

        // sku集合
        Sku sku = new Sku();
        sku.setSpuId(spuId);
        Result<List<Sku>> skuList = skuFeign.findList(sku);
        dataMap.put("skuList",skuList.getData());

        return dataMap;
    }
}
