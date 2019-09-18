package com.changgou.service.impl;

import com.alibaba.fastjson.JSON;
import com.changgou.service.WeixinPayService;
import com.github.wxpay.sdk.WXPayUtil;
import entity.HttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Service
public class WeixinPayServiceImpl implements WeixinPayService{
    @Value("${weixin.appid}")
    private String appid;

    @Value("${weixin.partner}")
    private String partner;

    @Value("${weixin.partnerkey}")
    private String partnerkey;

    @Value("${weixin.notifyurl}")
    private String notifyurl;


    /**
     * 获取预支付信息，创建二维码
     * @param map : 参数map
\     * @return
     */
    @Override
    public Map createNative(Map<String,String> map) {
        try {
            String out_trade_no = map.get("out_trade_no"); //商户订单号
            String total_fee = map.get("money"); //交易金额
            Map<String,String> paramMap = new HashMap<>();
            // 公众号id
            paramMap.put("appid",appid);
            // 商户id
            paramMap.put("mch_id",partner);
            // 随机字符串
            paramMap.put("nonce_str",appid);
            // 商品描述
            paramMap.put("body","畅购商品销售");
            paramMap.put("out_trade_no",out_trade_no);// 商户订单号
            paramMap.put("total_fee",total_fee);// 标价金额
            paramMap.put("spbill_create_ip","127.0.0.1");// 终端IP
            paramMap.put("notify_url",notifyurl);// 通知地址
            paramMap.put("trade_type","NATIVE ");// 交易类型

            // 自定义数据：交换机以及routingkey
            String exchange = map.get("exchange");
            String routingkey = map.get("routingkey");
            Map<String,String> attachMap = new HashMap<>();
            attachMap.put("exchange",exchange);
            attachMap.put("routingkey",routingkey);
            //如果是秒杀订单，需要传username
            String username = map.get("username");
            if(!StringUtils.isEmpty(username)){
                attachMap.put("username",username);
            }
            String attach = JSON.toJSONString(attachMap);
            paramMap.put("attach",attach);

            // 将map转为xml,携带签名
            String signedXml = WXPayUtil.generateSignedXml(paramMap, partnerkey);

            // 使用http工具类发送请求，请求参数为xml字符串,携带签名
            String url = "https://api.mch.weixin.qq.com/pay/unifiedorder";
            HttpClient httpClient = new HttpClient(url);
            // 设置发送https请求
            httpClient.setHttps(true);
            // 设置请求参数
            httpClient.setXmlParam(signedXml);
            // 发送post请求
            httpClient.post();

            // 获取返回结果
            String content = httpClient.getContent();
            Map<String,String> dataMap = WXPayUtil.xmlToMap(content);
            return dataMap;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 查询订单状态
     * @param out_trade_no : 客户端自定义订单编号
     * @return
     */
    @Override
    public Map queryPayStatus(String out_trade_no) {
        try {
            Map<String,String> paramMap = new HashMap<>();
            // 公众号id
            paramMap.put("appid",appid);
            // 商户id
            paramMap.put("mch_id",partner);
            // 随机字符串
            paramMap.put("nonce_str",appid);
            paramMap.put("out_trade_no",out_trade_no);// 商户订单号

            // 将map转为xml,携带签名
            String signedXml = WXPayUtil.generateSignedXml(paramMap, partnerkey);

            String url = "https://api.mch.weixin.qq.com/pay/orderquery";
            HttpClient httpClient = new HttpClient(url);

            // 设置发送https请求
            httpClient.setHttps(true);
            // 设置请求参数
            httpClient.setXmlParam(signedXml);
            // 发送post请求
            httpClient.post();

            // 获取返回结果
            String content = httpClient.getContent();
            Map<String,String> dataMap = WXPayUtil.xmlToMap(content);
            return dataMap;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
