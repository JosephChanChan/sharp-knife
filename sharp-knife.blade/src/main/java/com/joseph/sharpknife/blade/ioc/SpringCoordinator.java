package com.joseph.sharpknife.blade.ioc;

import com.joseph.sharpknife.blade.unit.TaskMeta;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Joseph
 */
public class SpringCoordinator implements ApplicationContextAware {

    private ApplicationContext springContext ;

    /**
     * taskName -> taskMeta
     */
    private Map<String, TaskMeta> taskMetaMap = new HashMap<>();


    /**
     * 从Spring中获取托管bean，注意如果没有找到或无法实例化会抛出异常
     *
     * @param beanName beanName
     * @return bean
     */
    public Object getBean(String beanName) {
        return springContext.getBean(beanName);
    }

    /**
     * 从Spring中获取托管bean，注意如果没有找到或无法实例化会抛出异常
     *
     * @param beanName beanName
     * @param clazz clazz
     * @return bean
     */
    public <T> T getBean(String beanName, Class<T> clazz) {
        return springContext.getBean(beanName, clazz);
    }

    public boolean alreadyContain(String taskName) {
        return taskMetaMap.containsKey(taskName);
    }

    public void maintainTask(TaskMeta taskMeta) {
        taskMetaMap.put(taskMeta.getTaskName(), taskMeta);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.springContext = applicationContext;
    }
}
