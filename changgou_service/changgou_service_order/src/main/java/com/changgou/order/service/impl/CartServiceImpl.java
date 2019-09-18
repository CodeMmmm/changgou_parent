package com.changgou.order.service.impl;

import com.changgou.goods.pojo.Sku;
import com.changgou.goods.pojo.Spu;
import com.changgou.goods.feign.SkuFeign;
import com.changgou.goods.feign.SpuFeign;
import com.changgou.order.pojo.OrderItem;
import com.changgou.order.service.CartService;
import entity.Result;
import entity.StatusCode;
import entity.TokenDecode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class CartServiceImpl implements CartService{
    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private SkuFeign skuFeign;

    @Autowired
    private SpuFeign spuFeign;

    /***
     * 加入购物车
     * @param num:购买商品数量
     * @param id：购买ID
     * @param username：购买用户
     * @return
     */
    @Override
    public void add(Integer num, Long id, String username) {
       // 当商品数量为小于等于0的时候，移除上坪信息
        if (num <= 0){
            redisTemplate.boundHashOps("Cart_" + username).delete(id);
            // 当购物车没东西，也删除购物车
            Long size = redisTemplate.boundHashOps("Cart_" + username).size();
            if (size == null || size <= 0){
                redisTemplate.delete("Cart_" + username);
            }
            return;
        }
        // 查找商品
        Result<Sku> skuResult = skuFeign.findById(id);
        if (skuResult != null && skuResult.isFlag()){
            Sku sku = skuResult.getData();
            // 查找spu
            Result<Spu> spuResult = spuFeign.findById(sku.getSpuId());

            // 转换对象为orderItem
            OrderItem orderItem = createOrderItem(num, id, sku, spuResult.getData());

            // 存入redis
            redisTemplate.boundHashOps("Cart_" + username).put(id,orderItem);

        }
    }

    /***
     * 查询用户购物车数据
     * @param username
     * @return
     */
    @Override
    public List<OrderItem> list(String username) {
        return redisTemplate.boundHashOps("Cart_"+username).values();
    }

    /***
     * 创建一个OrderItem对象
     * @param num
     * @param id
     * @param sku
     * @param spu
     * @return
     */
    public OrderItem createOrderItem(Integer num, Long id, Sku sku, Spu spu) {
        //将加入购物车的商品信息封装成OrderItem
        OrderItem orderItem = new OrderItem();
        orderItem.setCategoryId1(spu.getCategory1Id());
        orderItem.setCategoryId2(spu.getCategory2Id());
        orderItem.setCategoryId3(spu.getCategory3Id());
        orderItem.setSpuId(Long.valueOf(spu.getId().toString()));
        orderItem.setSkuId(id);
        orderItem.setName(sku.getName());
        orderItem.setPrice(sku.getPrice());
        orderItem.setNum(num);
        orderItem.setMoney(num*orderItem.getPrice());
        orderItem.setImage(spu.getImage());
        return orderItem;
    }
}
