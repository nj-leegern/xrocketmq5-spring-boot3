package com.leegern.xrocketmq5.core;

public class XRocketMQConstants {

    /* rocketmq基础属性配置 */
    public static final String ROCKETMQ_KEY_PREFIX        =     "rocketmq.";
    public static final String ROCKETMQ_KEY_ENABLED       =     ROCKETMQ_KEY_PREFIX + "enabled";


    /* bean name */
    public static final String BEAN_NAME_CONSUMER_POST_PROCESSOR           =   "x-rocketMQConsumerBeanPostProcessor";
    public static final String BEAN_NAME_PRODUCER_REGISTER_POST_PROCESSOR  =   "x-rocketMQProducerBeanDefinitionRegistryPostProcessor";


    /* 是否开启ssl */
    public static final Boolean SSL_ENABLED             =   false;
    /* 消息消费重试次数 */
    public static final Integer RETRY_CONSUME_TIMES     =   16;
    /* 消费并发度 */
    public static final Integer CONSUME_THREAD_NUM      =   20;
    /* 本地最大缓存条数 */
    public static final Integer MAX_CACHE_MSG_NUM       =   1024;
    /* 本地最大缓存大小(byte) */
    public static final Integer MAX_CACHE_MSG_SIZE      =   64 * 1024 * 1024;
    /* 消费请求超时时间 */
    public static final Integer CONSUME_REQUEST_TIMEOUT =   5 * 1000;
    /* 消费者名连接符 */
    public static final String  CONSUMER_NAME_DELIMITER  =   ".";
    /* 默认的过滤标签 */
    public static final String DEFAULT_FILTER_EXPRESS    =   "*";

    /* 生产者包路径连接符 */
    public static final String PRODUCER_PACKAGE_DELIMITER  =   ",";
    /* 应用扫描路径属性 */
    public static final String BASE_PACKAGE_NAME        =   "basePackages";
    /* 应用扫描路径类属性 */
    public static final String BASE_PACKAGE_CLAZZ_NAME  =   "basePackageClasses";
    /* 生产者接口扫描路径 */
    public static final String  SCAN_BASE_PACKAGE       =    "com.leegern";
    /* 请求超时时间 */
    public static final Integer SEND_MSG_TIMEOUT        =    5 * 1000;
    /* 消息发送重试次数 */
    public static final Integer RETRY_SEND_TIMES        =    3;
    /* 消息大小 */
    public static final Integer MAX_MESSAGE_SIZE        =    4 * 1024 * 1024;
    /* 事务异常检查间隔 */
    public static final Integer CHECK_TX_INTERVAL       =    60 * 1000 ;

    // 默认的bean作用域
    public static final String  DEFAULT_BEAN_SCOPE      =   "singleton";

    // 消息类型验证提示信息
    public static final String  CHECK_NULL_TIPS         =   "'%s' is empty when invoking '%s.%s()'";

    // 公共的生产者实例名
    public static final String  PRODUCER_NAME_PUBLIC    =   "producer.public";
    // 事务生产者实例名
    public static final String  PRODUCER_NAME_TX        =   "producer.tx.%s.%s";
}
