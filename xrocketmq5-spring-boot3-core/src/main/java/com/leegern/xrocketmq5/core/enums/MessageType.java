package com.leegern.xrocketmq5.core.enums;

/**
 * 消息类型
 */
public enum MessageType {

    /**
     * 普通消息
     */
    NORMAL,

    /**
     * 有序消息
     */
    ORDERLY,

    /**
     * 延时消息
     */
    DELAY,

    /**
     * 事务消息
     */
    TRANSACTION
}
