package com.leegern.xrocketmq5.core.consumer.annotation;

import com.leegern.xrocketmq5.core.XRocketMQConstants;
import com.leegern.xrocketmq5.core.enums.SelectorType;

import java.lang.annotation.*;

/**
 * 自定义消费消息方法注解
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy. RUNTIME)
@Documented
public @interface XRocketMQConsumeListener {

    /**
     * Topic name
     */
    String topicName();

    /**
     * Consumers of the same role is required to have exactly same subscriptions and consumerGroup to correctly achieve
     * load balance. It's required and needs to be globally unique.
     */
    String consumerGroup();

    /**
     * Control how to selector message
     */
    SelectorType selectorType() default SelectorType.TAG;

    /**
     * Control which message can be select.
     */
    String selectorExpress() default XRocketMQConstants.DEFAULT_FILTER_EXPRESS;
}
