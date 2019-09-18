package com.changgou.seckill.mq;

import com.alibaba.fastjson.JSON;
import com.changgou.seckill.service.SeckillOrderService;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RabbitListener(queues = "${mq.pay.queue.seckillorder}")
public class SeckillMessageListener {

    @Autowired
    private SeckillOrderService seckillOrderService;

    @RabbitHandler
    public void getMessage(String message){
        System.out.println(message);
        //将支付信息转成Map
        Map<String,String> resultMap = JSON.parseObject(message,Map.class);
        //return_code->通信标识-SUCCESS
        String return_code = resultMap.get("return_code");
        //out_trade_no->订单号
        String outtradeno = resultMap.get("out_trade_no");
        //自定义数据
        String attach = resultMap.get("attach");
        Map<String,String> attachMap = JSON.parseObject(attach,Map.class);

        if ("SUCCESS".equals(return_code)){
            //result_code->业务结果-SUCCESS->改订单状态
            String result_code = resultMap.get("result_code");
            if ("SUCCESS".equals(result_code)){
                // 修改订单状态为已支付
                seckillOrderService.updatePayStatus(resultMap.get("time_end"),resultMap.get("transaction_id"),attachMap.get("username"));
            }else {
                // 支付失败，删除订单
                seckillOrderService.closeOrder(attachMap.get("username"));
            }
        }else {
            throw new RuntimeException("微信服务器炸了");
        }

    }
}
