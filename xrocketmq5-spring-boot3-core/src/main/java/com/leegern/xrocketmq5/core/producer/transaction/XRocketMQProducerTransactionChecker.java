package com.leegern.xrocketmq5.core.producer.transaction;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leegern.xrocketmq5.core.XRocketMQException;
import com.leegern.xrocketmq5.core.json.JacksonProvider;
import com.leegern.xrocketmq5.core.producer.XRocketMQProducerCallback;
import com.leegern.xrocketmq5.core.producer.XRocketMQProducerResponse;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.rocketmq.client.apis.message.MessageView;
import org.apache.rocketmq.client.apis.producer.TransactionChecker;
import org.apache.rocketmq.client.apis.producer.TransactionResolution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * 事务回查器抽象类
 */
public abstract class XRocketMQProducerTransactionChecker<T> implements TransactionChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger(XRocketMQProducerTransactionChecker.class);

    /* 消息目标类clazz */
    private Class<T> messageClazz;

    /* json序列化 */
    private ObjectMapper json;

    /* 返回结果回调 */
    private XRocketMQProducerCallback responseCallback;


    /**
     * 自定义构造器
     */
    public XRocketMQProducerTransactionChecker() {
        // JSON实例化
        this.json = JacksonProvider.getInstance().getJson();
    }

    /**
     * 'messageClazz' of setter
     * @param messageClazz
     */
    public void setMessageClazz(Class<T> messageClazz) {
        this.messageClazz = messageClazz;
    }

    /**
     * 'responseCallback' of setter
     * @param responseCallback
     */
    public void setResponseCallback(XRocketMQProducerCallback responseCallback) {
        this.responseCallback = responseCallback;
    }


    /**
     * 覆写事务核查方法
     * @param messageView 消息视图
     * @return
     */
    @Override
    public TransactionResolution check(MessageView messageView) {
        TransactionResolution resolution = TransactionResolution.UNKNOWN;
        Exception exception = null;

        if (ObjectUtils.allNotNull(messageView, messageView.getBody())) {
            try {
                // 转换消息
                T message = this.parseMessage(messageView);
                // 回查事务状态
                Boolean result = this.doCheckTransaction(message);
                // 状态结果 true->已经提, false->未提交, null->未知
                if (Objects.nonNull(result)) {
                    resolution = result ? TransactionResolution.COMMIT : TransactionResolution.ROLLBACK;
                }
            } catch (Exception e) {
                exception = e;
                LOGGER.error("Check local transaction status failed:{}", ExceptionUtils.getStackTrace(e));
            } finally {
                // 提交或回滚状态执行结果回调
                if (! Objects.equals(resolution, TransactionResolution.UNKNOWN)) {
                    try {
                        // 组装响应结果
                        XRocketMQProducerResponse response = this.populateProducerResponse(Objects.equals(resolution, TransactionResolution.COMMIT), messageView);
                        // 执行返回结果回调方法
                        this.doResponseCallback(response, exception);
                    } catch (Exception e) {
                        LOGGER.error("Execute 'doResponseCallback' failed:{}", ExceptionUtils.getStackTrace(e));
                    }
                }
            }
        }

        if (Objects.equals(resolution, TransactionResolution.UNKNOWN)) {
            LOGGER.warn("Current transaction resolution is unknown, msgView:{}", messageView);
        }

        return resolution;
    }


    /**
     * 回查事务是否已经提交
     * @param msgBody 消息体
     * @return true:已经提交, false:未提交, null:未知
     */
    protected abstract Boolean doCheckTransaction(T msgBody);



    /**
     * 获取消息参数clazz
     * @return
     */
    @SuppressWarnings("unchecked")
    protected Class<T> getMessageClazz() {
        if (Objects.isNull(messageClazz)) {
            // 通过反射获取泛型参数clazz
            Type superClass = getClass().getGenericSuperclass();
            ParameterizedType type = (ParameterizedType) superClass;
            messageClazz = (Class<T>) type.getActualTypeArguments()[0];
        }
        return messageClazz;
    }


    /**
     * 组装发送响应结果
     * @param success
     * @param messageView
     * @return
     */
    private XRocketMQProducerResponse populateProducerResponse(boolean success, MessageView messageView) {
        XRocketMQProducerResponse response = new XRocketMQProducerResponse(success);

        if (Objects.nonNull(messageView)) {
            // 消息ID
            if (Objects.nonNull(messageView.getMessageId())) {
                response.setMessageId(messageView.getMessageId().toString());
            }
            // 扩展信息
            response.setExtInfo(this.extractMsgExt(messageView));
        }

        return response;
    }

    /**
     * 执行返回结果回调方法
     * @param response  返回结果
     * @param exception 异常实例
     */
    private void doResponseCallback(XRocketMQProducerResponse response, Exception exception) {
        if (Objects.nonNull(responseCallback)) {
            if (! response.getSuccess()) {
                if (Objects.isNull(exception))
                    exception = new XRocketMQException("The result of the check local transaction is not committed.");
                responseCallback.onFailure(response, exception);
            }
            else {
                responseCallback.onSuccess(response);
            }
        }
    }

    /**
     * 转换消息为泛型类型
     * @param messageView
     * @return
     */
    @SuppressWarnings("unchecked")
    private T parseMessage(MessageView messageView) {
        if (MessageView.class.equals(getMessageClazz())) {
            return (T) messageView; // 直接返回 MessageView
        }
        // 消息体
        String msgBody = StandardCharsets.UTF_8.decode(messageView.getBody()).toString();
        if (String.class.equals(getMessageClazz())) {
            return (T) msgBody;     // 直接返回 String
        }
        // json反序列化
        try {
            return json.readValue(msgBody, getMessageClazz());
        } catch (JsonProcessingException e) {
            throw new XRocketMQException("Json deserialization error", e);
        }
    }

    /**
     * 提取消息扩展信息
     * @param message
     * @return
     */
    private String extractMsgExt(MessageView message) {
        if (Objects.nonNull(message)) {
            StringBuilder sb = new StringBuilder("msgExt{");
            if (StringUtils.hasText(message.getTopic())) {
                sb.append("topic=").append(message.getTopic()).append(",");
            }
            if (message.getTag().isPresent()) {
                sb.append("tag=").append(message.getTag().get()).append(",");
            }
            if (!CollectionUtils.isEmpty(message.getKeys())) {
                sb.append("keys=").append(message.getKeys()).append(",");
            }
            if (message.getMessageGroup().isPresent()) {
                sb.append("messageGroup=").append(message.getMessageGroup().get()).append(",");
            }
            if (message.getDeliveryTimestamp().isPresent()) {
                sb.append("deliveryTimestamp=").append(message.getDeliveryTimestamp().get()).append(",");
            }
            String result = sb.toString();
            if (result.lastIndexOf(",") != -1) {
                result = result.substring(0, result.lastIndexOf(","));
            }
            return result + "}";
        }
        return null;
    }
}
