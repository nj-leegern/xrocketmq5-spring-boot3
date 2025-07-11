package com.leegern.xrocketmq5.core.producer.params;

/**
 * 生产者发送消息参数
 * @param <T>
 */
public abstract class XRocketMQProducerParam<T> {

    /* 业务消息ID */
    protected String businessId;

    /* 消息实体 */
    protected T msgBody;



    /**
     * 'msgBody' of getter
     * @return
     */
    public T getMsgBody() {
        return msgBody;
    }

    /**
     * 'businessId' of getter
     * @return
     */
    public String getBusinessId() {
        return businessId;
    }


}
