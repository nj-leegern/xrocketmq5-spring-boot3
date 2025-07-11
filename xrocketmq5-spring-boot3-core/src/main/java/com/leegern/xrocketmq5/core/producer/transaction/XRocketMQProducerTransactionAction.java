package com.leegern.xrocketmq5.core.producer.transaction;

/**
 * 事务消息本地事务执行器接口
 */
public interface XRocketMQProducerTransactionAction {

    /**
     * 执行本地事务
     * @return true成功 false失败
     */
    boolean doTransactionAction();

}
