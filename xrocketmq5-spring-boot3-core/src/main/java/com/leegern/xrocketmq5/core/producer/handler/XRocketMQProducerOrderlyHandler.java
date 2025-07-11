package com.leegern.xrocketmq5.core.producer.handler;

import com.leegern.xrocketmq5.core.XRocketMQException;
import com.leegern.xrocketmq5.core.producer.XRocketMQProducerMessage;
import com.leegern.xrocketmq5.core.producer.params.XRocketMQProducerOrderlyParam;
import com.leegern.xrocketmq5.core.producer.params.XRocketMQProducerParam;
import org.apache.rocketmq.client.apis.message.Message;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;

/**
 * 生产者发送顺序消息执行器
 */
public class XRocketMQProducerOrderlyHandler extends XRocketMQProducerBaseHandler {


    @Override
    public <T> void sendMessage(String topic, String tag, Boolean enableAsync, XRocketMQProducerParam<T> requestParam) {
        super.sendMessage(topic, tag, enableAsync, requestParam);
    }


    /**
     * 覆写构平台建消息方法
     * @param topic          话题
     * @param tag            标签
     * @param requestParam   请求参数
     * @return
     * @param <T>
     */
    @Override
    protected <T> Message buildRocketMessage(String topic, String tag, XRocketMQProducerParam<T> requestParam) {
        if (! (XRocketMQProducerOrderlyParam.class.isAssignableFrom(requestParam.getClass()))) {
            throw new XRocketMQException("'requestParam' must be instance of XRocketMQProducerOrderlyParam.");
        }
        XRocketMQProducerOrderlyParam<T> orderlyParam = (XRocketMQProducerOrderlyParam<T>) requestParam;
        if (! StringUtils.hasText(orderlyParam.getOrderGroup())) {
            throw new XRocketMQException("'orderGroup' must not be empty.");
        }
        return new XRocketMQProducerMessage(topic, parseJson(requestParam.getMsgBody()), tag, List.of(requestParam.getBusinessId()), orderlyParam.getOrderGroup(), null, new HashMap<>());
    }


//    /**
//     * 覆写构建消息方法
//     * @param topic          话题
//     * @param tag            标签
//     * @param requestParam   请求参数
//     * @return
//     * @param <T>
//     */
//    @Override
//    protected <T> Message buildMessage(String topic, String tag, DWRocketMQProducerParam<T> requestParam) {
//        if (! (DWRocketMQProducerOrderlyParam.class.isAssignableFrom(requestParam.getClass()))) {
//            throw new DWRocketMQException("'requestParam' must be instance of DWRocketMQProducerOrderlyParam.");
//        }
//        DWRocketMQProducerOrderlyParam<T> orderlyParam = (DWRocketMQProducerOrderlyParam<T>) requestParam;
//        if (! StringUtils.hasText(orderlyParam.getOrderGroup())) {
//            throw new DWRocketMQException("'orderGroup' must not be empty.");
//        }
//        MessageBuilder builder = ClientServiceProvider.loadService().newMessageBuilder()
//                .setTopic(topic)
//                .setBody(parseJson(requestParam.getMsgBody()))
//                .setMessageGroup(orderlyParam.getOrderGroup());
//
//        if (StringUtils.hasText(tag))
//            builder.setTag(tag);
//        if (StringUtils.hasText(requestParam.getBusinessId()))
//            builder.setKeys(requestParam.getBusinessId());
//
//        return builder.build();
//    }
}
