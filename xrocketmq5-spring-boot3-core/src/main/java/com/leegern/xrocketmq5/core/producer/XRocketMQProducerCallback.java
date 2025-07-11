package com.leegern.xrocketmq5.core.producer;

import org.springframework.lang.Nullable;

/**
 * 生产者发送完消息之后的结果回调处理接口
 */
public interface XRocketMQProducerCallback {

    /**
     * 发送成功回调方法
     * @param response 返回结果
     */
    void onSuccess(XRocketMQProducerResponse response);

    /**
     * 发送失败回调方法
     * @param response 返回结果
     * @param ex       异常信息
     */
    void onFailure(XRocketMQProducerResponse response, @Nullable Throwable ex);

}
