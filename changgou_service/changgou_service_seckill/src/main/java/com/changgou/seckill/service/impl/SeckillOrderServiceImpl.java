package com.changgou.seckill.service.impl;

import com.changgou.seckill.dao.SeckillGoodsMapper;
import com.changgou.seckill.dao.SeckillOrderMapper;
import com.changgou.seckill.pojo.SeckillGoods;
import com.changgou.seckill.pojo.SeckillOrder;
import com.changgou.seckill.pojo.SeckillStatus;
import com.changgou.seckill.service.SeckillOrderService;
import com.changgou.seckill.task.MultiThreadingCreateOrder;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import entity.IdWorker;
import entity.StatusCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tk.mybatis.mapper.entity.Example;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/****
 * @Author:shenkunlin
 * @Description:SeckillOrder业务层接口实现类
 * @Date 2019/6/14 0:16
 *****/
@Service
public class SeckillOrderServiceImpl implements SeckillOrderService {
    @Autowired
    private MultiThreadingCreateOrder multiThreadingCreateOrder;

    @Autowired
    private SeckillOrderMapper seckillOrderMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;

    /**
     * 删除订单
     * @param username
     */
    @Override
    public void closeOrder(String username) {
        // 获得排队信息状态
        SeckillStatus seckillStatus = (SeckillStatus) redisTemplate.boundHashOps("UserQueueStatus").get(username);
        // 删除redis中订单
        redisTemplate.boundHashOps("SeckillOrder").delete(username);
        //删除排队信息
        clearUserQueue(username);

        // 库存回滚，此时redis中不一定有该类商品，所以先判断
        String namespace = "SeckillGoods_"+seckillStatus.getTime();
        SeckillGoods seckillGoods = (SeckillGoods) redisTemplate.boundHashOps(namespace).get(seckillStatus.getGoodsId());

        if (seckillGoods == null){
            // redis中没有该类商品,就从数据库中查询
            seckillGoods = seckillGoodsMapper.selectByPrimaryKey(seckillStatus.getGoodsId());
            // 库存回滚,数据库更新
            seckillGoods.setStockCount(1);
            seckillGoodsMapper.updateByPrimaryKeySelective(seckillGoods);
        }else {
            // redis中有该类商品，直接更新即可
            // 库存增加,采用redis来控制，保证精确性
            Long seckillGoodsCount = redisTemplate.boundHashOps("SeckillGoodsCount").increment(seckillGoods.getId(), 1);
            seckillGoods.setStockCount(seckillGoodsCount.intValue());
        }
        redisTemplate.boundHashOps(namespace).put(seckillGoods.getId(),seckillGoods);
        //队列
        redisTemplate.boundListOps("SeckillGoodsCountList_"+seckillGoods.getId()).leftPush(seckillGoods.getId());

    }

