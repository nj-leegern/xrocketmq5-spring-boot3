package com.leegern.xrocketmq5.core.producer;

import org.apache.rocketmq.client.apis.message.Message;
import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;

import java.util.List;

/**
 * 生产者发送消息前后的拦截器接口
 */
public interface XRocketMQProducerInterceptor extends Ordered {

    /**
     * 消息发送之前执行
     * @param message 消息
     * @return
     */
    Message before(Message message);

    /**
     * 消息发送之后执行
     * @param message 消息
     * @param ex      异常
     */
    void after(Message message, @Nullable Throwable ex);

    /**
     * 指定生效的话题列表, 如果为空表示对所有话题生效.
     * @return
     */
    default List<String> matchTopics() {
        return null;
    }

    /**
     * 在容器中执行的顺序
     * @return
     */
    @Override
    default int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
