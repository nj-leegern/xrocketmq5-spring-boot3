package com.leegern.xrocketmq5.core.producer.params;


/**
 * 生产者延时消息参数
 * @param <T>
 */
public class XRocketMQProducerDelayParam<T> extends XRocketMQProducerParam<T> {

    /* 延时时长(单位:秒) */
    private Integer delaySecTime;

    /**
     * 私有化构造器
     * @param builder
     */
    private XRocketMQProducerDelayParam(Builder<T> builder) {
        super.businessId     =   builder.businessId;
        super.msgBody        =   builder.msgBody;
        this.delaySecTime    =   builder.delaySecTime;
    }


    /**
     * builder实例
     * @return
     * @param <T>
     */
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }


    /**
     * 'delaySecTime' of getter
     * @return
     */
    public Integer getDelaySecTime() {
        return delaySecTime;
    }

    /**
     * 建造器
     * @param <T>
     */
    public static final class Builder<T> {
        private String businessId;
        private T msgBody;
        private Integer delaySecTime;

        private Builder() {
        }

        public Builder<T> businessId(String businessId) {
            this.businessId = businessId;
            return this;
        }

        public Builder<T> msgBody(T msgBody) {
            this.msgBody = msgBody;
            return this;
        }

        public Builder<T> delaySecTime(Integer delaySecTime) {
            this.delaySecTime = delaySecTime;
            return this;
        }

        public XRocketMQProducerDelayParam<T> build() {
            return new XRocketMQProducerDelayParam<>(this);
        }
    }
}
