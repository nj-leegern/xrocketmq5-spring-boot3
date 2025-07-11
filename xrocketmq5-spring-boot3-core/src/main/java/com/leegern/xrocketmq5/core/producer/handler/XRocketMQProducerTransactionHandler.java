package com.leegern.xrocketmq5.core.producer.handler;

import com.leegern.xrocketmq5.core.XRocketMQException;
import com.leegern.xrocketmq5.core.producer.XRocketMQProducerResponse;
import com.leegern.xrocketmq5.core.producer.transaction.XRocketMQProducerTransactionAction;
import com.leegern.xrocketmq5.core.producer.params.XRocketMQProducerParam;
import com.leegern.xrocketmq5.core.producer.params.XRocketMQProducerTransactionParam;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.rocketmq.client.apis.ClientException;
import org.apache.rocketmq.client.apis.message.Message;
import org.apache.rocketmq.client.apis.producer.SendReceipt;
import org.apache.rocketmq.client.apis.producer.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * 生产者发送事务消息执行器
 */
public class XRocketMQProducerTransactionHandler extends XRocketMQProducerBaseHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(XRocketMQProducerTransactionHandler.class);

    /* 本地事务执行器 */
    private XRocketMQProducerTransactionAction transactionAction;


    @Override
    public <T> void sendMessage(String topic, String tag, Boolean enableAsync, XRocketMQProducerParam<T> requestParam) {
        // 获取本地事务执行器
        this.transactionAction = this.getTransactionAction(requestParam);
        // 调用父类发送消息
        super.sendMessage(topic, tag, enableAsync, requestParam);
    }


    /**
     * 覆写执行发送事务消息方法(事务消息只支持同步发送)
     * @param enableAsync 异步发送标识
     * @param topic       话题
     * @param dwMessage   平台内部消息实体
     */
    @Override
    protected void doSend(Boolean enableAsync, String topic, Message dwMessage)  {
        SendReceipt sendReceipt  = null;
        Transaction transaction  = null;
        Exception   txException  = null;
        boolean     actionResult = false;

        try {
            // 转换成rocketmq标准消息
            Message message = super.convertMessage(dwMessage);

            // 1.开启事务消息
            try {
                transaction = super.getProducer().beginTransaction();
            } catch (ClientException e) {
                throw new XRocketMQException("Begin transaction message error", e);
            }

            // 2.发送半事务消息
            try {
                sendReceipt = super.getProducer().send(message, transaction);
            } catch (ClientException e) {
                throw new XRocketMQException("Send transaction message error", e);
            }
        } catch (Exception e) {
            txException = e;
        } finally {
            try {
                // 发送消息之后拦截器
                super.invokeAfter(topic, dwMessage, txException);
            } catch (Exception ex) {
                LOGGER.error("Execute producer 'invokeAfter' failed after sync sending:{}", ExceptionUtils.getStackTrace(ex));
            }
        }

        // 半事务消息发送失败直接处理回调方法
        if (Objects.isNull(sendReceipt) || Objects.isNull(sendReceipt.getMessageId())) {
            // 组装响应结果
            XRocketMQProducerResponse response = super.populateProducerResponse(sendReceipt, dwMessage);
            // 执行返回结果回调方法
            super.doResponseCallback(response, txException);
            // 直接退出
            return;
        }

        /*
         * 执行本地事务, 并确定本地事务结果.
         *    1. 如果本地事务提交成功, 则提交消息事务.
         *    2. 如果本地事务提交失败, 则回滚消息事务.
         *    3. 如果本地事务未知异常, 则不处理, 等待事务消息回查.
         *    4. 提交或回滚事务消息异常, 则不处理, 等待事务消息回查.
         */
        // 3.执行本地事务
        try {
            actionResult = transactionAction.doTransactionAction();
        } catch (Exception e) {
            throw new XRocketMQException("Execute local transaction action error：" + ExceptionUtils.getStackTrace(e));
        }

        if (actionResult) {
            // 4.成功 -> 提交半事务消息
            try {
                transaction.commit();
            } catch (ClientException e) {
                throw new XRocketMQException("Commit transaction message error.", e);
            }
        } else {
            // 5.失败 -> 回滚半事务消息
            try {
                transaction.rollback();
            } catch (ClientException e) {
                throw new XRocketMQException("Rollback transaction message error", e);
            }
            txException = new XRocketMQException("Execute local transaction action failed and rollback transaction message.");
        }
        // 组装响应结果
        XRocketMQProducerResponse response = super.populateProducerResponse(sendReceipt, dwMessage);
        // 本地事务返回失败
        if (Objects.nonNull(txException)) response.setSuccess(false);
        // 执行返回结果回调方法
        super.doResponseCallback(response, txException);
    }

    /**
     * 获取本地事务执行器
     * @param requestParam
     * @return
     * @param <T>
     */
    private <T> XRocketMQProducerTransactionAction getTransactionAction(XRocketMQProducerParam<T> requestParam) {
        if (! (XRocketMQProducerTransactionParam.class.isAssignableFrom(requestParam.getClass()))) {
            throw new XRocketMQException("'requestParam' must be instance of XRocketMQProducerTransactionParam.");
        }
        XRocketMQProducerTransactionParam<T> transactionParam = (XRocketMQProducerTransactionParam<T>) requestParam;
        if (Objects.isNull(transactionParam.getTransactionAction())) {
            throw new XRocketMQException("'transactionAction' must be not empty.");
        }
        return transactionParam.getTransactionAction();
    }
}
