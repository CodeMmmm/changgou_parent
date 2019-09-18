package com.changgou.goods.feign;

import com.changgou.goods.pojo.Sku;
import entity.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(name = "goods")
@RequestMapping(value = "/sku")
public interface SkuFeign {
    /***
     * 库存递减
     * @param map
     * @return
     */
    @PostMapping(value = "/decr/count")
    public Result decrCount(@RequestParam Map<String,Integer> map);


    /***
     * 查询Sku全部数据
     * @return
     */
    @GetMapping
    Result<List<Sku>> findAll();

    /***
     * 根据ID查询Sku数据
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    Result<Sku> findById(@PathVariable("id") Long id);

    /***
     * 根据审核状态查询Sku
     * @param status
     * @return
     */
    @GetMapping("/status/{status}")
    Result<List<Sku>> findByStatus(@PathVariable("status") String status);

    /**
     * 根据条件搜索
     * @param sku
     * @return
     */
    @PostMapping(value = "/search" )
    public Result<List<Sku>> findList(@RequestBody(required = false) Sku sku);
}
