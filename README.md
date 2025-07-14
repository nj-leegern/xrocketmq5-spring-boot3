# xrocketmq5-spring-boot3
This project aims to help developers quickly integrate RocketMQ5 with Spring Boot3, and the dependent component versions are as follows:
- JDK -> 21
- Spring boot -> 3.2.1
- RocketMQ Client -> 5.0.7
- RocketMQ Server -> 5.3.3

## 1. 应用配置

### 1.1 基础配置
设置rocketmq.enabled=true开启rocketmq消息队列，并且设置服务的endpoints，如果启用了授权还需配置accessKey和secretKey。

| 配置项             | 名称                                   | 是否必填 | 备注                      |
| ------------------ | -------------------------------------- | -------- | ------------------------- |
| rocketmq.enabled   | 启用开关，true开启，false关闭          | 是       | 默认是false               |
| rocketmq.endpoints | 服务地址，host:port，若多个使用";"分隔 | 是       | 5.x版本通常是rmqproxy地址 |
| rocketmq.accessKey | 授权标识                               | 否       | 若开启安全访问才需配置    |
| rocketmq.secretKey | 密钥                                   | 否       | 若开启安全访问才需配置    |

举个栗子：

```properties
rocketmq.enabled=true
rocketmq.endpoints=192.168.1.192:28080;192.168.1.193:28080
rocketmq.accessKey=
rocketmq.secretKey=
```

### 1.2 生产者配置(producer)

| 配置项                            | 名称                        | 是否必填 | 备注                        |
| --------------------------------- | --------------------------- | -------- |---------------------------|
| rocketmq.producer.scanBasePackage | 生产者包路径(多个用","分隔) | 是       | @XRocketMQProducer修饰类的包路径 |
| rocketmq.producer.sendMsgTimeout  | 请求超时时间(单位毫秒)      | 否       | 默认值5000毫秒                 |
| rocketmq.producer.retrySendTimes  | 消息发送重试次数            | 否       | 默认是3次                     |

举个栗子：

```properties
rocketmq.producer.scanBasePackage=com.leegern.business.producer
rocketmq.producer.sendMsgTimeout=5000
rocketmq.producer.retrySendTimes=3
```

### 1.3 消费者配置(consumer)

| 配置项                                  | 名称                           | 是否必填 | 备注           |
| --------------------------------------- | ------------------------------ | -------- | -------------- |
| rocketmq.consumer.consumeRequestTimeout | 请求超时时间(单位毫秒)         | 否       | 默认值5000毫秒 |
| rocketmq.consumer.consumeThreadNum      | 消费并发线程数                 | 否       | 默认20         |
| rocketmq.consumer.maxCacheMsgNum        | 本地最大缓存消息数量           | 否       | 默认1024条     |
| rocketmq.consumer.maxCacheMsgSize       | 本地最大缓存消息大小(单位byte) | 否       | 默认64MB       |

举个栗子：

```properties
rocketmq.consumer.consumeRequestTimeout=5000
rocketmq.consumer.consumeThreadNum=20
rocketmq.consumer.maxCacheMsgNum=1024
rocketmq.consumer.maxCacheMsgSize=67108864
```



## 2. 生产者(Producer)

### 2.1 @XRocketMQProducer与@XRocketMQPusher用法

- @XRocketMQProducer注解用于修饰生产者接口上

- @XRocketMQPusher注解用于修饰生产者的具体方法上，并且此注解提供了以下属性参数：

  | 属性        | 名称                             | 是否必填 | 备注                                                         |
    | ----------- | -------------------------------- | -------- | :----------------------------------------------------------- |
  | topicName   | 主题名称                         | 是       |                                                              |
  | msgType     | 消息类型，默认'NORMAL'(普通消息) | 是       | 包括：NORMAL(普通消息)、ORDERLY(顺序消息)、DELAY(延时消息)、TRANSACTION(事务消息) |
  | tagExpress  | 消息标签表达式，默认空           | 否       | 消息过滤的标签表达式                                         |
  | enableAsync | 异步发送标识，默认false          | 否       |                                                              |

  举个栗子：

  ```java
  @XRocketMQProducer
  public interface IOrderProducer {
  
      @XRocketMQPusher(topicName = "order_mq_test", msgType = MessageType.NORMAL)
      void send4normal(XRocketMQProducerNormalParam<OrderEntity> producerParam, XRocketMQProducerCallback callback);
  
  }
  ```

