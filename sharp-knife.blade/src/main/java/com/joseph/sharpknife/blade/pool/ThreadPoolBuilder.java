package com.joseph.sharpknife.blade.pool;

import com.joseph.sharpknife.blade.constnat.CommonConstant;
import com.joseph.sharpknife.blade.unit.RunWrapper;
import com.joseph.sharpknife.blade.constnat.LogConstant;
import com.joseph.sharpknife.blade.context.InheritBehaviour;
import com.joseph.sharpknife.blade.queue.MonitoredTaskQueue;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.concurrent.*;

/**
 * @author Joseph
 */
@Slf4j
@Getter
public class ThreadPoolBuilder {

    ThreadPoolEnums type;

    String threadPoolName;

    int coreThreads = CommonConstant.CORE_CPUS;

    int maxThreads = CommonConstant.MAX_THREADS;

    long keepAliveTimes = 60;

    TimeUnit timeUnit = TimeUnit.SECONDS;

    /**
     * 任务队列
     */
    MonitoredTaskQueue<Runnable> queue;

    /**
     * 默认的拒绝策略
     */
    RejectedExecutionHandler rejectedStrategy = (r, executor) -> {
        if (r instanceof RunWrapper && executor instanceof MonitoredThreadPool) {
            throw new RejectedExecutionException(String.format(
                    "%s, threadPool=%s, taskType=%s, too heavily and reject a task!",
                    LogConstant.LOG_HEAD,
                    ((MonitoredThreadPool) executor).getThreadPoolName(),
                    ((RunWrapper<?, ?>) r).getTaskMeta().getTaskType().getTypeName()));
        }
    };

    /**
     * 默认的线程工厂
     */
    ThreadFactory threadFactory = r -> {
        Thread worker = new Thread(r);
        worker.setName(String.format(CommonConstant.THREAD_PREFIX, ThreadPoolFactory.generateNumber()));
        return worker;
    };

    /**
     * 父子线程变量传承行为
     */
    InheritBehaviour inheritBehaviour ;

    public ThreadPoolBuilder(ThreadPoolEnums type,
                             String threadPoolName,
                             int coreThreads,
                             int maxThreads,
                             long keepAliveTimes,
                             TimeUnit timeUnit,
                             MonitoredTaskQueue<Runnable> queue,
                             RejectedExecutionHandler rejectedStrategy,
                             ThreadFactory threadFactory,
                             InheritBehaviour inheritBehaviour) {
        this.type = type;
        this.threadPoolName = threadPoolName;
        this.coreThreads = coreThreads;
        this.maxThreads = maxThreads;
        this.queue = queue;
        this.inheritBehaviour = inheritBehaviour;
        if (keepAliveTimes >= 0) {
            this.keepAliveTimes = keepAliveTimes;
        }
        if (null != timeUnit) {
            this.timeUnit = timeUnit;
        }
        if (null != rejectedStrategy) {
            this.rejectedStrategy = rejectedStrategy;
        }
        if (null != threadFactory) {
            this.threadFactory = threadFactory;
        }

        if (null == type) {
            throw new IllegalArgumentException("type can't be null");
        }
        if (StringUtils.isEmpty(this.threadPoolName)) {
            throw new IllegalArgumentException("threadPoolName can't be blank");
        }
        if (this.coreThreads <= 0) {
            throw new IllegalArgumentException("coreThreads must be positive number");
        }
        if (this.maxThreads < this.coreThreads) {
            throw new IllegalArgumentException("maxThreads must greater than coreThreads");
        }
        if (this.queue == null) {
            throw new IllegalArgumentException("MonitoredTaskQueue can't be blank");
        }
    }

}
