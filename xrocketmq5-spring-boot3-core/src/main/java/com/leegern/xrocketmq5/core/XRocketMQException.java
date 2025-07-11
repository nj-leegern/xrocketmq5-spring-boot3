package com.leegern.xrocketmq5.core;

/**
 *  自定义rocketmq异常
 */
public class XRocketMQException extends RuntimeException {

    public XRocketMQException(String message) {
        super(message);
    }

    public XRocketMQException(String message, Throwable cause) {
        super(message, cause);
    }

    public XRocketMQException(Throwable cause) {
        super(cause);
    }
}
