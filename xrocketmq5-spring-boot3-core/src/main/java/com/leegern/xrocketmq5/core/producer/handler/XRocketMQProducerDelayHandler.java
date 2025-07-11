package com.leegern.xrocketmq5.core.producer.handler;

import com.leegern.xrocketmq5.core.XRocketMQException;
import com.leegern.xrocketmq5.core.producer.XRocketMQProducerMessage;
import com.leegern.xrocketmq5.core.producer.params.XRocketMQProducerDelayParam;
import com.leegern.xrocketmq5.core.producer.params.XRocketMQProducerParam;
import org.apache.rocketmq.client.apis.message.Message;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;

/**
 * 生产者发送延时消息执行器
 */
public class XRocketMQProducerDelayHandler extends XRocketMQProducerBaseHandler {


    @Override
    public <T> void sendMessage(String topic, String tag, Boolean enableAsync, XRocketMQProducerParam<T> requestParam) {
        super.sendMessage(topic, tag, enableAsync, requestParam);
    }


    /**
     * 覆写构建平台消息体方法
     * @param topic          话题
     * @param tag            标签
     * @param requestParam   请求参数
     * @return
     * @param <T>
     */
    @Override
    protected <T> Message buildRocketMessage(String topic, String tag, XRocketMQProducerParam<T> requestParam) {
        if (! (XRocketMQProducerDelayParam.class.isAssignableFrom(requestParam.getClass()))) {
            throw new XRocketMQException("'requestParam' must be instance of XRocketMQProducerDelayParam.");
        }
        XRocketMQProducerDelayParam<T> delayParam = (XRocketMQProducerDelayParam<T>) requestParam;
        if (delayParam.getDelaySecTime() <= 0 ) {
            throw new XRocketMQException("'delaySecTime' must be greater than zero.");
        }
        return new XRocketMQProducerMessage(topic, parseJson(requestParam.getMsgBody()), tag, List.of(requestParam.getBusinessId()), null,
                (System.currentTimeMillis() + Duration.ofSeconds(delayParam.getDelaySecTime()).toMillis()), new HashMap<>());
    }


//    /**
//     * 覆写构建消息体方法
//     * @param topic          话题
//     * @param tag            标签
//     * @param requestParam   请求参数
//     * @return
//     * @param <T>
//     */
//    @Override
//    protected <T> Message buildMessage(String topic, String tag, DWRocketMQProducerParam<T> requestParam) {
//        if (! (DWRocketMQProducerDelayParam.class.isAssignableFrom(requestParam.getClass()))) {
//            throw new DWRocketMQException("'requestParam' must be instance of DWRocketMQProducerDelayParam.");
//        }
//        DWRocketMQProducerDelayParam<T> delayParam = (DWRocketMQProducerDelayParam<T>) requestParam;
//        if (delayParam.getDelaySecTime() <= 0 ) {
//            throw new DWRocketMQException("'delaySecTime' must be greater than zero.");
//        }
//        MessageBuilder builder = ClientServiceProvider.loadService().newMessageBuilder()
//                .setTopic(topic)
//                .setBody(parseJson(requestParam.getMsgBody()))
//                .setDeliveryTimestamp(System.currentTimeMillis() + Duration.ofSeconds(delayParam.getDelaySecTime()).toMillis());
//
//        if (StringUtils.hasText(tag))
//            builder.setTag(tag);
//        if (StringUtils.hasText(requestParam.getBusinessId()))
//            builder.setKeys(requestParam.getBusinessId());
//
//        return builder.build();
//    }
}