​		producerParam：生产者参数实例，分别对应不同消息类型参数，具体见 [2.2生产者参数用法](#_22-生产者参数用法)

​        producerCallback：生产者发送完消息之后的结果回调处理实例，具体见 [2.3生产者响应回调用法](#_23-生产者响应回调用法)

### 2.2 生产者参数用法

生产者参数根据消息类型分为：普通、延时、顺序、事务等消息参数类型。

#### 2.2.1 XRocketMQProducerNormalParam普通消息参数

| 属性       | 名称       | 是否必填 | 备注                   |
| ---------- | ---------- | -------- | ---------------------- |
| businessId | 业务消息ID | 否       | 用于表示消息的唯一标识 |
| msgBody    | 消息实体   | 是       | 消息对象实例           |

举个栗子：

```java
XRocketMQProducerNormalParam<OrderEntity> param = XRocketMQProducerNormalParam.<OrderEntity>builder()
                .businessId("ORDER_001")
                .msgBody(new OrderEntity("NORMAL"))
                .build();
```
#### 2.2.2 XRocketMQProducerDelayParam延时消息参数

| 属性         | 名称              | 是否必填 | 备注                   |
| ------------ | ----------------- | -------- | ---------------------- |
| businessId   | 业务消息ID        | 否       | 用于表示消息的唯一标识 |
| msgBody      | 消息实体          | 是       | 消息对象实例           |
| delaySecTime | 延时时长(单位:秒) | 是       | 消息延迟发送的间隔时间 |

举个例子：

```java
XRocketMQProducerDelayParam<OrderEntity> param = XRocketMQProducerDelayParam.<OrderEntity>builder()
                .businessId("ORDER_DELAY_001")
                .msgBody(new OrderEntity("DELAY"))
                .delaySecTime(30)
                .build();
```

#### 2.2.3 XRocketMQProducerOrderlyParam顺序消息参数

| 属性       | 名称           | 是否必填 | 备注                                         |
| ---------- | -------------- | -------- | -------------------------------------------- |
| businessId | 业务消息ID     | 否       | 用于表示消息的唯一标识                       |
| msgBody    | 消息实体       | 是       | 消息对象实例                                 |
| orderGroup | 消息排序分组名 | 是       | 同一组消息被分配至一个队列中保证消费的有序性 |

举个栗子：

```java
XRocketMQProducerOrderlyParam<OrderEntity> param = XRocketMQProducerOrderlyParam.<OrderEntity>builder()
                .businessId("ORDER_ORDERLY_001")
                .msgBody(new OrderEntity("ORDERLY"))
                .orderGroup("ORDER_NO_1001")
                .build();
```

#### 2.2.4 XRocketMQProducerTransactionParam事务消息参数

| 属性               | 名称           | 是否必填 | 备注                       |
| ------------------ | -------------- | -------- | -------------------------- |
| businessId         | 业务消息ID     | 否       | 用于表示消息的唯一标识     |
| msgBody            | 消息实体       | 是       | 消息对象实例               |
| transactionChecker | 事务回查器实例 | 是       | 用于检查本地事务执行的结果 |
| transactionAction  | 本地事务执行器 | 是       | 执行本地事务的接口实现     |

- 事务回查器XRocketMQProducerTransactionChecker，继承此抽象类覆写doCheckTransaction(T msgBody)，检查本地事务状态是否已提交，返回结果：true->已经提, false->未提交, null->未知
- 本地事务执行器XRocketMQProducerTransactionAction，实现此接口覆写doTransactionAction()，执行本地事务业务逻辑，返回结果：true->成功，false->失败；

举个栗子：

```java
// 订单服务实例
@Resource
IOrderService orderService;

......

OrderEntity orderEntity = new OrderEntity("ORDERLY", "ORDER_NO_1000");
XRocketMQProducerTransactionParam<OrderEntity> param = XRocketMQProducerTransactionParam.<OrderEntity>builder()
                .businessId("ORDER_TRANSACTION_001")
                .msgBody(orderEntity)
                .transactionChecker(new XRocketMQProducerTransactionChecker<OrderEntity>() {
                    @Override
                    protected Boolean doCheckTransaction(OrderEntity msgBody) {
                        // 根据订单ID检查订单事务状态
                        return orderService.checkOrderById(msgBody.getOrderId());
                    }
                })
                .transactionAction(new XRocketMQProducerTransactionAction() {
                    @Override
                    public boolean doTransactionAction() {
                        // 变更订单状态
                        return orderService.modifyOrderStatus(orderEntity.getOrderId());
                    }
                })
                .build();
```
### 2.3 生产者响应回调用法

- 生产者发送完消息之后的结果回调处理接口XRocketMQProducerCallback，覆写onSuccess(XRocketMQProducerResponse response)、onFailure(XRocketMQProducerResponse response, @Nullable Throwable ex)方法，其中onSuccess方法表示成功的回调，onFailure方法表示失败的回调；

- 生产者发送消息后的返回结果对象XRocketMQProducerResponse，其中属性如下：

  | 属性      | 名称                       | 是否非空 | 备注                                       |
    | --------- | -------------------------- | -------- | ------------------------------------------ |
  | success   | 是否成功，true:是 false:否 | 是       |                                            |
  | messageId | 消息ID                     |          | rocketmq内部消息唯一标识                   |
  | extInfo   | 消息扩展信息               |          | 包括：主题、标签、消息分组、延时间隔等信息 |

举个栗子：

```java
   public class NormalProducerCallback implements XRocketMQProducerCallback {
		
       /**
         * 发送成功回调方法
         * @param response 返回结果
         */
        @Override
        public void onSuccess(XRocketMQProducerResponse response) {
            LOGGER.info("Normal message success ===>{}", response.toString());
        }
		
        /**
         * 发送失败回调方法
         * @param response 返回结果
         * @param ex       异常信息
         */
        @Override
        public void onFailure(XRocketMQProducerResponse response, Throwable ex) {
            LOGGER.error("Normal message failed ===>{}, ex: {}", response.toString(), ex);
        }
    }
```

### 2.4 生产者示例

#### 2.4.1 定义生产者接口发送方法

```java
@XRocketMQProducer
public interface IOrderProducer {
	
    // 发送普通消息
    @XRocketMQPusher(topicName = "order_mq_test")
    void send4normal(XRocketMQProducerNormalParam<OrderEntity> producerParam, XRocketMQProducerCallback callback);
	
    // 发送延时消息
    @XRocketMQPusher(topicName = "order_mq_test_delay", msgType = MessageType.DELAY)
    void send4delay(XRocketMQProducerDelayParam<OrderEntity> producerParam, XRocketMQProducerCallback callback);
	
    // 发送顺序消息
    @XRocketMQPusher(topicName = "order_mq_test_orderly", msgType = MessageType.ORDERLY)
    void send4orderly(XRocketMQProducerOrderlyParam<OrderEntity> producerParam, XRocketMQProducerCallback callback);
	
    // 发送事务消息
    @XRocketMQPusher(topicName = "order_mq_test_tx", msgType = MessageType.TRANSACTION)
    void send4transaction(XRocketMQProducerTransactionParam<OrderEntity> producerParam, XRocketMQProducerCallback callback);
    
}
```
#### 2.4.2 定义消息发送入口

```java
@Service
public class RocketmqServiceImpl implements IRocketmqService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RocketmqServiceImpl.class);
	
    // 生产者实例
    @Resource
    private IOrderProducer orderProducer;
	// 订单服务实例
	@Resource
	IOrderService orderService;
    
	// 发送普通消息
    @Override
    public void sendMsg4normal() {
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setVersion("5.3.0.1014");
        orderEntity.setName("ORDER");
        orderEntity.setMsgType("NORMAL");

        XRocketMQProducerNormalParam<OrderEntity> param = XRocketMQProducerNormalParam.<OrderEntity>builder()
                .businessId("ORDER_001")
                .msgBody(orderEntity)
                .build();

        orderProducer.send4normal(param, new XRocketMQProducerCallback() {
            @Override
            public void onSuccess(XRocketMQProducerResponse response) {
                LOGGER.info("*** sendMsg4normal success ===>{}", response.toString());
            }

            @Override
            public void onFailure(XRocketMQProducerResponse response, Throwable ex) {
                LOGGER.error("*** sendMsg4normal failed ===>{}, ex: {}", response.toString(), ex);
            }
        });
    }
	
    // 发送延时消息
    @Override
    public void sendMsg4delay() {
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setVersion("5.3.0.1014");
        orderEntity.setName("ORDER");
        orderEntity.setMsgType("DELAY");

        XRocketMQProducerDelayParam<OrderEntity> param = XRocketMQProducerDelayParam.<OrderEntity>builder()
                .businessId("ORDER_DELAY_001")
                .msgBody(orderEntity)
                .delaySecTime(30)
                .build();

        orderProducer.send4delay(param, new XRocketMQProducerCallback() {
            @Override
            public void onSuccess(XRocketMQProducerResponse response) {
                LOGGER.info("*** sendMsg4delay success ===>{}", response.toString());
            }

            @Override
            public void onFailure(XRocketMQProducerResponse response, Throwable ex) {
                LOGGER.error("*** sendMsg4delay failed ===>{}, ex: {}", response.toString(), ex);
            }
        });
    }
	
    // 发送顺序消息
    @Override
    public void sendMsg4orderly() {
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setVersion("5.3.0.1014");
        orderEntity.setName("ORDER");
        orderEntity.setMsgType("ORDERLY");

        XRocketMQProducerOrderlyParam<OrderEntity> param = XRocketMQProducerOrderlyParam.<OrderEntity>builder()
                .businessId("ORDER_ORDERLY_001")
                .msgBody(orderEntity)
                .orderGroup("ORDER_NO_1001")
                .build();

        orderProducer.send4orderly(param, new XRocketMQProducerCallback() {
            @Override
            public void onSuccess(XRocketMQProducerResponse response) {
                LOGGER.info("*** sendMsg4orderly success ===>{}", response.toString());
            }

            @Override
            public void onFailure(XRocketMQProducerResponse response, Throwable ex) {
                LOGGER.error("*** sendMsg4orderly failed ===>{}, ex: {}", response.toString(), ex);
            }
        });
    }
	
    // 发送事务消息
    @Override
    public void sendMsg4transaction() {
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setMsgType("TRANSACTION");
        orderEntity.setOrderId("ORDER_NO_1000");

        XRocketMQProducerTransactionParam<OrderEntity> param = XRocketMQProducerTransactionParam.<OrderEntity>builder()
                .businessId("ORDER_TRANSACTION_001")
                .msgBody(orderEntity)
                .transactionChecker(new XRocketMQProducerTransactionChecker<OrderEntity>() {
                    @Override
                    protected Boolean doCheckTransaction(OrderEntity msgBody) {
                        // 根据订单ID检查订单事务状态是否提交
                        return orderService.checkOrderById(msgBody.getOrderId());
                    }
                })
                .transactionAction(new XRocketMQProducerTransactionAction() {
                    @Override
                    public boolean doTransactionAction() {
                        // 变更订单状态
                        return orderService.modifyOrderStatus(orderEntity.getOrderId());
                    }
                })
                .build();

        orderProducer.send4transaction(param, new XRocketMQProducerCallback() {
            @Override
            public void onSuccess(XRocketMQProducerResponse response) {
                LOGGER.info("*** sendMsg4transaction success ===>{}", response.toString());
            }

            @Override
            public void onFailure(XRocketMQProducerResponse response, Throwable ex) {
                LOGGER.error("*** sendMsg4transaction failed ===>{}, ex: {}", response.toString(), ex);
            }
        });
    }
}    
```



## 3. 消费者(Consumer)

### 3.1 @XRocketMQConsumeListener用法

@XRocketMQConsumeListener用于修饰消费消息的方法上，并且此bean须交予Spring管理，此注解提供以下属性：

| 属性            | 名称                            | 是否必填 | 备注                                         |
| --------------- | ------------------------------- | -------- | -------------------------------------------- |
| topicName       | 订阅消息的主题                  | 是       |                                              |
| consumerGroup   | 消费组名称                      | 是       | 同一消费组下所有实例的topic、tag必须一致     |
| selectorType    | 消息选择器类型，默认是TAG       | 否       | 包括：TAG和SQL92(不推荐)                     |
| selectorExpress | 消息过滤表达式，默认是'*'(所有) | 否       | 与消息选择器类型相关联，支持TAG或SQL92表达式 |

举个栗子：

```java
@Service
public class RocketmqServiceImpl implements IRocketmqService {
    
    @XRocketMQConsumeListener(topicName = "order_mq_test", consumerGroup = "group_order_mq_test")
    @Override
    public void testConsumeMsg4normal(OrderEntity orderEntity) {
        LOGGER.info("consume 'normal' msg: {}", orderEntity.toString());
    }
    
}
```

### 3.2 消费者示例

定义一个由Spring托管的Bean，然后定义消息消费方法：

```java
@Service
public class RocketmqServiceImpl implements IRocketmqService {
    
    // 消费普通消息
    @XRocketMQConsumeListener(topicName = "order_mq_test", consumerGroup = "group_order_mq_test")
    @Override
    public void consumeMsg4normal(OrderEntity orderEntity) {
        LOGGER.info("consume 'normal' msg: {}", orderEntity.toString());
    }

	// 消费延时消息
    @XRocketMQConsumeListener(topicName = "order_mq_test_delay", consumerGroup = "group_order_mq_test_delay")
    @Override
    public void consumeMsg4delay(OrderEntity orderEntity) {
        LOGGER.info("consume 'delay' msg: {}", orderEntity.toString());
    }
	
    // 消费顺序消息
    @XRocketMQConsumeListener(topicName = "order_mq_test_orderly", consumerGroup = "group_order_mq_test_orderly")
    @Override
    public void consumeMsg4orderly(OrderEntity orderEntity) {
        LOGGER.info("consume 'orderly' msg: {}", orderEntity.toString());
    }
	
    // 消费事务消息
    @XRocketMQConsumeListener(topicName = "order_mq_test_tx", consumerGroup = "group_order_mq_test_tx")
    @Override
    public void consumeMsg4transaction(OrderEntity orderEntity) {
        LOGGER.info("consume 'transaction' msg: {}", orderEntity.toString());
    }
}
```



## 4. 消息拦截器

### 4.1 消息发送拦截器

此组件提供了消息<font color="red">**发送**</font>前后的拦截功能，增强自定义业务的植入能力，具体实现XRocketMQProducerInterceptor接口，并且覆写before(Message message)、after(Message message, @Nullable Throwable ex)方法，如果只对某些主题生效(默认是匹配所有主题)，覆写 matchTopics()方法，如若多个拦截器有顺序执行覆写getOrder()方法即可；

举个栗子：

```java
@Service("orderProducerInterceptor")
public class OrderProducerInterceptor implements XRocketMQProducerInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderProducerInterceptor.class);

    @Override
    public Message before(Message message) {
        // 透传TOKEN
        message.getProperties().put("TOKEN", "L2V4ZWN1dGlvbi10YXNrLTM5NTA0Mi1hc3NpZ25lZHRvbWUuaHRtbA");
        LOGGER.info("[orderProducerInterceptor] before message:{}", message);
        return message;
    }

    @Override
    public void after(Message message, Throwable ex) {
        LOGGER.info("[orderProducerInterceptor] after message:{}", message);
    }

    @Override
    public List<String> matchTopics() {
        return List.of("order_mq_test_delay");
    }

    @Override
    public int getOrder() {
        return XRocketMQProducerInterceptor.super.getOrder()-1;
    }
}
```

### 4.2 消息消费拦截器

此组件提供了消息<font color="red">**接收**</font>前后的拦截功能，增强自定义业务的植入能力，具体实现XRocketMQConsumerInterceptor接口，并且覆写before(MessageView messageView)、after(MessageView messageView, @Nullable Exception ex)方法，如果只对某些主题生效(默认是匹配所有主题)，需覆写 matchTopics()方法，如若多个拦截器有顺序执行需覆写getOrder()方法即可；

```java
@Service("orderConsumerInterceptor")
public class OrderConsumerInterceptor implements XRocketMQConsumerInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderConsumerInterceptor.class);

    @Override
    public MessageView before(MessageView messageView) {
        // 透传TOKEN
        String token = messageView.getProperties().get("TOKEN");
        LOGGER.info("[orderConsumerInterceptor] before MessageView:{}", messageView);
        return messageView;
    }

    @Override
    public void after(MessageView messageView, Exception ex) {
        LOGGER.info("[orderConsumerInterceptor] after MessageView:{}", messageView);
    }

    @Override
    public List<String> matchTopics() {
        return List.of("order_mq_test_delay");
    }

    @Override
    public int getOrder() {
        return XRocketMQConsumerInterceptor.super.getOrder();
    }
}
```

