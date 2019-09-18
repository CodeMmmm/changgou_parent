package com.changgou.search.controller;

import com.changgou.search.feign.SkuFeign;
import com.changgou.search.pojo.SkuInfo;
import entity.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@Controller
@RequestMapping(value = "/search")
public class SkuController {

    @Autowired
    private SkuFeign skuFeign;

    /**
     * 将特殊关键字切换
     * @param searchMap
     */
    public void handlerSearchMap(Map<String,String> searchMap){
        if (searchMap != null){
            for (Map.Entry<String, String> entry : searchMap.entrySet()) {
                if (entry.getKey().startsWith("spec_")){
                    entry.setValue(entry.getValue().replace("+","%2B"));
                }
            }
        }
    }

    /**
     * 搜索
     * @param searchMap
     * @return
     */
    @GetMapping(value = "/list")
    public String search(@RequestParam(required = false) Map<String,String> searchMap, Model model) {
        // 转换关键字
        handlerSearchMap(searchMap);
        //调用changgou-service-search微服务
        Map resultMap = skuFeign.search(searchMap);
        model.addAttribute("result", resultMap);
        model.addAttribute("searchMap",searchMap);
        String[] urls = url(searchMap);
        model.addAttribute("url",urls[0]);
        model.addAttribute("sortUrl",urls[1]);

        // 分页计算
        Page<SkuInfo> page = new Page<SkuInfo>(
            Long.parseLong(resultMap.get("total").toString()),
            Integer.parseInt(resultMap.get("pageNum").toString())+1,
            Integer.parseInt(resultMap.get("pageSize").toString())
        );
        model.addAttribute("page",page);

        return "search";
    }

    private String[] url(Map<String,String> searchMap) {
        String url = "/search/list";
        String sortUrl = "/search/list";
        if (searchMap != null || searchMap.size() > 0){
            url += "?";
            sortUrl += "?";
            for (String key : searchMap.keySet()) {
                // 跳过分页
                if (key.equals("pageNum")){
                    continue;
                }
                url += key + "=" + searchMap.get(key) + "&";
                // 跳过排序
                if (key.equals("sortField") || key.equals("sortRule")){
                    continue;
                }
                sortUrl += key + "=" + searchMap.get(key) + "&";
            }

            // 去掉最后一个&
            url = url.substring(0,url.length()-1);
        }
        return new String[]{url,sortUrl};
    }
}