package com.spring;

import com.spring.annotations.Autowired;
import com.spring.annotations.Component;
import com.spring.annotations.ComponentScan;
import com.spring.annotations.Scope;
import com.spring.beans.BeanDefinition;
import com.spring.beans.SingletonFactory;
import com.spring.interfaces.BeanNameAware;
import com.spring.interfaces.BeanPostProcessor;
import com.spring.interfaces.InitializingBean;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class XINApplicationContext {
    //用以表示Spring配置文件的类
    private Class<?> configClass;

    //一级缓存：单例池
    private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>();

    //二级缓存：earlySingletonObjects
    private final Map<String, Object> earlySingletonObjects = new HashMap<>();

    //三级缓存：singletonFactories
    private final Map<String, Object> singletonFactories = new HashMap<>();

    private final ConcurrentHashMap<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>();

    private List<BeanPostProcessor> beanPostProcessorList = new ArrayList<>();

    public XINApplicationContext(Class<?> configClass) {
        this.configClass = configClass;
        // 解析配置类
        scan(configClass);
        // 实例并初始化每个bean
        for (String beanName : beanDefinitionMap.keySet()) {
            Object bean = getBean(beanName);
        }
    }

    /**
     * 将所有bean的beanDefinition放入beanDefinitionMap中
     * @param configClass 配置文件类
     */
    public void scan (Class<?> configClass) {
        //解析配置类
        //解析ComponentScan
        ComponentScan componentScan = configClass.getDeclaredAnnotation(ComponentScan.class);
        //扫描的路径
        String path = componentScan.value();
        //默认为配置类的路径
        if (path.equals("")) {
            String _path = configClass.getResource("").getPath();
            path = _path.substring(_path.indexOf("classes/")+8);
        }
        path = path.replace(".", "/");
        ClassLoader classLoader = XINApplicationContext.class.getClassLoader();//app
        //转化后的扫描的路径
        URL resource = classLoader.getResource(path);
        assert resource != null;
        //得到文件
        File file = new File(resource.getFile());
        //用于循环迭代
        Queue<File> fileQueue = new LinkedList<>();
        fileQueue.add(file);
        while (!fileQueue.isEmpty()) {
            file = fileQueue.poll();
            //如果file是文件夹，则将其中所有的文件加入队列中
            if (file.isDirectory()) {
                File[] files = file.listFiles();
                if (files != null) {
                    fileQueue.addAll(Arrays.asList(files));
                }
            }else {
                String fileName = file.getAbsolutePath();
                //说明是class文件
                if (fileName.endsWith(".class")) {
                    String className = fileName.substring(fileName.indexOf("classes")+8, fileName.indexOf(".class"));
                    //获取到类名
                    className = className.replace("\\", ".");
                    try {
                        //通过类加载器得到类
                        Class<?> clazz = classLoader.loadClass(className);
                        //当前类有Component注解
                        if (clazz.isAnnotationPresent(Component.class)) {
                            // BeanPostProcessor
                            if (BeanPostProcessor.class.isAssignableFrom(clazz)) {
                                BeanPostProcessor instance = (BeanPostProcessor) clazz.getDeclaredConstructor().newInstance();
                                beanPostProcessorList.add(instance);
                            }

                            //Bean解析类
                            BeanDefinition beanDefinition = new BeanDefinition();
                            beanDefinition.setClazz(clazz);
                            Component componentAnnotation = clazz.getAnnotation(Component.class);
                            String beanName = componentAnnotation.value().equals("") ?
                                    //类名首字母小写作为beanName
                                    clazz.getSimpleName().substring(0, 1).toLowerCase() + clazz.getSimpleName().substring(1)
                                    : componentAnnotation.value();

                            //当前类有Scope注解
                            if (clazz.isAnnotationPresent(Scope.class)) {
                                //判断是单例还是原型
                                Scope scopeAnnotation = clazz.getDeclaredAnnotation(Scope.class);
                                if (!scopeAnnotation.value().equals("singleton") && !scopeAnnotation.value().equals("prototype")) {
                                    throw new RuntimeException("Scope value wrong");
                                }else {
                                    beanDefinition.setScope(scopeAnnotation.value());
                                }
                            }
                            beanDefinitionMap.put(beanName, beanDefinition);
                        }
                    } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * 根据bean解析类创建bean
     * @param beanDefinition bean解析类
     * @return bean
     */
    public Object createBean(String beanName, BeanDefinition beanDefinition) {
        //先根据Clazz获取其Constructor
        Constructor<?> constructor = null;
        try {
            constructor = beanDefinition.getClazz().getConstructor();
            // 使得可以调用私有方法
            constructor.setAccessible(true);
            // 创建实例
            Object instance = constructor.newInstance();

            // 单例情况下考虑循环依赖
            if (beanDefinition.getScope().equals("singleton")) {
                // 提前暴露到三级缓存
                singletonFactories.put(beanName, new SingletonFactory(beanName, beanDefinition, instance));
            }

            for (Field declaredField : beanDefinition.getClazz().getDeclaredFields()) {
                //依赖注入
                //根据属性名
                if (declaredField.isAnnotationPresent(Autowired.class)) {
                    Object bean = getBean(declaredField.getName());
                    if (bean == null) {
                        throw new RuntimeException("Autowired 属性不存在");
                    }
                    declaredField.setAccessible(true);
                    declaredField.set(instance, bean);
                }
            }

            // Aware回调
            if (instance instanceof BeanNameAware) {
                ((BeanNameAware)instance).setBeanName(beanName);
            }

            // beanPostProcessor#postProcessBeforeInitialization
            for (BeanPostProcessor beanPostProcessor : beanPostProcessorList) {
                instance = beanPostProcessor.postProcessBeforeInitialization(instance, beanName);
            }

            // 初始化
            if (instance instanceof InitializingBean) {
                try {
                    ((InitializingBean)instance).afterPropertiesSet();
                }catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // beanPostProcessor#postProcessAfterInitialization
            for (BeanPostProcessor beanPostProcessor : beanPostProcessorList) {
                instance = beanPostProcessor.postProcessAfterInitialization(instance, beanName);
            }

            // 单例情况下考虑缓存升级并获取缓存中的实例
            if (beanDefinition.getScope().equals("singleton")) {
                instance = getSingletonObject(beanName);
                // 将二级缓存删除并升级到一级缓存
                singletonObjects.put(beanName, instance);
                earlySingletonObjects.remove(beanName);
            }

            return instance;
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 根据beanName获取bean
     * @param name bean的name
     * @return bean
     */
    public Object getBean(String name) {
        if (beanDefinitionMap.containsKey(name)) {
            BeanDefinition beanDefinition = beanDefinitionMap.get(name);
            if (beanDefinition.getScope().equals("singleton")) {
                Object singletonObject = getSingletonObject(name);
                // 如果依旧为空，则需要创建bean
                if (singletonObject == null) {
                    singletonObject = createBean(name, beanDefinition);
                }
                return singletonObject;
            }else if (beanDefinition.getScope().equals("prototype")) {
                return createBean(name, beanDefinition);
            }else {
                throw new RuntimeException("Scope value wrong");
            }
        }
        return null;
    }

    private Object getSingletonObject(String name) {
        // 一级缓存获取
        Object singletonObject = singletonObjects.get(name);
        // 二级缓存获取
        if (singletonObject == null) {
            singletonObject =  earlySingletonObjects.get(name);
            // 三级缓存获取
            if (singletonObject == null) {
                SingletonFactory singletonFactory = (SingletonFactory) singletonFactories.get(name);
                if (singletonFactory != null) {
                    singletonObject = singletonFactory.getObject();
                    // 此时需要缓存升级
                    if (singletonObject != null) {
                        // 放入二级缓存
                        earlySingletonObjects.put(name, singletonObject);
                        // 三级缓存删除
                        singletonFactories.remove(name);
                    }
                }
            }
        }
        return singletonObject;
    }
}
