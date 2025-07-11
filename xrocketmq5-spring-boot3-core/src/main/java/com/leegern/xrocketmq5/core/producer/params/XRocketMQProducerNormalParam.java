package com.leegern.xrocketmq5.core.producer.params;


/**
 * 生产者普通消息参数
 *      exam: XRocketMQProducerNormalParam<String> param =
 *                                                  XRocketMQProducerNormalParam.<String>builder().businessId("").msgBody("").build();
 * @param <T>
 */
public class XRocketMQProducerNormalParam<T> extends XRocketMQProducerParam<T> {


    /**
     * 私有化构造器
     * @param builder
     */
    private XRocketMQProducerNormalParam(Builder<T> builder) {
        super.businessId  =  builder.businessId;
        super.msgBody     =  builder.msgBody;
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
     * 建造器
     * @param <T>
     */
    public static final class Builder<T> {
        private String businessId;
        private T msgBody;

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

        public XRocketMQProducerNormalParam<T> build() {
            return new XRocketMQProducerNormalParam<>(this);
        }
    }
}
