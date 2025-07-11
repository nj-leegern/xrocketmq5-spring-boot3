package com.leegern.xrocketmq5.core.consumer;

import com.leegern.xrocketmq5.core.XRocketMQConstants;
import com.leegern.xrocketmq5.core.XRocketMQException;
import com.leegern.xrocketmq5.core.consumer.annotation.XRocketMQConsumeListener;
import com.leegern.xrocketmq5.core.consumer.listener.XRocketMQConsumerDefaultMessageListener;
import com.leegern.xrocketmq5.core.consumer.listener.XRocketMQConsumerMessageListener;
import com.leegern.xrocketmq5.core.enums.SelectorType;
import com.leegern.xrocketmq5.core.properties.XRocketMQProperties;
import org.apache.rocketmq.client.apis.ClientConfiguration;
import org.apache.rocketmq.client.apis.ClientException;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.StaticSessionCredentialsProvider;
import org.apache.rocketmq.client.apis.consumer.FilterExpression;
import org.apache.rocketmq.client.apis.consumer.FilterExpressionType;
import org.apache.rocketmq.client.apis.consumer.PushConsumer;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.*;

/**
 * 消费者消费消息监听处理器
 */
public class XRocketMQConsumeListenerProcessor {

    /* Spring容器上下文 */
    private ApplicationContext applicationContext;

    /* 消费者实例持有者 */
    private XRocketMQConsumerHolder consumerHolder;

    /* 消费者属性 */
    private XRocketMQProperties consumerProperties;


    /**
     * 自定义构造器
     * @param applicationContext
     * @param consumerHolder
     * @param consumerProperties
     */
    public XRocketMQConsumeListenerProcessor(ApplicationContext applicationContext, XRocketMQConsumerHolder consumerHolder,
                                             XRocketMQProperties consumerProperties) {
        this.consumerHolder      =  consumerHolder;
        this.consumerProperties  =  consumerProperties;
        this.applicationContext  =  applicationContext;
    }


    /**
     * 处理消费监听器
     * @param consumeListener 消费监听器
     * @param method          消费方法
     * @param bean            实例对象
     */
    public void processConsumeListener(XRocketMQConsumeListener consumeListener, Method method, Object bean) {
        // 参数验证
        this.checkNull(consumeListener, consumerProperties.getEndpoints());

        // 获取当前bean实例的可调用Method对象
        Method invocableMethod = AopUtils.selectInvocableMethod(method, bean.getClass());
        // 获取方法参数类型
        Class<?> parameterTypeClazz = this.getParameterTypeClazz(invocableMethod);
        // 消费消息拦截器
        List<XRocketMQConsumerInterceptor> interceptors = this.getConsumerInterceptors();
        // 消费消息监听器
        XRocketMQConsumerMessageListener messageListener =
                new XRocketMQConsumerDefaultMessageListener(bean, parameterTypeClazz, invocableMethod, interceptors, consumeListener.topicName());

        PushConsumer pushConsumer = null;
        try {
            // 生成消息过滤器
            FilterExpression filterExpression = this.createFilterExpression(consumeListener.selectorType(), consumeListener.selectorExpress());
            // 创建消费者实例
            pushConsumer = this.buildConsumer(consumeListener.topicName(), consumeListener.consumerGroup(), filterExpression, messageListener);
        } catch (Exception e) {
            throw new XRocketMQException(e);
        }

        // 消费者名称
        String consumerName = this.generateConsumerName(consumeListener.consumerGroup(), bean.getClass().getName(), method.getName());
        // 缓存消费者实例
        this.consumerHolder.putConsumer(consumerName, pushConsumer);
    }

