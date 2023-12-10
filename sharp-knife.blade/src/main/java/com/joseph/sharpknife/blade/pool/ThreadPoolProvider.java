package com.joseph.sharpknife.blade.pool;

import org.springframework.core.annotation.Order;

import java.util.List;

/**
 * 线程池提供
 *
 * @author Joseph
 */
@Order
public interface ThreadPoolProvider {

    /**
     * 在Spring容器初始化完成后，框架会调用方法获取用户提供的线程池。
     * 线程池会在框架内维护，以实现未来的动态线程池
     *
     * @return thread pool
     */
    List<MonitoredThreadPool> buildPools();

}