    /**
     * 更新秒杀订单状态
     * @param endtime
     * @param transaction_id
     * @param username
     */
    @Override
    public void updatePayStatus(String endtime, String transaction_id, String username) {
        SeckillOrder seckillOrder = (SeckillOrder) redisTemplate.boundHashOps("SeckillOrder").get(username);
        if (seckillOrder != null){
            try {
                seckillOrder.setStatus("1"); // 已支付
                seckillOrder.setTransactionId(transaction_id);
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
                Date payTimeInfo = simpleDateFormat.parse(endtime);
                seckillOrder.setPayTime(payTimeInfo);
                seckillOrderMapper.insertSelective(seckillOrder); // 更新后存入数据库中

                // 删除redis中的订单
                redisTemplate.boundHashOps("SeckillOrder").delete(username);

                // 清空redis中的排队队列等信息
                clearUserQueue(username);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }

    /***
     * 清理用户排队抢单信息
     */
    public void clearUserQueue(String username){
        //排队标识
        redisTemplate.boundHashOps("UserQueueCount").delete(username);
        //排队信息清理掉
        redisTemplate.boundHashOps("UserQueueStatus").delete(username);
    }

    @Override
    public SeckillStatus queryStatus(String username) {
        return (SeckillStatus) redisTemplate.boundHashOps("UserQueueStatus").get(username);
    }

    /**
     * 添加秒杀订单
     * @param id:商品ID
     * @param time:商品秒杀开始时间
     * @param username:用户登录名
     * @return
     */
    @Override
    public Boolean add(Long id, String time, String username) {
        // 检查用户是否重复排队,采用redis的自增。
        Long userQueueCount = redisTemplate.boundHashOps("UserQueueCount").increment(username, 1);
        if (userQueueCount > 1){
            // 100表示重复排队
            throw new RuntimeException("100");
        }
        //排队信息封装
        SeckillStatus seckillStatus = new SeckillStatus(username, new Date(),1, id,time);
        //将秒杀抢单信息存入到Redis中,这里采用List方式存储,List本身是一个队列
        redisTemplate.boundListOps("SeckillOrderQueue").leftPush(seckillStatus);
        // 将状态信息存入redis中
        redisTemplate.boundHashOps("UserQueueStatus").put(username,seckillStatus);
        multiThreadingCreateOrder.createOrder();
        return true;
    }

    /**
     * SeckillOrder条件+分页查询
     * @param seckillOrder 查询条件
     * @param page 页码
     * @param size 页大小
     * @return 分页结果
     */
    @Override
    public PageInfo<SeckillOrder> findPage(SeckillOrder seckillOrder, int page, int size){
        //分页
        PageHelper.startPage(page,size);
        //搜索条件构建
        Example example = createExample(seckillOrder);
        //执行搜索
        return new PageInfo<SeckillOrder>(seckillOrderMapper.selectByExample(example));
    }

    /**
     * SeckillOrder分页查询
     * @param page
     * @param size
     * @return
     */
    @Override
    public PageInfo<SeckillOrder> findPage(int page, int size){
        //静态分页
        PageHelper.startPage(page,size);
        //分页查询
        return new PageInfo<SeckillOrder>(seckillOrderMapper.selectAll());
    }

    /**
     * SeckillOrder条件查询
     * @param seckillOrder
     * @return
     */
    @Override
    public List<SeckillOrder> findList(SeckillOrder seckillOrder){
        //构建查询条件
        Example example = createExample(seckillOrder);
        //根据构建的条件查询数据
        return seckillOrderMapper.selectByExample(example);
    }


    /**
     * SeckillOrder构建查询对象
     * @param seckillOrder
     * @return
     */
    public Example createExample(SeckillOrder seckillOrder){
        Example example=new Example(SeckillOrder.class);
        Example.Criteria criteria = example.createCriteria();
        if(seckillOrder!=null){
            // 主键
            if(!StringUtils.isEmpty(seckillOrder.getId())){
                    criteria.andEqualTo("id",seckillOrder.getId());
            }
            // 秒杀商品ID
            if(!StringUtils.isEmpty(seckillOrder.getSeckillId())){
                    criteria.andEqualTo("seckillId",seckillOrder.getSeckillId());
            }
            // 支付金额
            if(!StringUtils.isEmpty(seckillOrder.getMoney())){
                    criteria.andEqualTo("money",seckillOrder.getMoney());
            }
            // 用户
            if(!StringUtils.isEmpty(seckillOrder.getUserId())){
                    criteria.andEqualTo("userId",seckillOrder.getUserId());
            }
            // 创建时间
            if(!StringUtils.isEmpty(seckillOrder.getCreateTime())){
                    criteria.andEqualTo("createTime",seckillOrder.getCreateTime());
            }
            // 支付时间
            if(!StringUtils.isEmpty(seckillOrder.getPayTime())){
                    criteria.andEqualTo("payTime",seckillOrder.getPayTime());
            }
            // 状态，0未支付，1已支付
            if(!StringUtils.isEmpty(seckillOrder.getStatus())){
                    criteria.andEqualTo("status",seckillOrder.getStatus());
            }
            // 收货人地址
            if(!StringUtils.isEmpty(seckillOrder.getReceiverAddress())){
                    criteria.andEqualTo("receiverAddress",seckillOrder.getReceiverAddress());
            }
            // 收货人电话
            if(!StringUtils.isEmpty(seckillOrder.getReceiverMobile())){
                    criteria.andEqualTo("receiverMobile",seckillOrder.getReceiverMobile());
            }
            // 收货人
            if(!StringUtils.isEmpty(seckillOrder.getReceiver())){
                    criteria.andEqualTo("receiver",seckillOrder.getReceiver());
            }
            // 交易流水
            if(!StringUtils.isEmpty(seckillOrder.getTransactionId())){
                    criteria.andEqualTo("transactionId",seckillOrder.getTransactionId());
            }
        }
        return example;
    }

    /**
     * 删除
     * @param id
     */
    @Override
    public void delete(Long id){
        seckillOrderMapper.deleteByPrimaryKey(id);
    }

    /**
     * 修改SeckillOrder
     * @param seckillOrder
     */
    @Override
    public void update(SeckillOrder seckillOrder){
        seckillOrderMapper.updateByPrimaryKey(seckillOrder);
    }

    /**
     * 增加SeckillOrder
     * @param seckillOrder
     */
    @Override
    public void add(SeckillOrder seckillOrder){
        seckillOrderMapper.insert(seckillOrder);
    }

    /**
     * 根据ID查询SeckillOrder
     * @param id
     * @return
     */
    @Override
    public SeckillOrder findById(Long id){
        return  seckillOrderMapper.selectByPrimaryKey(id);
    }

    /**
     * 查询SeckillOrder全部数据
     * @return
     */
    @Override
    public List<SeckillOrder> findAll() {
        return seckillOrderMapper.selectAll();
    }
}
