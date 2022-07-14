package com.joseph.sharpknife.blade.pool;


import java.util.concurrent.atomic.AtomicInteger;

/**
 * 线程池工厂
 *
 * @author Joseph
 */
public class ThreadPoolFactory {

    private static final AtomicInteger THREAD_NUMBER = new AtomicInteger();


    public static int generateNumber() {
        return THREAD_NUMBER.incrementAndGet();
    }

    public static MonitoredThreadPool build(ThreadPoolBuilder builder) {
        MonitoredThreadPool threadPool ;
        switch (builder.getType()) {
            default:
            case DEFAULT:
                threadPool = DefaultThreadPool.build(builder);
                break;

            case HYSTRIX:
                throw new IllegalArgumentException("Hystrix thread pool does not support now");
        }
        return threadPool;
    }


}
