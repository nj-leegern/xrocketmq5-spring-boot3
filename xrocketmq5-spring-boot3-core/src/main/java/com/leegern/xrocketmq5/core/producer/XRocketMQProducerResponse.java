package com.leegern.xrocketmq5.core.producer;

/**
 * 生产者发送消息后的返回结果包装
 */
public class XRocketMQProducerResponse {

    // 成功标识  true 成功, false 失败
    private Boolean success;

    // 消息ID
    private String messageId;

    // 扩展信息
    private String extInfo;


    public XRocketMQProducerResponse() {
        this.success = false;
    }

    public XRocketMQProducerResponse(Boolean success) {
        this.success = success;
    }


    public Boolean getSuccess() {
        return success;
    }
    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getMessageId() {
        return messageId;
    }
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getExtInfo() {
        return extInfo;
    }
    public void setExtInfo(String extInfo) {
        this.extInfo = extInfo;
    }

    @Override
    public String toString() {
        return "DWRocketMQProducerResponse{" +
                "success=" + success +
                ", messageId='" + messageId + '\'' +
                ", extInfo='" + extInfo + '\'' +
                '}';
    }
}
