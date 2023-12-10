package com.joseph.sharpknife.blade.ioc;

import com.joseph.sharpknife.blade.kits.AssertKit;
import com.joseph.sharpknife.blade.pool.MonitoredThreadPool;
import com.joseph.sharpknife.blade.pool.SharpKnifePoolManager;
import com.joseph.sharpknife.blade.constnat.LogConstant;
import com.joseph.sharpknife.blade.pool.ThreadPoolProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 调度框架收集任务节点
 *
 * @author Joseph
 */
@Slf4j
public class ThreadPoolCollector implements BeanPostProcessor {

    private static final Set<String> COLLECTED = new HashSet<>(2);


    /**
     * 在spring bean实例化后，afterProperties函数后，init-method函数后调用。
     * 收集线程池
     *
     * @param bean bean
     * @param beanName beanName
     * @return thread pool config
     * @throws BeansException BeansException
     */
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (!(bean instanceof ThreadPoolProvider)) {
            return bean;
        }
        if (COLLECTED.contains(beanName)) {
            log.warn("{} ThreadPoolProvider={} collect repeatedly!", LogConstant.LOG_HEAD, beanName);
            return bean;
        }

        log.info("{} handle ThreadPoolProvider={}", LogConstant.LOG_HEAD, beanName);

        ThreadPoolProvider provider = (ThreadPoolProvider) bean;

        List<MonitoredThreadPool> monitoredThreadPools ;
        try {
            monitoredThreadPools = provider.buildPools();
        }
        catch (RuntimeException e) {
            log.error("{} ThreadPoolProvider={} buildPools error", LogConstant.LOG_HEAD, beanName);
            throw e;
        }

        AssertKit.notEmpty(monitoredThreadPools, String.format("ThreadPoolProvider=%s must provide MonitoredThreadPool", beanName));

        monitoredThreadPools.forEach(e -> {
            log.info("{} ThreadPoolProvider={} collect a pool={}", LogConstant.LOG_HEAD, beanName, e.getThreadPoolName());
            SharpKnifePoolManager.getManager().putPool(e.getThreadPoolName(), e);
        });

        COLLECTED.add(beanName);

        return bean;
    }

}
