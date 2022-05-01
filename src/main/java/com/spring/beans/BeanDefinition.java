package com.spring.beans;

public class BeanDefinition {
    private Class<?> clazz;

    private final String DEFAULT_SCOPE = "singleton";

    private String scope = DEFAULT_SCOPE;

    public BeanDefinition() {
    }

    public BeanDefinition(Class<?> clazz, String scope) {
        this.clazz = clazz;
        this.scope = scope;
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public void setClazz(Class<?> claszz) {
        this.clazz = claszz;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }
}
