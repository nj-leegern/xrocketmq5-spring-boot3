package com.leegern.xrocketmq5.core.consumer.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leegern.xrocketmq5.core.XRocketMQException;
import com.leegern.xrocketmq5.core.consumer.XRocketMQConsumerInterceptor;
import com.leegern.xrocketmq5.core.json.JacksonProvider;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.rocketmq.client.apis.consumer.ConsumeResult;
import org.apache.rocketmq.client.apis.message.MessageView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 *  消费者消费消息监听器默认实现
 */
public class XRocketMQConsumerDefaultMessageListener implements XRocketMQConsumerMessageListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(XRocketMQConsumerDefaultMessageListener.class);

    /* 实例对象 */
    private Object beanObj;

    /* 消息目标类clazz */
    private Class<?> messageClazz;

    /* 消费消息的方法 */
    private Method invocableMethod;

    /* 消费消息前后的拦截器 */
    private List<XRocketMQConsumerInterceptor> interceptors;

    /* 订阅的主题 */
    private String topicName;

    /* json序列化 */
    private ObjectMapper json;

    /**
     * 自定义构造器
     * @param messageClazz
     * @param invocableMethod
     * @param interceptors
     * @param topicName
     */
    public XRocketMQConsumerDefaultMessageListener(Object bean, Class<?> messageClazz, Method invocableMethod,
                                                   List<XRocketMQConsumerInterceptor> interceptors, String topicName) {
        this.beanObj           =   bean;
        this.messageClazz      =   messageClazz;
        this.invocableMethod   =   invocableMethod;
        this.interceptors      =   interceptors;
        this.topicName         =   topicName;

        // JSON实例化
        this.json = JacksonProvider.getInstance().getJson();

        // 拦截器排序
        this.interceptors.sort(Comparator.comparingInt(XRocketMQConsumerInterceptor::getOrder));
    }


    @Override
    public ConsumeResult consume(MessageView messageView) {
        if (ObjectUtils.allNotNull(messageView, messageView.getBody())) {
            Exception ex = null;
            try {
                // 消费消息之前拦截器
                messageView = this.invokeBefore(messageView);

                // 转换消息
                Object msgObj = this.convertMessage(messageView, messageClazz);

                StopWatch stopWatch = StopWatch.createStarted();
                // 消费消息
                Object returnVal = this.doInvoke(invocableMethod, beanObj, msgObj);
                if(! stopWatch.isStopped())
                    stopWatch.stop();

                if (LOGGER.isDebugEnabled())
                    LOGGER.debug("Consume message[msgId={}] cost: {}ms", messageView.getMessageId(), stopWatch.getTime());

                // 处理方法执行结果
                if (! Objects.isNull(returnVal)) {
                    if (returnVal instanceof ConsumeResult) {
                        return (ConsumeResult) returnVal;
                    }
                    else if (returnVal instanceof Boolean) {
                        return ((boolean) returnVal) ? ConsumeResult.SUCCESS : ConsumeResult.FAILURE;
                    }
                }
            } catch (Exception e) {
                ex = e;
                LOGGER.error("Consume message of method[{}] failed, msg:{}, err:{}", invocableMethod.getName(), messageView, ExceptionUtils.getStackTrace(e));
                return ConsumeResult.FAILURE;
            } finally {
                try {
                    // 消费消息之后拦截器
                    this.invokeAfter(messageView, ex);
                } catch (Exception e) {
                    LOGGER.error("Execute consumer 'invokeAfter' err:{}", ExceptionUtils.getStackTrace(e));
                }
            }
        } else {
            LOGGER.warn("'messageView' does not contain any body when consuming msg : {}", messageView);
        }

        return ConsumeResult.SUCCESS;
    }


    @Override
    public Object convertMessage(MessageView messageView, Class<?> targetClazz) {
        if (Objects.equals(targetClazz, MessageView.class)) {
            return messageView;
        }
        // 消息体
        String msgBody = StandardCharsets.UTF_8.decode(messageView.getBody()).toString();
        if (Objects.equals(targetClazz, String.class)) {
            return msgBody;
        }
        // json反序列化
        try {
            return this.json.readValue(msgBody, targetClazz);
        } catch (JsonProcessingException e) {
            throw new XRocketMQException("Json deserialization error", e);
        }
    }


    /**
     * 执行消费消息方法
     * @param invocableMethod 消费消息的方法
     * @param beanObj         bean实例
     * @param paramObj        消息参数
     * @return
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    private Object doInvoke(Method invocableMethod, Object beanObj, Object paramObj)
            throws InvocationTargetException, IllegalAccessException {
        // 解析桥接方法对应的实际目标方法
        Method invokeMethod = BridgeMethodResolver.findBridgedMethod(invocableMethod);
        // 执行消费消息方法
        return invokeMethod.invoke(beanObj, paramObj);
    }


    /**
     * 消费消息之前拦截器
     * @param messageView  消息视图
     * @return
     */
    private MessageView invokeBefore(MessageView messageView) {
        if (! CollectionUtils.isEmpty(interceptors)) {
            for (XRocketMQConsumerInterceptor interceptor : interceptors) {
                if (CollectionUtils.isEmpty(interceptor.matchTopics()) || interceptor.matchTopics().contains(topicName)) {
                    messageView = interceptor.before(messageView);
                }
            }
        }
        return messageView;
    }

    /**
     * 消费消息之后拦截器
     * @param messageView  消息视图
     * @param ex
     */
    private void invokeAfter(MessageView messageView, Exception ex) {
        if (! CollectionUtils.isEmpty(interceptors)) {
            for (XRocketMQConsumerInterceptor interceptor : interceptors) {
                if (CollectionUtils.isEmpty(interceptor.matchTopics()) || interceptor.matchTopics().contains(topicName)) {
                    interceptor.after(messageView, ex);
                }
            }
        }
    }

}
