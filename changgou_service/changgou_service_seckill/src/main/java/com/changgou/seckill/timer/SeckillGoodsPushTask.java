package com.changgou.seckill.timer;

import com.changgou.seckill.dao.SeckillGoodsMapper;
import com.changgou.seckill.pojo.SeckillGoods;
import entity.DateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tk.mybatis.mapper.entity.Example;

import java.util.Date;
import java.util.List;
import java.util.Set;

@Component
public class SeckillGoodsPushTask {
    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    /****
     * 每5秒执行一次,符合秒杀的商品存入redis
     */
    @Scheduled(cron = "0/5 * * * * ?")
    public void loadGoodsPushRedis(){
        // 计算秒杀时间段
        List<Date> dateMenus = DateUtil.getDateMenus();
        for (Date startTime : dateMenus) {
            String timespace ="SeckillGoods_"+ DateUtil.data2str(startTime,"yyyyMMddHH");
            Example example = new Example(SeckillGoods.class);
            Example.Criteria criteria = example.createCriteria();
            // 审核状态通过 为1
            criteria.andEqualTo("status","1");
            // 商品库存个数大于0
            criteria.andGreaterThan("stockCount",0);
            // 结束时间大于现在时间且在两小时之内,开始时间小于现在时间
            criteria.andGreaterThanOrEqualTo("startTime",startTime);
            criteria.andLessThanOrEqualTo("endTime",DateUtil.addDateHour(startTime,2));
            // redis中没有该商品的缓存
            Set keys = redisTemplate.boundHashOps(timespace).keys();
            if (keys != null && keys.size() > 0){
                criteria.andNotIn("id",keys);
            }

            // 查找出符合条件的所有商品
            List<SeckillGoods> seckillGoodsList = seckillGoodsMapper.selectByExample(example);


            for (SeckillGoods seckillGoods : seckillGoodsList) {
                // 将商品的id存入队列
                redisTemplate.boundListOps("SeckillGoodsCountList_"+seckillGoods.getId()).leftPushAll(putAllIds(seckillGoods.getStockCount(),seckillGoods.getId()));
                // 将商品库存存入，因为内存中控制数量容易导致不精确，所以采用redis
                redisTemplate.boundHashOps("SeckillGoodsCount").increment(seckillGoods.getId(),seckillGoods.getStockCount());
                // 存入redis中
                redisTemplate.boundHashOps(timespace).put(seckillGoods.getId(),seckillGoods);
            }
        }
    }

    public Long[] putAllIds(Integer num,Long id){
        Long[] ids = new Long[num];
        for (int i=0;i<ids.length;i++){
            ids[i] = id;
        }
        return ids;
    }
}
