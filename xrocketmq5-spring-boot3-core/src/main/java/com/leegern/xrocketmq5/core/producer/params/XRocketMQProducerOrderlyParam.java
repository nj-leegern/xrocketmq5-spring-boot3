package com.leegern.xrocketmq5.core.producer.params;


/**
 * 生产者有序消息参数
 * @param <T>
 */
public class XRocketMQProducerOrderlyParam<T> extends XRocketMQProducerParam<T> {

    /* 消息排序分组名 */
    private String orderGroup;

    /**
     * 私有化构造器
     * @param builder
     */
    private XRocketMQProducerOrderlyParam(Builder<T> builder) {
        super.businessId = builder.businessId;
        super.msgBody    = builder.msgBody;
        this.orderGroup  = builder.orderGroup;
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
     * 'orderGroup' of getter
     * @return
     */
    public String getOrderGroup() {
        return orderGroup;
    }


    /**
     * 建造器
     * @param <T>
     */
    public static final class Builder<T> {
        private String businessId;
        private T msgBody;
        private String orderGroup;

        private Builder() {}

        public Builder<T> businessId(String businessId) {
            this.businessId = businessId;
            return this;
        }

        public Builder<T> msgBody(T msgBody) {
            this.msgBody = msgBody;
            return this;
        }

        public Builder<T> orderGroup(String orderGroup) {
            this.orderGroup = orderGroup;
            return this;
        }

        public XRocketMQProducerOrderlyParam<T> build() {
            return new XRocketMQProducerOrderlyParam<>(this);
        }
    }
}
