package com.leegern.xrocketmq5.core.consumer;

import org.apache.rocketmq.client.apis.message.MessageView;
import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;

import java.util.List;

/**
 * 消费者消费消息前后的拦截器接口
 */
public interface XRocketMQConsumerInterceptor extends Ordered {

    /**
     * 消息消费之前执行
     * @param messageView 消息视图
     * @return
     */
    MessageView before(MessageView messageView);

    /**
     * 消息消费之前后执行
     * @param messageView 消息视图
     * @param ex          异常信息
     */
    void after(MessageView messageView, @Nullable Exception ex);

    /**
     * 指定生效的话题列表, 如果为空表示对所有话题生效.
     * @return
     */
    default List<String> matchTopics() {
        return null;
    }


    /**
     * 执行顺序
     * @return
     */
    @Override
    default int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
