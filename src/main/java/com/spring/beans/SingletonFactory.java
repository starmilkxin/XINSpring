package com.spring.beans;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

public class SingletonFactory {
    private String beanName;

    private BeanDefinition beanDefinition;

    private final Object instance;

    public SingletonFactory(String name, BeanDefinition beanDefinition, Object instance) {
        this.beanName = name;
        this.beanDefinition = beanDefinition;
        this.instance = instance;
    }

    /**
     * 判断对象是否需要代理，并返回处理后的对象
     * 代理只能用Cglib动态代理，因为jdk动态代理返回的类型是代理对象的接口类型，Cglib则是代理对象的类型
     * @return 如果需要代理，返回代理后的对象，否则则是原对象
     */
    public Object getObject() {
        MyInvocationHandler myInvocationHandler = new MyInvocationHandler(this.instance);
        return myInvocationHandler.getInstance();
    }
}

// Cglib动态代理
class MyInvocationHandler implements MethodInterceptor {
    private Object instance;

    public MyInvocationHandler(Object instance) {
        this.instance = instance;
    }

    public Object getInstance() {
        //在内存中创建一个动态类的字节码
        Enhancer enhancer=new Enhancer();//此时并没有做继承
        //设置父类,因为Cglib是针对指定的类生成一个子类，所以需要指定父类
        //除了完成继承关系外，还将父类所有的方法名反射过来，并在自己的类中
        enhancer.setSuperclass(this.instance.getClass());

        enhancer.setCallback(this);
        //创建并返回代理对象
        return enhancer.create();
    }

    @Override
    public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
        System.out.println("前代理: " + o.getClass());
        Object object = method.invoke(instance, objects);
//        或者Object object = methodProxy.invoke(instance, objects);
        System.out.println("后代理: " + o.getClass());
        return object;
    }
}
