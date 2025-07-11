package com.leegern.xrocketmq5.autoconfigure;

import com.leegern.xrocketmq5.core.XRocketMQConstants;
import com.leegern.xrocketmq5.core.consumer.XRocketMQConsumerBeanPostProcessor;
import com.leegern.xrocketmq5.core.producer.XRocketMQProducerBeanDefinitionRegistryPostProcessor;
import com.leegern.xrocketmq5.core.properties.XRocketMQProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * rocketmq自动配置
 */
@Configuration
@ConditionalOnProperty(name = XRocketMQConstants.ROCKETMQ_KEY_ENABLED, havingValue = "true", matchIfMissing = false)
@EnableConfigurationProperties(XRocketMQProperties.class)
public class XRocketMQAutoConfiguration {


    /**
     * 生产者前置处理器
     * @param rocketMQProperties 生产者属性配置
     * @return
     */
    @Bean(XRocketMQConstants.BEAN_NAME_PRODUCER_REGISTER_POST_PROCESSOR)
    @ConditionalOnClass(XRocketMQProperties.class)
    @ConditionalOnMissingBean(XRocketMQProducerBeanDefinitionRegistryPostProcessor.class)
    public XRocketMQProducerBeanDefinitionRegistryPostProcessor newDWRocketMQProducerBeanDefinitionRegistryPostProcessor(
            XRocketMQProperties rocketMQProperties
    ) {
        XRocketMQProducerBeanDefinitionRegistryPostProcessor definitionRegistryPostProcessor = new XRocketMQProducerBeanDefinitionRegistryPostProcessor();
        // 生产者接口包路径
        definitionRegistryPostProcessor.setScanBasePackage(rocketMQProperties.getProducer().getScanBasePackage());
        return definitionRegistryPostProcessor;
    }

    /**
     * 消费者后置处理器
     * @return
     */
    @Bean(XRocketMQConstants.BEAN_NAME_CONSUMER_POST_PROCESSOR)
    @ConditionalOnClass(XRocketMQProperties.class)
    @ConditionalOnMissingBean(XRocketMQConsumerBeanPostProcessor.class)
    public XRocketMQConsumerBeanPostProcessor newDWRocketMQConsumerBeanPostProcessor() {
        return new XRocketMQConsumerBeanPostProcessor();
    }


}
