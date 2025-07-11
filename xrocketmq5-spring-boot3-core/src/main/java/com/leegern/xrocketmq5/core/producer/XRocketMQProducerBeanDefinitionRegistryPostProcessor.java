package com.leegern.xrocketmq5.core.producer;

import com.leegern.xrocketmq5.core.XRocketMQConstants;
import com.leegern.xrocketmq5.core.producer.annotation.XRocketMQProducer;
import com.leegern.xrocketmq5.core.properties.XRocketMQProperties;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * 自定义bean定义前置处理器, 扫描自定义注解并且动态修改bean定义
 */
public class XRocketMQProducerBeanDefinitionRegistryPostProcessor implements BeanDefinitionRegistryPostProcessor, ApplicationContextAware, DisposableBean {

    /* Spring容器上下文 */
    private ApplicationContext applicationContext;

    /* 生产者实例持有者 */
    private XRocketMQProducerHolder producerHolder;

    /* 扫描的包路径(多个用逗号分隔) */
    private String scanBasePackage;;


    /**
     * 自定义构造器
     */
    public XRocketMQProducerBeanDefinitionRegistryPostProcessor() {
        producerHolder = new XRocketMQProducerHolder();
    }


    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        // 扫描带有自定义DWRocketMQProducer注解的接口
        XRocketMQProducerClassPathBeanDefinitionScanner scanner
                = new XRocketMQProducerClassPathBeanDefinitionScanner(registry, applicationContext, producerHolder);
        scanner.addIncludeFilter(new AnnotationTypeFilter(XRocketMQProducer.class));
        scanner.scan(StringUtils.tokenizeToStringArray(this.getProducerPackageName(registry), ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS));
    }


    /**
     * 获取生成者包扫描路径
     * @param registry
     * @return
     */
    private String getProducerPackageName(BeanDefinitionRegistry registry) {
        String basePackage = XRocketMQConstants.SCAN_BASE_PACKAGE;
        Set<String> packages = null;
        if (! StringUtils.hasText(this.scanBasePackage)) {
            // 获取生产者属性配置
            XRocketMQProperties producerProperties = applicationContext.getBean(XRocketMQProperties.class);
            if (StringUtils.hasText(producerProperties.getProducer().getScanBasePackage())) {
                // 设置包扫描路径
                this.scanBasePackage = producerProperties.getProducer().getScanBasePackage();
            }
        }

        // 设置包扫描路径
        if (StringUtils.hasText(this.scanBasePackage)) {
            basePackage = this.scanBasePackage;
        }
        // 平台应用包路径作为扫描路径
        else if (!CollectionUtils.isEmpty((packages = this.extractAnnotationScanPackages(registry)))) {
            basePackage = String.join(XRocketMQConstants.PRODUCER_PACKAGE_DELIMITER, packages);
        }

        return basePackage;
    }

    /**
     * 获取应用ComponentScan配置的包路径
     * @param registry
     * @return
     */
    private Set<String> extractAnnotationScanPackages(BeanDefinitionRegistry registry) {
        Set<String> packages = new HashSet<>();
        for (String beanName : registry.getBeanDefinitionNames()) {
            BeanDefinition bd = registry.getBeanDefinition(beanName);
            if (bd instanceof AnnotatedBeanDefinition) {
                AnnotationMetadata metadata = ((AnnotatedBeanDefinition) bd).getMetadata();
                if (metadata.hasAnnotation(ComponentScan.class.getName())) {
                    Map<String, Object> attrs = metadata.getAnnotationAttributes(ComponentScan.class.getName());
                    if (! CollectionUtils.isEmpty(attrs)) {
                        // 处理 basePackages
                        Collections.addAll(packages, (String[]) attrs.get(XRocketMQConstants.BASE_PACKAGE_NAME));
                        // 处理 basePackageClasses
                        Class<?>[] basePackageClasses = (Class<?>[]) attrs.get(XRocketMQConstants.BASE_PACKAGE_CLAZZ_NAME);
                        for (Class<?> clazz : basePackageClasses) {
                            packages.add(clazz.getPackage().getName());
                        }
                    }
                }
            }
        }
        return packages;
    }


    /**
     * 'applicationContext' of setter
     * @param applicationContext
     * @throws BeansException
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * 'scanBasePackage' of setter
     * @param scanBasePackage
     */
    public void setScanBasePackage(String scanBasePackage) {
        this.scanBasePackage = scanBasePackage;
    }

    /**
     * destroy resource
     * @throws Exception
     */
    @Override
    public void destroy() throws Exception {
        if (Objects.nonNull(producerHolder)) {
            // 关闭生产者实例连接
            producerHolder.closeAll();
        }
    }
}
