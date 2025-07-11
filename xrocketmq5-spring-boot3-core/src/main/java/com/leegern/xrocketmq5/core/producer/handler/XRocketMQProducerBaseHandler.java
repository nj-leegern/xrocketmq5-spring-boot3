package com.leegern.xrocketmq5.core.producer.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leegern.xrocketmq5.core.XRocketMQException;
import com.leegern.xrocketmq5.core.json.JacksonProvider;
import com.leegern.xrocketmq5.core.producer.XRocketMQProducerCallback;
import com.leegern.xrocketmq5.core.producer.XRocketMQProducerInterceptor;
import com.leegern.xrocketmq5.core.producer.XRocketMQProducerMessage;
import com.leegern.xrocketmq5.core.producer.XRocketMQProducerResponse;
import com.leegern.xrocketmq5.core.producer.params.XRocketMQProducerParam;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.message.Message;
import org.apache.rocketmq.client.apis.message.MessageBuilder;
import org.apache.rocketmq.client.apis.producer.Producer;
import org.apache.rocketmq.client.apis.producer.SendReceipt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * 生产者发送消息的执行器抽象类
 *          具体包括：普通消息、顺序消息、事务消息、延迟消息等子类
 */
public abstract class XRocketMQProducerBaseHandler implements XRocketMQProducerHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(XRocketMQProducerBaseHandler.class);

    /* 生产者实例 */
    private Producer producer;

    /* 发送消息前后的拦截器 */
    private List<XRocketMQProducerInterceptor> interceptors;

    /* 返回结果回调 */
    private XRocketMQProducerCallback responseCallback;

    /* json序列化 */
    private ObjectMapper json;


    /**
     * 自定义构造器
     */
    public XRocketMQProducerBaseHandler() {
        // JSON实例化
        this.json = JacksonProvider.getInstance().getJson();
    }


    @Override
    public <T> void sendMessage(String topic, String tag, Boolean enableAsync, XRocketMQProducerParam<T> requestParam) {

        // 构建平台内部消息实体
        Message dwMessage = this.buildRocketMessage(topic, tag, requestParam);

        // 执行消息发送前的拦截器
        dwMessage = this.invokeBefore(topic, dwMessage);

        // 发送消息
        this.doSend(enableAsync, topic, dwMessage);

    }

    /**
     * 执行发送消息
     * @param enableAsync 异步发送标识
     * @param topic       话题
     * @param dwMessage   平台内部消息
     */
    protected void doSend(Boolean enableAsync, String topic, Message dwMessage) {
        if (enableAsync) {
            // 异步发送
            this.sendAsync(topic, dwMessage);
        } else {
            // 同步发送
            this.sendSync(topic, dwMessage);
        }
    }

    /**
     * 同步发送
     * @param topic     话题
     * @param dwMessage 平台内部消息
     */
    protected void sendSync(String topic, Message dwMessage) {
        SendReceipt sendReceipt = null;
        Exception   exception   = null;

        try {
            // 转换成rocketmq标准消息
            Message message = this.convertMessage(dwMessage);
            // 同步发送
            sendReceipt = producer.send(message);
        } catch (Exception e) {
            exception = e;
        } finally {
            try {
                // 发送消息之后拦截器
                this.invokeAfter(topic, dwMessage, exception);
            } catch (Exception e) {
                LOGGER.error("Execute producer 'invokeAfter' failed after sync sending:{}", ExceptionUtils.getStackTrace(e));
            }
        }
        // 组装响应结果
        XRocketMQProducerResponse response = this.populateProducerResponse(sendReceipt, dwMessage);
        // 执行返回结果回调方法
        this.doResponseCallback(response, exception);
    }

    /**
     * 异步发送
     * @param topic     话题
     * @param dwMessage 平台内部消息
     */
    protected void sendAsync(String topic, Message dwMessage) {
        // 转换消息
        Message message = this.convertMessage(dwMessage);
        // 异步发送
        CompletableFuture<SendReceipt> resultFuture = producer.sendAsync(message);
        // 处理结果
        resultFuture.whenComplete(((sendReceipt, throwable) -> {
            try {
                // 发送消息之后拦截器
                this.invokeAfter(topic, dwMessage, throwable);
            } catch (Exception e) {
                LOGGER.error("Execute producer 'invokeAfter' failed after async sending:{}", ExceptionUtils.getStackTrace(e));
            }
            // 组装响应结果
            XRocketMQProducerResponse response = populateProducerResponse(sendReceipt, dwMessage);
            // 执行返回结果回调方法
            doResponseCallback(response, throwable);
        }));
    }

    /**
     * 构建生产者消息
     * @param topic          话题
     * @param tag            标签
     * @param requestParam   请求参数
     * @return
     * @param <T>
     */
    protected <T> Message buildRocketMessage(String topic, String tag, XRocketMQProducerParam<T> requestParam) {
        return new XRocketMQProducerMessage(topic, parseJson(requestParam.getMsgBody()), tag, List.of(requestParam.getBusinessId()), null, null, new HashMap<>());
    }

    /**
     * 转换消息(平台消息转成rocketmq标准消息)
     * @param dwMessage 平台内部消息
     * @return
     */
    protected Message convertMessage(Message dwMessage) {
        MessageBuilder builder = ClientServiceProvider.loadService().newMessageBuilder()
                .setTopic(dwMessage.getTopic())
                .setBody(dwMessage.getBody().array());

        // 消息标签
        if (dwMessage.getTag().isPresent() && StringUtils.hasText(dwMessage.getTag().get()))
            builder.setTag(dwMessage.getTag().get());
        // 消息标识
        if (! CollectionUtils.isEmpty(dwMessage.getKeys()))
            builder.setKeys(dwMessage.getKeys().toArray(String[]::new));
        // 延时时间
        if (dwMessage.getDeliveryTimestamp().isPresent())
            builder.setDeliveryTimestamp(dwMessage.getDeliveryTimestamp().get());
        // 消息分组
        if (dwMessage.getMessageGroup().isPresent() && StringUtils.hasText(dwMessage.getMessageGroup().get()))
            builder.setMessageGroup(dwMessage.getMessageGroup().get());
        // 消息属性组
        if (! CollectionUtils.isEmpty(dwMessage.getProperties()))
            dwMessage.getProperties().forEach(builder::addProperty);

        return builder.build();
    }


    /**
     * 消息对象json序列化
     * @param msgBody
     * @return
     */
    protected byte[] parseJson(Object msgBody) {
        try {
            return this.json.writeValueAsString(msgBody).getBytes(StandardCharsets.UTF_8);
        } catch (JsonProcessingException e) {
            throw new XRocketMQException("Json serialization error", e);
        }
    }

    /**
     * 组装发送响应结果
     * @param sendReceipt
     * @param dwMessage
     * @return
     */
    protected XRocketMQProducerResponse populateProducerResponse(SendReceipt sendReceipt, Message dwMessage) {
        XRocketMQProducerResponse response = new XRocketMQProducerResponse(false);
        // 成功
        if (Objects.nonNull(sendReceipt) && Objects.nonNull(sendReceipt.getMessageId())) {
            response.setSuccess(true);
            response.setMessageId(sendReceipt.getMessageId().toString());
        }
        // 扩展信息
        if (Objects.nonNull(dwMessage)) {
            response.setExtInfo(this.extractMsgExt(dwMessage));
        }
        return response;
    }

    /**
     * 执行返回结果回调方法
     * @param response  返回结果
     * @param exception 异常实体
     */
    protected void doResponseCallback(XRocketMQProducerResponse response, Throwable exception) {
        if (Objects.nonNull(exception)) {
            LOGGER.warn("Send message to rocketmq error: {}", ExceptionUtils.getStackTrace(exception));
        }
        if (Objects.nonNull(responseCallback)) {
            if (! response.getSuccess()) {
                if (Objects.isNull(exception))
                    exception = new XRocketMQException("Response 'messageId' is null when send message to rocketmq.");
                responseCallback.onFailure(response, exception);
            }
            else {
                responseCallback.onSuccess(response);
            }
        }
    }

    /**
     * 发送消息之后拦截器
     * @param topic 话题
     * @param msg   消息
     * @param ex    异常
     */
    protected void invokeAfter(String topic, Message msg, Throwable ex) {
        if (! CollectionUtils.isEmpty(this.interceptors)) {
            for (XRocketMQProducerInterceptor interceptor : this.interceptors) {
                if (CollectionUtils.isEmpty(interceptor.matchTopics()) || interceptor.matchTopics().contains(topic)) {
                    interceptor.after(msg, ex);
                }
            }
        }
    }

    /**
     * 'producer' of getter
     * @return
     */
    public Producer getProducer() {
        return producer;
    }

    /**
     * 'producer' of setter
     * @param producer 生产者实例
     */
    @Override
    public void setProducer(Producer producer) {
        this.producer = producer;
    }

    /**
     * 'interceptors' of setter
     * @param interceptors
     */
    @Override
    public void setInterceptors(List<XRocketMQProducerInterceptor> interceptors) {
        this.interceptors = interceptors;
        // 拦截器排序
        this.interceptors.sort(Comparator.comparingInt(XRocketMQProducerInterceptor::getOrder));
    }

    /**
     * 'responseCallback' of setter
     * @param responseCallback
     */
    @Override
    public void setResponseCallback(XRocketMQProducerCallback responseCallback) {
        this.responseCallback = responseCallback;
    }


    /**
     * 发送消息之前拦截器
     * @param topic 话题
     * @param msg   消息
     * @return
     */
    private Message invokeBefore(String topic, Message msg) {
        if (! CollectionUtils.isEmpty(this.interceptors)) {
            for (XRocketMQProducerInterceptor interceptor : this.interceptors) {
                if (CollectionUtils.isEmpty(interceptor.matchTopics()) || interceptor.matchTopics().contains(topic)) {
                    msg = interceptor.before(msg);
                }
            }
        }
        return msg;
    }

    /**
     * 提取消息扩展信息
     * @param message
     * @return
     */
    private String extractMsgExt(Message message) {
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
