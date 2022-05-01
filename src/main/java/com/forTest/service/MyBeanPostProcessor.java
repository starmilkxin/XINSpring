package com.forTest.service;

import com.spring.annotations.Component;
import com.spring.interfaces.BeanPostProcessor;

@Component
public class MyBeanPostProcessor implements BeanPostProcessor {
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        System.out.println("before init:" + beanName);
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        System.out.println("after init:" + beanName);
        return bean;
    }
}
