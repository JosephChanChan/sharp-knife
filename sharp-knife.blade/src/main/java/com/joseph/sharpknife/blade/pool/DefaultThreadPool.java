package com.joseph.sharpknife.blade.pool;

import com.joseph.sharpknife.blade.constnat.LogConstant;
import com.joseph.sharpknife.blade.context.EventWaiter;
import com.joseph.sharpknife.blade.context.ExecutionResult;
import com.joseph.sharpknife.blade.queue.MonitoredTaskQueue;
import com.joseph.sharpknife.blade.unit.RunWrapper;
import com.joseph.sharpknife.blade.unit.TaskMeta;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;

/**
 * @author Joseph
 */
@Slf4j
public class DefaultThreadPool extends MonitoredThreadPool {

    private String threadPoolName ;

    private DefaultThreadPool(int corePoolSize,
                              int maximumPoolSize,
                              long keepAliveTime,
                              TimeUnit unit,
                              MonitoredTaskQueue<Runnable> workQueue,
                              ThreadFactory threadFactory,
                              RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
    }

    public static DefaultThreadPool build(ThreadPoolBuilder builder /*, 这里应该还要有个监控线程池客户端管理对象 */) {
        DefaultThreadPool pool =  new DefaultThreadPool(
                builder.getCoreThreads(),
                builder.getMaxThreads(),
                builder.getKeepAliveTimes(),
                builder.getTimeUnit(),
                builder.getQueue(),
                builder.getThreadFactory(),
                builder.getRejectedStrategy()
        );
        pool.threadPoolName = builder.threadPoolName;
        return pool;
    }


    @Override
    public <Ctx, Res> void toPool(RunWrapper<Ctx, Res> runWrapper) {
        TaskMeta taskMeta = runWrapper.getTaskMeta();
        EventWaiter eventWaiter = runWrapper.getScheduleRequest().getRequestPipeline().getWaiter();
        ExecutionResult executionResult = runWrapper.getScheduleRequest().getRequestPipeline().getExecutionResult();
        try {
            this.execute(runWrapper);
        }
        catch (RejectedExecutionException e) {
            // 线程池饱和，短时间内继续提交任务意义不大，终止链路图
            executionResult.setError(e);
            eventWaiter.wakeUpWhileError();
            log.error("{} threadPool={}, taskType={}, taskName={} submit rejected={}",
                    LogConstant.LOG_HEAD,
                    threadPoolName,
                    taskMeta.getTaskType().getTypeName(),
                    taskMeta.getTaskName(),
                    e.getMessage());
        }
    }

    @Override
    public String getThreadPoolName() {
        return this.threadPoolName;
    }
}
