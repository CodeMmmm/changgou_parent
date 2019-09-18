package com.changgou.order.mq.listener;


import com.alibaba.fastjson.JSON;
import com.changgou.order.service.OrderService;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.util.Map;

@Component
@RabbitListener(queues = "${mq.pay.queue.order}")
public class OrderMessageListener {
    @Autowired
    private OrderService orderService;

    @RabbitHandler
    public void getMessage(String message) throws ParseException {
        Map<String,String> map = JSON.parseObject(message, Map.class);
        // 返回状态码
        String return_code = map.get("return_code");
        // 业务结果
        String result_code = map.get("result_code");
        // 支付结果通知成功
        if ("SUCCESS".equals(return_code)){
            System.out.println("监听到的支付消息:"+map);
            // 支付结果
            if ("SUCCESS".equals(result_code)){
                // 支付成功,修改订单状态: 参数：商品订单号，支付完成时间，微信支付订单号
                orderService.updateStatus(map.get("out_trade_no"),map.get("time_end"),map.get("transaction_id"));
//                System.out.println("ok");
            }else {
                // 支付失败，先关闭支付。再逻辑删除订单，库存回滚

                orderService.deleteOrder(map.get("out_trade_no"));
            }
        }
    }
}
