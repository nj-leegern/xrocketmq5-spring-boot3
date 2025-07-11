package com.leegern.xrocketmq5.core.producer.params;

import com.leegern.xrocketmq5.core.producer.transaction.XRocketMQProducerTransactionAction;
import com.leegern.xrocketmq5.core.producer.transaction.XRocketMQProducerTransactionChecker;

/**
 * 生产者事务消息参数
 * @param <T>
 */
public class XRocketMQProducerTransactionParam<T> extends XRocketMQProducerParam<T> {

    /* 事务回查器实例 */
    private XRocketMQProducerTransactionChecker transactionChecker;

    /* 本地事务执行器 */
    private XRocketMQProducerTransactionAction transactionAction;


    /**
     * 私有化构造器
     * @param builder
     */
    private XRocketMQProducerTransactionParam(Builder<T> builder) {
        super.businessId         =  builder.businessId;
        super.msgBody            =  builder.msgBody;
        this.transactionChecker  =  builder.transactionChecker;
        this.transactionAction   =  builder.transactionAction;
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
     * 'transactionChecker' of getter
     * @return
     */
    public XRocketMQProducerTransactionChecker getTransactionChecker() {
        return transactionChecker;
    }

    /**
     * 'transactionAction' of getter
     * @return
     */
    public XRocketMQProducerTransactionAction getTransactionAction() {
        return transactionAction;
    }


    /**
     * 建造器
     * @param <T>
     */
    public static final class Builder<T> {
        private String businessId;
        private T msgBody;
        private XRocketMQProducerTransactionChecker transactionChecker;
        private XRocketMQProducerTransactionAction  transactionAction;

        private Builder() {}

        public Builder<T> businessId(String businessId) {
            this.businessId = businessId;
            return this;
        }

        public Builder<T> msgBody(T msgBody) {
            this.msgBody = msgBody;
            return this;
        }

        public Builder<T> transactionChecker(XRocketMQProducerTransactionChecker transactionChecker) {
            this.transactionChecker = transactionChecker;
            return this;
        }

        public Builder<T> transactionAction(XRocketMQProducerTransactionAction transactionAction) {
            this.transactionAction = transactionAction;
            return this;
        }

        public XRocketMQProducerTransactionParam<T> build() {
            return new XRocketMQProducerTransactionParam<>(this);
        }
    }
}
