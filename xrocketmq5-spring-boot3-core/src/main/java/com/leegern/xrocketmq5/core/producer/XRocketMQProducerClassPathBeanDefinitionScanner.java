package com.leegern.xrocketmq5.core.producer;

import com.leegern.xrocketmq5.core.XRocketMQConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;

import java.util.Set;

/**
 * 自定义类路径扫描器, 扫描出@DWRocketMQProducer注解的接口类
 */
public class XRocketMQProducerClassPathBeanDefinitionScanner extends ClassPathBeanDefinitionScanner {

    private static final Logger LOGGER = LoggerFactory.getLogger(XRocketMQProducerClassPathBeanDefinitionScanner.class);

    /* Spring容器上下文 */
    private ApplicationContext applicationContext;

    /* 生产者实例持有者 */
    private XRocketMQProducerHolder producerHolder;

    /* 生产者接口FactoryBean */
    private Class<?> producerFactoryBeanClazz = XRocketMQProducerFactoryBean.class;


    /**
     * 自定义构造器
     * @param registry
     * @param applicationContext
     * @param producerHolder
     */
    public XRocketMQProducerClassPathBeanDefinitionScanner(BeanDefinitionRegistry registry, ApplicationContext applicationContext,
                                                           XRocketMQProducerHolder producerHolder) {
        super(registry);
        this.applicationContext  =  applicationContext;
        this.producerHolder      =  producerHolder;
    }



    /**
     * 扫描生产者@DWRocketMQProducer修饰的接口包
     * @param basePackages
     * @return
     */
    @Override
    protected Set<BeanDefinitionHolder> doScan(String... basePackages) {
        Set<BeanDefinitionHolder> beanDefinitionHolders = super.doScan(basePackages);
        if (! beanDefinitionHolders.isEmpty()) {
            // 处理生产者bean定义, 指定其为FactoryBean实例
            beanDefinitionHolders.forEach(this::registerBeanDefinition);
        }
        return beanDefinitionHolders;
    }



    @Override
    protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
        return beanDefinition.getMetadata().isInterface() && beanDefinition.getMetadata().isIndependent();
    }


    /**
     * 注册自定义的生产者bean定义, 指定其类为FactoryBean
     * @param beanDefinitionHolder
     */
    private void registerBeanDefinition(BeanDefinitionHolder beanDefinitionHolder) {
        AbstractBeanDefinition definition = (AbstractBeanDefinition) beanDefinitionHolder.getBeanDefinition();

        // producer接口的Class
        String beanClassName = definition.getBeanClassName();
        if (LOGGER.isInfoEnabled())
            LOGGER.info(String.format("Creating bean with name '%s' and '%s' producerInterface", beanDefinitionHolder.getBeanName(), beanClassName));
        // 设置构造器参数->producer接口的Class对象
        definition.getConstructorArgumentValues().addGenericArgumentValue(beanClassName);
        // 修改producer接口Class对象为DWRocketMQProducerFactoryBean.class
        definition.setBeanClass(this.producerFactoryBeanClazz);
        // 设置属性->applicationContext & producerHolder
        definition.getPropertyValues().add("applicationContext", this.applicationContext);
        definition.getPropertyValues().add("producerHolder",     this.producerHolder);
        // 注入类型
        definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
        // 作用域
        definition.setScope(XRocketMQConstants.DEFAULT_BEAN_SCOPE);
    }
}
