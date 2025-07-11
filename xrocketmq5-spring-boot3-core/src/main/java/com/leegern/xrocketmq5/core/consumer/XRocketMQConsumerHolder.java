package com.leegern.xrocketmq5.core.consumer;

import org.apache.rocketmq.client.apis.consumer.PushConsumer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 消费者实例缓存器
 */
public class XRocketMQConsumerHolder {

    /* PushConsumer实例持有者 */
    private Map<String, PushConsumer> consumerHolder = new HashMap<>();


    /**
     * 缓存PushConsumer实例
     * @param consumerName  消费者名称
     * @param pushConsumer  消费者实例
     */
    public void putConsumer(String consumerName, PushConsumer pushConsumer) {
        consumerHolder.put(consumerName, pushConsumer);
    }

    /**
     * 获取PushConsumer实例
     * @param consumerName 消费者名称
     * @return
     */
    public PushConsumer getConsumer(String consumerName) {
        return consumerHolder.get(consumerName);
    }

    /**
     * 关闭消费者连接
     * @throws IOException
     */
    public void closeAll() throws IOException {
        for (Map.Entry<String, PushConsumer> entry : consumerHolder.entrySet()) {
            PushConsumer pushConsumer = entry.getValue();
            if (!Objects.isNull(pushConsumer)) {
                pushConsumer.close();
            }
        }
        consumerHolder.clear();
    }
}
