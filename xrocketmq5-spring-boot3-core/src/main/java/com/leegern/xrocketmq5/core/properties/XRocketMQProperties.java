package com.leegern.xrocketmq5.core.properties;

import com.leegern.xrocketmq5.core.XRocketMQConstants;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * rocketmq配置
 */
@ConfigurationProperties(prefix = "rocketmq")
public class XRocketMQProperties {

    /* 服务端地址(多节点用分号分隔)*/
    private String endpoints;

    /* 访问密钥 */
    private String accessKey;

    /* 保密的密钥 */
    private String secretKey;

    /* 是否开启SSL */
    private Boolean sslEnabled = XRocketMQConstants.SSL_ENABLED;

    /* 生产者配置 */
    private XRocketMQProducerProperties producer = new XRocketMQProducerProperties();

    /* 消费者配置 */
    private XRocketMQConsumerProperties consumer = new XRocketMQConsumerProperties();


    public String getEndpoints() {
        return endpoints;
    }
    public void setEndpoints(String endpoints) {
        this.endpoints = endpoints;
    }

    public String getAccessKey() {
        return accessKey;
    }
    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }
    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public Boolean getSslEnabled() {
        return sslEnabled;
    }
    public void setSslEnabled(Boolean sslEnabled) {
        this.sslEnabled = sslEnabled;
    }

    public XRocketMQProducerProperties getProducer() {
        return producer;
    }
    public void setProducer(XRocketMQProducerProperties producer) {
        this.producer = producer;
    }

    public XRocketMQConsumerProperties getConsumer() {
        return consumer;
    }
    public void setConsumer(XRocketMQConsumerProperties consumer) {
        this.consumer = consumer;
    }

    /**
     * 生产者属性全局配置
     */
    public static class XRocketMQProducerProperties {

        /* 扫描的包路径(多个用逗号分隔) */
        private String scanBasePackage = XRocketMQConstants.SCAN_BASE_PACKAGE;

        /* 请求超时时间(单位毫秒) */
        private Integer sendMsgTimeout = XRocketMQConstants.SEND_MSG_TIMEOUT;

        /* 消息发送重试次数 */
        private Integer retrySendTimes = XRocketMQConstants.RETRY_SEND_TIMES;

        /* 消息大小 */
        private Integer maxMessageSize = XRocketMQConstants.MAX_MESSAGE_SIZE;

        /* 事务异常检查间隔 */
        private Integer checkTransactionInterval= XRocketMQConstants.CHECK_TX_INTERVAL;


        public String getScanBasePackage() {
            return scanBasePackage;
        }
        public void setScanBasePackage(String scanBasePackage) {
            this.scanBasePackage = scanBasePackage;
        }

        public Integer getSendMsgTimeout() {
            return sendMsgTimeout;
        }
        public void setSendMsgTimeout(Integer sendMsgTimeout) {
            this.sendMsgTimeout = sendMsgTimeout;
        }

        public Integer getRetrySendTimes() {
            return retrySendTimes;
        }
        public void setRetrySendTimes(Integer retrySendTimes) {
            this.retrySendTimes = retrySendTimes;
        }

        public Integer getMaxMessageSize() {
            return maxMessageSize;
        }
        public void setMaxMessageSize(Integer maxMessageSize) {
            this.maxMessageSize = maxMessageSize;
        }

        public Integer getCheckTransactionInterval() {
            return checkTransactionInterval;
        }
        public void setCheckTransactionInterval(Integer checkTransactionInterval) {
            this.checkTransactionInterval = checkTransactionInterval;
        }
    }

    /**
     *  消费者属性全局配置
     */
    public static class XRocketMQConsumerProperties {

        /* 消费组名称 */
        private String consumerGroup;

        /* 消费请求超时时间 */
        private Integer consumeRequestTimeout = XRocketMQConstants.CONSUME_REQUEST_TIMEOUT;

        /* 消息消费重试次数 */
        private Integer retryConsumeTimes = XRocketMQConstants.RETRY_CONSUME_TIMES;

        /* 消费并发线程数 */
        private Integer consumeThreadNum = XRocketMQConstants.CONSUME_THREAD_NUM;

        /* 本地最大缓存消息数量 */
        private Integer maxCacheMsgNum = XRocketMQConstants.MAX_CACHE_MSG_NUM;

        /* 本地最大缓存消息大小 */
        private Integer maxCacheMsgSize = XRocketMQConstants.MAX_CACHE_MSG_SIZE;


        public String getConsumerGroup() {
            return consumerGroup;
        }
        public void setConsumerGroup(String consumerGroup) {
            this.consumerGroup = consumerGroup;
        }

        public Integer getConsumeRequestTimeout() {
            return consumeRequestTimeout;
        }
        public void setConsumeRequestTimeout(Integer consumeRequestTimeout) {
            this.consumeRequestTimeout = consumeRequestTimeout;
        }

        public Integer getRetryConsumeTimes() {
            return retryConsumeTimes;
        }
        public void setRetryConsumeTimes(Integer retryConsumeTimes) {
            this.retryConsumeTimes = retryConsumeTimes;
        }

        public Integer getConsumeThreadNum() {
            return consumeThreadNum;
        }
        public void setConsumeThreadNum(Integer consumeThreadNum) {
            this.consumeThreadNum = consumeThreadNum;
        }

        public Integer getMaxCacheMsgNum() {
            return maxCacheMsgNum;
        }
        public void setMaxCacheMsgNum(Integer maxCacheMsgNum) {
            this.maxCacheMsgNum = maxCacheMsgNum;
        }

        public Integer getMaxCacheMsgSize() {
            return maxCacheMsgSize;
        }
        public void setMaxCacheMsgSize(Integer maxCacheMsgSize) {
            this.maxCacheMsgSize = maxCacheMsgSize;
        }
    }
}
