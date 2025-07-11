package com.leegern.xrocketmq5.core.producer;

import com.leegern.xrocketmq5.core.XRocketMQConstants;
import com.leegern.xrocketmq5.core.XRocketMQException;
import com.leegern.xrocketmq5.core.enums.MessageType;
import com.leegern.xrocketmq5.core.producer.annotation.XRocketMQPusher;
import com.leegern.xrocketmq5.core.producer.handler.*;
import com.leegern.xrocketmq5.core.producer.params.XRocketMQProducerParam;
import com.leegern.xrocketmq5.core.producer.params.XRocketMQProducerTransactionParam;
import com.leegern.xrocketmq5.core.producer.transaction.XRocketMQProducerTransactionChecker;
import com.leegern.xrocketmq5.core.properties.XRocketMQProperties;
import org.apache.rocketmq.client.apis.ClientConfiguration;
import org.apache.rocketmq.client.apis.ClientException;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.StaticSessionCredentialsProvider;
import org.apache.rocketmq.client.apis.producer.Producer;
import org.apache.rocketmq.client.apis.producer.ProducerBuilder;
import org.apache.rocketmq.client.apis.producer.TransactionChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 自定义生产者(producer)接口方法代理
 */
@SuppressWarnings("ALL")
public class XRocketMQProducerProxy implements MethodInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(XRocketMQProducerProxy.class);

    /* 应用上下文 */
    private ApplicationContext applicationContext;

    /* 代理目标clazz */
    private Class<?> targetClazz;

    /* 生产者实例持有者 */
    private XRocketMQProducerHolder producerHolder;

    /* 发送消息前后的拦截器 */
    private List<XRocketMQProducerInterceptor> interceptors;

    /* 生产者属性配置 */
    private XRocketMQProperties producerProperties;



    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        XRocketMQProducerProxy that = (XRocketMQProducerProxy) o;
        return Objects.equals(targetClazz, that.targetClazz);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(targetClazz);
    }


    /**
     * 自定义构造器
     * @param applicationContext
     * @param targetClazz
     * @param producerHolder
     */
    public XRocketMQProducerProxy(ApplicationContext applicationContext, Class<?> targetClazz, XRocketMQProducerHolder producerHolder) {
        this.applicationContext   =   applicationContext;
        this.producerHolder       =   producerHolder;
        this.targetClazz          =   targetClazz;
    }


    @Override
    public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
        // 获取producer接口方法上的注解
        XRocketMQPusher pusherAnnotation = AnnotationUtils.getAnnotation(method, XRocketMQPusher.class);
        if (! Objects.isNull(pusherAnnotation)) {
            // 话题
            String topicName = pusherAnnotation.topicName();
            if (! StringUtils.hasText(topicName)) {
                throw new XRocketMQException(String.format(XRocketMQConstants.CHECK_NULL_TIPS, "topicName", obj.getClass().getName(), method.getName()));
            }
            // 消息类型
            MessageType msgType = pusherAnnotation.msgType();
            if (Objects.isNull(msgType)) {
                throw new XRocketMQException(String.format(XRocketMQConstants.CHECK_NULL_TIPS, "msgType", obj.getClass().getName(), method.getName()));
            }
            // 提取原方法参数
            ArgsEntity argsEntity = this.getArgsEntity(args);
            if (Objects.isNull(argsEntity.param) || Objects.isNull(argsEntity.param.getMsgBody())) {
                throw new XRocketMQException(String.format(XRocketMQConstants.CHECK_NULL_TIPS, "producer param or data", obj.getClass().getName(), method.getName()));
            }

            // 生产者名称
            String producerName = this.makeProducerName(msgType, obj.getClass().getName(), method.getName());
            // 核查事务消息且获取事务回查器
            XRocketMQProducerTransactionChecker txCheck = this.checkAndGetTransactionChecker(msgType, argsEntity);

            Producer producer = null;
            try {
                // 获取生产者实例
                producer = this.makeAndGetProducer(producerName, this.getProducerProperties(), topicName, txCheck);
            } catch (Exception e) {
                throw new XRocketMQException("Create producer instance failed", e);
            }
            // 发送消息
            this.sendMessage(producer, argsEntity, topicName, pusherAnnotation.tagExpress(), msgType, pusherAnnotation.enableAsync());

            return null;
        }

        // 如果没有生产者发送方法注解, 执行原始方法
        return proxy.invokeSuper(obj, args);
    }


    /**
     * 发送消息
     * @param producer     生产者实例
     * @param argsParam    请求参数
     * @param topicName    话题名称
     * @param msgTag       消息标签
     * @param msgType      消息类型
     * @param enableAsync  异步发送
     */
    private void sendMessage(Producer producer, ArgsEntity argsParam, String topicName,
                             String msgTag, MessageType msgType, boolean enableAsync) {

        // 获取对应的消息发送器
        XRocketMQProducerHandler producerHandler = this.getProducerHandler(msgType, producer, argsParam.callback);
        // 发送消息
        producerHandler.sendMessage(topicName, msgTag, enableAsync, argsParam.param);

    }



    /**
     * 创建生产者实例
     * @param producerProperties 生产者属性配置
     * @param topic              话题
     * @param txChecker          事务回查
     * @return
     */
    private Producer buildProducer(XRocketMQProperties producerProperties, String topic, TransactionChecker txChecker) throws ClientException {
        final ClientServiceProvider provider = ClientServiceProvider.loadService();
        ClientConfiguration clientConfiguration = ClientConfiguration.newBuilder()
                .setEndpoints(producerProperties.getEndpoints())
                .setCredentialProvider(new StaticSessionCredentialsProvider(producerProperties.getAccessKey(), producerProperties.getSecretKey()))
                .enableSsl(producerProperties.getSslEnabled())
                .setRequestTimeout(Duration.ofMillis(producerProperties.getProducer().getSendMsgTimeout()))
                .build();
        ProducerBuilder builder = provider.newProducerBuilder()
                .setClientConfiguration(clientConfiguration)
                .setTopics(topic)
                .setMaxAttempts(producerProperties.getProducer().getRetrySendTimes());

        if (! Objects.isNull(txChecker)) {
            builder.setTransactionChecker(txChecker);
        }
        return builder.build();
    }


    /**
     * 生成生产者名称
     * @param msgType        消息类型
     * @param producerClazz  生产者类名
     * @param txMethod       事务方法名
     * @return
     */
    private String makeProducerName(MessageType msgType, String producerClazz, String txMethod) {
        if (msgType == MessageType.TRANSACTION) {
            return String.format(XRocketMQConstants.PRODUCER_NAME_TX, producerClazz, txMethod);
        }
        else {
            return XRocketMQConstants.PRODUCER_NAME_PUBLIC;
        }
    }

    /**
     * 获取生产者实例
     * @param producerName       生产者名称
     * @param producerProperties 生产者属性配置
     * @param topic              消息话题
     * @param transactionChecker 事务回查器
     * @return
     */
    private Producer makeAndGetProducer(String producerName, XRocketMQProperties producerProperties,
                                        String topic, TransactionChecker transactionChecker) throws ClientException {
        Producer producer = null;
        if (Objects.isNull(producer = producerHolder.getProducer(producerName))) {
            synchronized (XRocketMQProducerProxy.class) {
                if (Objects.isNull(producer = producerHolder.getProducer(producerName))) {
                    // 创建生产者实例
                    producer = this.buildProducer(producerProperties, topic, transactionChecker);
                    // 存入缓存
                    producerHolder.putProducer(producerName, producer);
                }
            }
        }
        return producer;
    }

    /**
     * 核查事务消息且获取事务回查器
     * @param messageType   消息类型
     * @param argsParam     参数实体
     * @return
     */
    private XRocketMQProducerTransactionChecker checkAndGetTransactionChecker(MessageType messageType, ArgsEntity argsParam) {
        XRocketMQProducerTransactionChecker transactionChecker = null;
        // 是否是事务消息
        if (messageType == MessageType.TRANSACTION) {
            if (! (XRocketMQProducerTransactionParam.class.isAssignableFrom(argsParam.param.getClass()))) {
                throw new XRocketMQException("'requestParam' must be instance of XRocketMQProducerTransactionParam when 'messageType' is Transaction");
            }
            XRocketMQProducerTransactionParam transactionParam = (XRocketMQProducerTransactionParam) argsParam.param;
            if (Objects.isNull(transactionParam.getTransactionChecker())) {
                throw new XRocketMQException("'transactionChecker' must not be empty");
            }
            transactionChecker = transactionParam.getTransactionChecker();
//            // 设置消息体clazz
//            transactionChecker.setMessageClazz(transactionParam.getMsgBody().getClass());
            // 设置相应结果回调
            transactionChecker.setResponseCallback(argsParam.callback);
        }
        return transactionChecker;
    }

    /**
     * 获取容器中所有生产者拦截器集合
     * @return
     */
    private List<XRocketMQProducerInterceptor> getProducerInterceptors() {
        if (CollectionUtils.isEmpty(this.interceptors)) {
            synchronized (this) {
                if (CollectionUtils.isEmpty(this.interceptors)) {
                    // 包括父容器以及子容器
                    Map<String, XRocketMQProducerInterceptor> beanMap = BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext, XRocketMQProducerInterceptor.class);
                    if (! CollectionUtils.isEmpty(beanMap)) {
                        this.interceptors = new ArrayList<>(beanMap.values());
                    }
                }
            }
        }
        return this.interceptors;
    }

    /**
     * 获取生产者属性配置
     * @return
     */
    private XRocketMQProperties getProducerProperties() {
        if (Objects.isNull(this.producerProperties)) {
            synchronized (XRocketMQProperties.class) {
                if (Objects.isNull(this.producerProperties)) {
                    this.producerProperties = applicationContext.getBean(XRocketMQProperties.class);
                }
            }
        }
        return this.producerProperties;
    }

    /**
     * 根据消息类型实例化对应的消息执行器
     * @param msgType   消息类型
     * @param producer  生产者实例
     * @param responseCallback 响应结果回调
     * @return
     */
    private XRocketMQProducerHandler getProducerHandler(MessageType msgType, Producer producer, XRocketMQProducerCallback responseCallback) {
        XRocketMQProducerHandler producerHandler = null;
        switch (msgType) {
            // 延时消息
            case MessageType.DELAY -> producerHandler = new XRocketMQProducerDelayHandler();
            // 有序消息
            case MessageType.ORDERLY -> producerHandler = new XRocketMQProducerOrderlyHandler();
            // 事务消息
            case MessageType.TRANSACTION -> producerHandler = new XRocketMQProducerTransactionHandler();
            // 普通消息
            default -> producerHandler = new XRocketMQProducerNormalHandler();
        }
        producerHandler.setProducer(producer);
        producerHandler.setInterceptors(this.getProducerInterceptors());
        producerHandler.setResponseCallback(responseCallback);
        return producerHandler;
    }

    /**
     * 提取方法参数
     * @param args
     * @return
     */
    private ArgsEntity getArgsEntity(Object[] args) {
        XRocketMQProducerParam param = null;
        XRocketMQProducerCallback callback = null;
        // 提取参数
        for (Object arg : args) {
            if (XRocketMQProducerParam.class.isAssignableFrom(arg.getClass())) {
                param = (XRocketMQProducerParam) arg;
            }
            else if (XRocketMQProducerCallback.class.isAssignableFrom(arg.getClass())) {
                callback = (XRocketMQProducerCallback) arg;
            }
        }
        return new ArgsEntity(param, callback);
    }

    /**
     * 参数实体
     */
    static class ArgsEntity {
        /* 消息请求参数 */
        XRocketMQProducerParam param;
        /* 返回结果回调 */
        XRocketMQProducerCallback callback;

        public ArgsEntity(XRocketMQProducerParam param, XRocketMQProducerCallback callback) {
            this.param = param;
            this.callback = callback;
        }
    }
}
