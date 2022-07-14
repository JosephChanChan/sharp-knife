package com.joseph.sharpknife.blade.pool;

import com.joseph.sharpknife.blade.constnat.LogConstant;
import com.joseph.sharpknife.blade.context.ScheduleRequest;
import com.joseph.sharpknife.blade.exception.SharpKnifeException;
import com.joseph.sharpknife.blade.queue.MonitoredTaskQueue;
import com.joseph.sharpknife.blade.unit.Hooker;
import com.joseph.sharpknife.blade.unit.RunWrapper;
import com.joseph.sharpknife.blade.unit.TaskMeta;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;

/**
 * @author Joseph
 */
@Slf4j
public abstract class MonitoredThreadPool extends ThreadPoolExecutor {

    protected MonitoredThreadPool(int corePoolSize,
                                  int maximumPoolSize,
                                  long keepAliveTime,
                                  TimeUnit unit,
                                  MonitoredTaskQueue<Runnable> workQueue,
                                  ThreadFactory threadFactory,
                                  RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
    }

    public <Ctx, Res> void handle(boolean async, RunWrapper<Ctx, Res> runWrapper) {
        beforeWork(Thread.currentThread(), runWrapper);
        String taskName = runWrapper.getTaskName();
        if (async) {
            log.info("{} taskName={} submitted to executor pool", LogConstant.LOG_HEAD, taskName);
            toPool(runWrapper);
        }
        else {
            log.info("{} taskName={} keep executed by worker={}", LogConstant.LOG_HEAD, taskName, Thread.currentThread().getName());
            keepHandle(runWrapper);
        }
    }

    /**
     * 提交给线程池，交由具体线程池执行
     */
    protected abstract <Ctx, Res> void toPool(RunWrapper<Ctx, Res> runWrapper);

    /**
     * 由当前线程继续执行
     */
    protected <Ctx, Res> void keepHandle(RunWrapper<Ctx, Res> runWrapper) {
        runWrapper.run();
    }

    protected void beforeWork(Thread t, Runnable r) {
        TaskMeta taskMeta = ((RunWrapper<?, ?>) r).getTaskMeta();
        log.info("{} taskName={} executing now", LogConstant.LOG_HEAD, taskMeta.getTaskName());

        // 记录排队时间
        ((RunWrapper<?, ?>) r).markStartExecuteTime();
        ((RunWrapper<?, ?>) r).addHook(new Hooker(0, (unused) -> afterWork(t, r)));
    }

    protected void afterWork(Thread t, Runnable r) {
        // 上报排队时间 & 执行时间
        RunWrapper<?, ?> runWrapper = (RunWrapper<?, ?>) r;
        runWrapper.markDoneTime();
        TaskMeta taskMeta = runWrapper.getTaskMeta();
        ScheduleRequest<?, ?> scheduleRequest = runWrapper.getScheduleRequest();
        long cost ;
        if ((cost = runWrapper.executeTimeout()) > 0) {
            // 上报任务执行超时
            SharpKnifeException timeoutException = new SharpKnifeException(
                    String.format("%s execute timeout taskName=%s cost=%s limit=%s",
                            LogConstant.LOG_HEAD, taskMeta.getTaskName(), cost, taskMeta.getTimeout()));
            taskMeta.getTaskNode().onError(scheduleRequest.getUserContext(), scheduleRequest.getUserResult(), timeoutException);
        }
        else {
            log.info("{} taskName={} has been executed", LogConstant.LOG_HEAD, taskMeta.getTaskName());
        }
    }

    /**
     * 强制线程池命名
     *
     * @return 线程池名称
     */
    abstract String getThreadPoolName();

}
