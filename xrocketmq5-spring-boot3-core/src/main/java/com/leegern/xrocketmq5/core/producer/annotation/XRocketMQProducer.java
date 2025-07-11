package com.leegern.xrocketmq5.core.producer.annotation;

import java.lang.annotation.*;

/**
 * 自定义生产者接口注解
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface XRocketMQProducer {

}
