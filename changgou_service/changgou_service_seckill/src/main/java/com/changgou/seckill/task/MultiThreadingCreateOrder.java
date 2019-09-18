package com.changgou.seckill.task;

import com.alibaba.fastjson.JSON;
import com.changgou.seckill.dao.SeckillGoodsMapper;
import com.changgou.seckill.pojo.SeckillGoods;
import com.changgou.seckill.pojo.SeckillOrder;
import com.changgou.seckill.pojo.SeckillStatus;
import entity.IdWorker;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

@Component
public class MultiThreadingCreateOrder {
    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;

    @Autowired
    private IdWorker idWorker;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /***
     * 多线程下单操作
     */
    @Async
    public void createOrder(){
        try {
            SeckillStatus seckillStatus = (SeckillStatus) redisTemplate.boundListOps("SeckillOrderQueue").rightPop();
            if (seckillStatus == null){
                return;
            }
            // 查看当前商品是否有库存
            String time = seckillStatus.getTime();
            Long id = seckillStatus.getGoodsId();
            String username = seckillStatus.getUsername();

            // 采用队列的形式取出
            Object o = redisTemplate.boundListOps("SeckillGoodsCountList_" + id).rightPop();
            if (o == null){
                // 说明没有库存,清空排队和重复排队信息
                clearUserQueue(username);
                throw new RuntimeException("商品不存在或已售罄");
            }

            SeckillGoods seckillGoods = (SeckillGoods) redisTemplate.boundHashOps("SeckillGoods_" + time).get(id);

            if (seckillGoods == null || seckillGoods.getStockCount() <= 0){
                throw new RuntimeException("商品不存在或已售罄");
            }
            // 新建订单
            SeckillOrder seckillOrder = new SeckillOrder();
            seckillOrder.setId(idWorker.nextId());
            seckillOrder.setSeckillId(id);
            seckillOrder.setMoney(seckillGoods.getCostPrice());
            seckillOrder.setUserId(username);
            seckillOrder.setCreateTime(new Date());
            seckillOrder.setStatus("0"); // 0:未支付

            // 将订单存入redis
            redisTemplate.boundHashOps("SeckillOrder").put(username,seckillOrder);

            // 库存减少,采用redis来控制，保证精确性
            Long seckillGoodsCount = redisTemplate.boundHashOps("SeckillGoodsCount").increment(seckillGoods.getId(), -1);
            seckillGoods.setStockCount(seckillGoodsCount.intValue());

            Long size = redisTemplate.boundListOps("SeckillGoodsCountList_" + seckillGoods.getId()).size();

            // 判断当前商品是否还有库存
            if (seckillGoodsCount <= 0){
                // 库存不足,清空redis中这类商品，同步数量到数据库中
                seckillGoods.setStockCount(size.intValue());
                redisTemplate.boundHashOps("SeckillGoods_" + time).delete(id);
                seckillGoodsMapper.updateByPrimaryKeySelective(seckillGoods);
            }else {
                // 还有库存，则更新redis中即可
                redisTemplate.boundHashOps("SeckillGoods_" + time).put(id,seckillGoods);
            }

            // 抢单成功，更新状态
            seckillStatus.setStatus(2);
            seckillStatus.setOrderId(seckillOrder.getId());
            seckillStatus.setMoney(Float.valueOf(seckillOrder.getMoney()));
            redisTemplate.boundHashOps("UserQueueStatus").put(username,seckillStatus);

            // 发送消息给延时队列
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
            System.out.println("下单时间："+simpleDateFormat.format(new Date()));

            rabbitTemplate.convertAndSend("delaySeckillQueue", (Object) JSON.toJSONString(seckillStatus), new MessagePostProcessor() {
                @Override
                public Message postProcessMessage(Message message) throws AmqpException {
                    message.getMessageProperties().setExpiration("10000");
                    return message;
                }
            });
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    /**
     * 清楚redis中排队和重复排队的自增
     * @param username
     */
    public void clearUserQueue(String username){
        // 排队标识
        redisTemplate.boundHashOps("UserQueueCount").delete(username);
        // 排队信息
        redisTemplate.boundHashOps("UserQueueStatus").delete(username);
    }
}