    /**
     * 创建消费者实例
     * @param topic            订阅话题
     * @param consumerGroup    消费组名
     * @param filterExpression 过滤表达式
     * @param messageListener  消息监听器
     * @return
     */
    private PushConsumer buildConsumer(String topic, String consumerGroup, FilterExpression filterExpression,
                                       XRocketMQConsumerMessageListener messageListener) throws ClientException {
        final ClientServiceProvider provider = ClientServiceProvider.loadService();
        // 客户端配置项
        ClientConfiguration clientConfiguration = ClientConfiguration.newBuilder()
                .setCredentialProvider(new StaticSessionCredentialsProvider(consumerProperties.getAccessKey(), consumerProperties.getSecretKey()))
                .setEndpoints(consumerProperties.getEndpoints())
                .enableSsl(consumerProperties.getSslEnabled())
                .setRequestTimeout(Duration.ofMillis(consumerProperties.getConsumer().getConsumeRequestTimeout()))
                .build();
        // 创建消费者实例
        return provider.newPushConsumerBuilder()
                .setConsumerGroup(consumerGroup)
                .setClientConfiguration(clientConfiguration)
                .setConsumptionThreadCount(consumerProperties.getConsumer().getConsumeThreadNum())
                .setMaxCacheMessageCount(consumerProperties.getConsumer().getMaxCacheMsgNum())
                .setMaxCacheMessageSizeInBytes(consumerProperties.getConsumer().getMaxCacheMsgSize())
                .setSubscriptionExpressions(Collections.singletonMap(topic, filterExpression))
                .setMessageListener(messageListener)
                .build();
    }

    /**
     * 获取方法参数类型
     * @param invocableMethod 消费消息的方法
     * @return
     */
    private Class<?> getParameterTypeClazz(Method invocableMethod) {
        // 获取参数类型
        Class<?>[] parameterTypes = invocableMethod.getParameterTypes();
        if (ObjectUtils.isEmpty(parameterTypes) || parameterTypes.length != 1) {
            throw new XRocketMQException("The method parameter type is invalid.");
        }
        return parameterTypes[0];
    }

    /**
     * 获取容器中所有消费拦截器集合
     * @return
     */
    private List<XRocketMQConsumerInterceptor> getConsumerInterceptors() {
        // 包括父容器以及子容器
        Map<String, XRocketMQConsumerInterceptor> beanMap = BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext, XRocketMQConsumerInterceptor.class);
        if (! CollectionUtils.isEmpty(beanMap)) {
            return new ArrayList<>(beanMap.values());
        }
        return null;
    }

    /**
     * 创建消息过滤器
     * @param selectorType     选择器类型
     * @param selectorExpress  选择器表达式
     * @return
     */
    private FilterExpression createFilterExpression(SelectorType selectorType, String selectorExpress) {
        FilterExpression filterExpression = new FilterExpression();
        if (Objects.nonNull(selectorType) && StringUtils.hasText(selectorExpress)) {
            filterExpression = new FilterExpression(selectorExpress, (selectorType == SelectorType.TAG ? FilterExpressionType.TAG : FilterExpressionType.SQL92));
        }
        else if (StringUtils.hasText(selectorExpress)) {
            filterExpression = new FilterExpression(selectorExpress, FilterExpressionType.TAG);
        }
        return filterExpression;
    }

    /**
     * 生成消费者名称
     * @param consumerGroup 消费组
     * @param clazzName     类名
     * @param methodName    方法名
     * @return
     */
    private String generateConsumerName(String consumerGroup, String clazzName, String methodName) {
        return String.join(XRocketMQConstants.CONSUMER_NAME_DELIMITER, consumerGroup, clazzName, methodName);
    }

    /**
     * 参数检查
     * @param consumeListener
     * @param endpoints
     */
    private void checkNull(XRocketMQConsumeListener consumeListener, String endpoints) {
        String errMsg = "";
        if (! StringUtils.hasText(endpoints)) {
            errMsg = "The 'endpoint' of rocketmq is empty.";
        }
        else if (! StringUtils.hasText(consumeListener.topicName())) {
            errMsg = "The consumer 'topicName' of rocketmq is empty.";
        }
        else if (! StringUtils.hasText(consumeListener.consumerGroup())) {
            errMsg = "The consumer 'consumerGroup' of rocketmq is empty.";
        }
        else if (Objects.isNull(consumeListener.selectorType())) {
            errMsg = "The consumer 'selectorType' of rocketmq is empty.";
        }

        if (StringUtils.hasText(errMsg)) {
            throw new XRocketMQException(errMsg);
        }
    }
}
