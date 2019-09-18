package com.changgou.seckill.mq;

import com.alibaba.fastjson.JSON;
import com.changgou.seckill.pojo.SeckillStatus;
import com.changgou.seckill.service.SeckillOrderService;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

@Component
@RabbitListener(queues = "seckillQueue")
public class DelaySeckillMessageListener {

    @Autowired
    private SeckillOrderService seckillOrderService;

    @Autowired
    private RedisTemplate redisTemplate;

    @RabbitHandler
    public void getMessage(String message){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
        System.out.println("回滚时间："+simpleDateFormat.format(new Date()));

        //获取用户的排队信息
        SeckillStatus seckillStatus = JSON.parseObject(message,SeckillStatus.class);

        Object userQueueStatus = redisTemplate.boundHashOps("UserQueueStatus").get(seckillStatus.getUsername());
        // 判断，为空则说明订单已经支付，不为空则说明未支付
        if (userQueueStatus != null){
            // 先关闭微信支付
            // 再关闭订单
            seckillOrderService.closeOrder(seckillStatus.getUsername());
        }
    }
}
