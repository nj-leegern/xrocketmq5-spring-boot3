package com.leegern.xrocketmq5.core.producer.handler;

import com.leegern.xrocketmq5.core.producer.params.XRocketMQProducerParam;

/**
 * 生产者发送普通消息执行器
 */
public class XRocketMQProducerNormalHandler extends XRocketMQProducerBaseHandler {


    @Override
    public <T> void sendMessage(String topic, String tag, Boolean enableAsync, XRocketMQProducerParam<T> requestParam) {
        super.sendMessage(topic, tag, enableAsync, requestParam);
    }
}
