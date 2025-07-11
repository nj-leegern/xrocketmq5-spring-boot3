package com.leegern.xrocketmq5.core.producer;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.context.ApplicationContext;

/**
 * 自定义生产者(producer)接口FactoryBean, 主要为了创建接口代理类
 * @param <T>
 */
public class XRocketMQProducerFactoryBean<T> implements FactoryBean<T> {

    /* 应用上下文 */
    private ApplicationContext applicationContext;

    /* 生产者接口clazz */
    private Class<T> producerInterfaceClazz;

    /* 生产者实例持有者 */
    private XRocketMQProducerHolder producerHolder;

    /**
     * 自定义构造器
     * @param producerInterfaceClazz
     */
    public XRocketMQProducerFactoryBean(Class<T> producerInterfaceClazz) {
        this.producerInterfaceClazz = producerInterfaceClazz;
    }


    /**
     * 创建producer接口代理对象
     * @return
     * @throws Exception
     */
    @Override
    public T getObject() throws Exception {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(this.producerInterfaceClazz);
        enhancer.setCallback(new XRocketMQProducerProxy(applicationContext, producerInterfaceClazz, producerHolder));
        enhancer.setUseCache(true);
        //noinspection unchecked
        return (T) enhancer.create();
    }

    @Override
    public Class<?> getObjectType() {
        return this.producerInterfaceClazz;
    }


    /**
     * 'applicationContext' of getter
     * @return
     */
    public ApplicationContext getApplicationContext() {
        return this.applicationContext;
    }
    /**
     * 'applicationContext' of setter
     * @param applicationContext
     */
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * 'producerHolder' of getter
     * @return
     */
    public XRocketMQProducerHolder getProducerHolder() {
        return producerHolder;
    }

    /**
     * 'producerHolder' of setter
     * @param producerHolder
     */
    public void setProducerHolder(XRocketMQProducerHolder producerHolder) {
        this.producerHolder = producerHolder;
    }
}
