package com.leegern.xrocketmq5.core.consumer;

import com.leegern.xrocketmq5.core.consumer.annotation.XRocketMQConsumeListener;
import com.leegern.xrocketmq5.core.properties.XRocketMQProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.AopInfrastructureBean;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 自定义消费者后置处理器, 对自定义消费监听器@DWRocketMQConsumeListener修饰的方法做消息消费增强处理
 */
public class XRocketMQConsumerBeanPostProcessor implements BeanPostProcessor, ApplicationContextAware, DisposableBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(XRocketMQConsumerBeanPostProcessor.class);

    /* 消费者实例持有者 */
    private final XRocketMQConsumerHolder consumerHolder = new XRocketMQConsumerHolder();

    /* 无注解修饰的clazz缓存集合 */
    private final Set<Class<?>> nonAnnotatedClasses = Collections.newSetFromMap(new ConcurrentHashMap<>(64));

    /* Spring容器上下文 */
    private ApplicationContext applicationContext;


    /**
     * 查找自定义消费监听器方法，实例化pushConsumer并且通过反射来消费消息
     * @param bean      bean实例
     * @param beanName  bean名称
     * @return
     * @throws BeansException
     */
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        // 跳过AOP基础设施Bean
        if (bean instanceof AopInfrastructureBean) {
            return bean;
        }
        // 获取被代理对象的最终目标类
        Class<?> targetClass = AopProxyUtils.ultimateTargetClass(bean);

        if (! nonAnnotatedClasses.contains(bean.getClass())) {
            // 查找消费消息监听器注解
            Map<Method, XRocketMQConsumeListener> methodMap = this.scanConsumeListenerAnnotation(targetClass);
            // 缓存无注解的bean的clazz
            if (CollectionUtils.isEmpty(methodMap)) {
                nonAnnotatedClasses.add(targetClass);
            }
            else {
                // 消费者属性配置
                XRocketMQProperties consumerProperties = applicationContext.getBean(XRocketMQProperties.class);
                methodMap.forEach(((method, dwRocketMQConsumeListener) -> {
                    // 消费监听处理器
                    new XRocketMQConsumeListenerProcessor(applicationContext, consumerHolder, consumerProperties).processConsumeListener(dwRocketMQConsumeListener, method, bean);
                }));
            }
        }

        return bean;
    }

    /**
     * 查找消费消息监听器注解
     * @param targetClass 目标对象clazz
     * @return
     */
    private Map<Method, XRocketMQConsumeListener> scanConsumeListenerAnnotation(Class<?> targetClass) {
        return MethodIntrospector.selectMethods(targetClass,
                (MethodIntrospector.MetadataLookup<XRocketMQConsumeListener>) method -> AnnotatedElementUtils.getMergedAnnotation(method, XRocketMQConsumeListener.class));
    }



    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void destroy() throws Exception {
        // 关闭消费者连接
        consumerHolder.closeAll();
    }
}
