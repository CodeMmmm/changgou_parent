package com.changgou.controller;

import com.alibaba.fastjson.JSON;
import com.changgou.service.WeixinPayService;
import com.github.wxpay.sdk.WXPayUtil;
import entity.Result;
import entity.StatusCode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.util.Map;

@RestController
@RequestMapping(value = "/weixin/pay")
@CrossOrigin
public class WeixinPayController {

    @Autowired
    private WeixinPayService weixinPayService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /****
     * 支付结果通知回调方法
     */
    @RequestMapping(value = "/notify/url")
    public String notifyurl(HttpServletRequest request) throws Exception{
        // 获取输入流
        ServletInputStream inputStream = request.getInputStream();
        // 读取输入流中的数据写入map中
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] bytes = new byte[1024];
        int len = 0;
        while ((len=inputStream.read(bytes)) != -1){
            baos.write(bytes,0,len);
        }

        byte[] byteArray = baos.toByteArray();
        String xmlResult = new String(byteArray,"UTF-8");
        Map<String,String> dataMap = WXPayUtil.xmlToMap(xmlResult);

        // 接收自定义数据：交换机和routingkey
        String attach = dataMap.get("attach");
        Map<String,String> attachMap = JSON.parseObject(attach, Map.class);
        String exchange = attachMap.get("exchange");
        String routingkey = attachMap.get("routingkey");

        System.out.println("回调成功了");
        // 发送消息给MQ
        rabbitTemplate.convertAndSend(exchange,routingkey, JSON.toJSONString(dataMap));

        String result = "<xml>" + "<return_code><![CDATA[SUCCESS]]></return_code>" + "<return_msg><![CDATA[OK]]></return_msg>" + "</xml>";
        return result;
    }

    /***
     * 创建二维码
     * @return
     */
    @RequestMapping(value = "/create/native")
    public Result createNative(@RequestParam Map<String,String> parameterMap){
        Map<String,String> resultMap = weixinPayService.createNative(parameterMap);
        return new Result(true, StatusCode.OK,"创建二维码预付订单成功！",resultMap);
    }

    /***
     * 微信支付状态查询
     */
    @GetMapping(value = "/status/query")
    public Result queryStatus(String outtradeno){
        //查询支付状态
        Map map = weixinPayService.queryPayStatus(outtradeno);
        return new Result(true,StatusCode.OK,"查询支付状态成功！",map);
    }
}
