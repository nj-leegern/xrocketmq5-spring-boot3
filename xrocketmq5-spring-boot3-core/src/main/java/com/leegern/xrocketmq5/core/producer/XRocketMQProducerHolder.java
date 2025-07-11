package com.leegern.xrocketmq5.core.producer;

import org.apache.rocketmq.client.apis.producer.Producer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 生产者实例缓存器
 */
public class XRocketMQProducerHolder {

    /* Producer实例持有者 */
    private Map<String, Producer> producerHolder = new HashMap<>();


    /**
     * 缓存Producer实例
     * @param producerName  生产者者名称
     * @param producer      生产者实例
     */
    public void putProducer(String producerName, Producer producer) {
        producerHolder.put(producerName, producer);
    }

    /**
     * 获取Producer实例
     * @param producerName 生产者名称
     * @return
     */
    public Producer getProducer(String producerName) {
        return producerHolder.get(producerName);
    }

    /**
     * 关闭生产者实例连接
     * @throws IOException
     */
    public void closeAll() throws IOException {
        for (Map.Entry<String, Producer> entry : producerHolder.entrySet()) {
            Producer producer = entry.getValue();
            if (!Objects.isNull(producer)) {
                producer.close();
            }
        }
        producerHolder.clear();
    }
}
