package com.leegern.xrocketmq5.core.producer.annotation;


import com.leegern.xrocketmq5.core.enums.MessageType;

import java.lang.annotation.*;

/**
 * 自定义生产者接口方法注解
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface XRocketMQPusher {

    /**
     * 话题名称
     * @return
     */
    String topicName();

    /**
     * 消息类型
     * @see MessageType
     * @return
     */
    MessageType msgType() default MessageType.NORMAL;


    /**
     * 是否异步发送消息
     * @return
     */
    boolean enableAsync() default false;

    /**
     * 消息标签表达式
     * @return
     */
    String tagExpress() default "";
}
