package com.leegern.xrocketmq5.core.producer;

import org.apache.rocketmq.client.apis.message.Message;
import org.apache.rocketmq.shaded.com.google.common.base.MoreObjects;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * 重写消息实现@see org.apache.rocketmq.client.java.message.MessageImpl,
 *      公有化properties便于参数透传
 */
public class XRocketMQProducerMessage implements Message {

    protected final Collection<String> keys;
    final byte[] body;
    private final String topic;
    @Nullable
    private final String tag;
    @Nullable
    private final String messageGroup;
    @Nullable
    private final Long deliveryTimestamp;
    private final Map<String, String> properties;


    public XRocketMQProducerMessage(String topic, byte[] body, @Nullable String tag,
                                    Collection<String> keys, @Nullable String messageGroup,
                                    @Nullable Long deliveryTimestamp, Map<String, String> properties) {
        this.topic = topic;
        this.body = body;
        this.tag = tag;
        this.messageGroup = messageGroup;
        this.deliveryTimestamp = deliveryTimestamp;
        this.keys = keys;
        this.properties = properties;
    }


    @Override
    public String getTopic() {
        return this.topic;
    }

    @Override
    public ByteBuffer getBody() {
        return ByteBuffer.wrap(this.body);
    }

    /**
     * 属性公有化, 便于透传参数
     * @return
     */
    @Override
    public Map<String, String> getProperties() {
        return this.properties;
    }

    @Override
    public Collection<String> getKeys() {
        return new ArrayList<>(this.keys);
    }

    @Override
    public Optional<String> getTag() {
        return Optional.ofNullable(this.tag);
    }

    @Override
    public Optional<Long> getDeliveryTimestamp() {
        return Optional.ofNullable(this.deliveryTimestamp);
    }

    @Override
    public Optional<String> getMessageGroup() {
        return Optional.ofNullable(this.messageGroup);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("topic", this.topic)
                .add("tag", this.tag)
                .add("messageGroup", this.messageGroup)
                .add("deliveryTimestamp", this.deliveryTimestamp)
                .add("keys", this.keys)
                .add("properties", this.properties)
                .toString();
    }
}
