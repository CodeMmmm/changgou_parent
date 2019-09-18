package com.changgou.order.service;

import com.changgou.order.pojo.OrderItem;

import java.util.List;

public interface CartService {
    /***
     * 添加购物车
     * @param num:购买商品数量
     * @param id：购买ID
     * @param username：购买用户
     * @return
     */
    void add(Integer num, Long id, String username);

    /***
     * 查询用户的购物车数据
     * @param username
     * @return
     */
    List<OrderItem> list(String username);
}


