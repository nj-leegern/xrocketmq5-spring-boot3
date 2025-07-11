package com.leegern.xrocketmq5.core.consumer.listener;

import org.apache.rocketmq.client.apis.consumer.MessageListener;
import org.apache.rocketmq.client.apis.message.MessageView;

/**
 * 消费者消费消息监听器
 */
public interface XRocketMQConsumerMessageListener extends MessageListener {

    /**
     * 转换消息为指定的目标类型实例
     * @param messageView  消息试图
     * @param targetClazz  消息目标类型
     * @return
     */
    Object convertMessage(MessageView messageView, Class<?> targetClazz);
}
