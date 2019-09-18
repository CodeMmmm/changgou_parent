package com.changgou.order.mq.queue;


import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QueueConfig {
    // 延时队列，过期将数据发送给另外一个队列
    @Bean
    public Queue orderDelayQueue(){
        return QueueBuilder.durable("orderDelayQueue").withArgument("x-dead-letter-exchange","orderListenerExchange").withArgument("x-dead-letter-routing-key","orderListenerQueue").build();
    }

    // 接收延时队列的队列,队列2
    @Bean
    public Queue orderListenerQueue(){
        return new Queue("orderListenerQueue",true);
    }

    /***
     * 创建交换机
     */
    @Bean
    public Exchange orderListenerExchange(){
        return new DirectExchange("orderListenerExchange");
    }

    /***
     * 队列Queue2绑定Exchange
     */
    @Bean
    public Binding orderListenerBinding(Queue orderListenerQueue, Exchange orderListenerExchange){
        return BindingBuilder.bind(orderListenerQueue).to(orderListenerExchange).with("orderListenerQueue").noargs();
    }

}
