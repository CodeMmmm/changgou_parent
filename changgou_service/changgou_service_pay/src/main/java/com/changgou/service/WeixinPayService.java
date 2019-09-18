package com.changgou.service;

import java.util.Map;

public interface WeixinPayService {
    /*****
     * 创建二维码
     * @param map : 参数数组
     * @return
     */
    public Map createNative(Map<String,String> map);

    /***
     * 查询订单状态
     * @param out_trade_no : 客户端自定义订单编号
     * @return
     */
    public Map queryPayStatus(String out_trade_no);
}
