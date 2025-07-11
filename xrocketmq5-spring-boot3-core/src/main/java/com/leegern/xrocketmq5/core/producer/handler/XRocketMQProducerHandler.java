package com.leegern.xrocketmq5.core.producer.handler;

import com.leegern.xrocketmq5.core.producer.XRocketMQProducerCallback;
import com.leegern.xrocketmq5.core.producer.XRocketMQProducerInterceptor;
import com.leegern.xrocketmq5.core.producer.params.XRocketMQProducerParam;
import org.apache.rocketmq.client.apis.producer.Producer;

import java.util.List;

/**
 * 生产者发送消息的执行器接口
 */
public interface XRocketMQProducerHandler {

    /**
     * 发送消息
     * @param topic         消息主题
     * @param tag           消息标签
     * @param enableAsync   异步发送标识
     * @param requestParam  消息参数
     */
    <T> void sendMessage(String topic, String tag, Boolean enableAsync, XRocketMQProducerParam<T> requestParam);

    /**
     * 设置生产者对象
     * @param producer 生产者实例
     */
    void setProducer(Producer producer);

    /**
     * 设置发送消息前后的拦截器
     * @param interceptors
     */
    void setInterceptors(List<XRocketMQProducerInterceptor> interceptors);

    /**
     * 设置相应结果回调
     * @param responseCallback
     */
    void setResponseCallback(XRocketMQProducerCallback responseCallback);
}
