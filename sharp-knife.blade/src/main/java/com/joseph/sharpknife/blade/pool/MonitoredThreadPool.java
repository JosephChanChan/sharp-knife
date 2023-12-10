package com.joseph.sharpknife.blade.pool;

import com.joseph.common.kit.StringKit;
import com.joseph.common.kit.collections.CollectionsKit;
import com.joseph.sharpknife.blade.exception.SharpKnifeException;
import com.joseph.sharpknife.blade.config.GlobalConfigHolder;
import com.joseph.sharpknife.blade.constnat.LogConstant;
import com.joseph.sharpknife.blade.context.InheritBehaviour;
import com.joseph.sharpknife.blade.context.ScheduleRequest;
import com.joseph.sharpknife.blade.queue.MonitoredTaskQueue;
import com.joseph.sharpknife.blade.unit.Hooker;
import com.joseph.sharpknife.blade.unit.RunWrapper;
import com.joseph.sharpknife.blade.unit.TaskMeta;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.util.concurrent.*;

/**
 * @author Joseph
 */
@Slf4j
public abstract class MonitoredThreadPool extends ThreadPoolExecutor {

    private GlobalConfigHolder configHolder;

    private final InheritBehaviour inheritBehaviour ;

    protected MonitoredThreadPool(int corePoolSize,
                                  int maximumPoolSize,
                                  long keepAliveTime,
                                  TimeUnit unit,
                                  MonitoredTaskQueue<Runnable> workQueue,
                                  ThreadFactory threadFactory,
                                  RejectedExecutionHandler handler,
                                  InheritBehaviour inheritBehaviour) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
        this.inheritBehaviour = inheritBehaviour;
    }

    public void setConfigHolder(GlobalConfigHolder configHolder) {
        this.configHolder = configHolder;
    }

    public <Ctx, Res> void handle(boolean async, RunWrapper<Ctx, Res> runWrapper) {
        beforeWork(runWrapper);
        String taskName = runWrapper.getTaskName();
        if (async) {
            log.info("{} taskName={} submitted to executor pool", LogConstant.LOG_HEAD, taskName);
            if (null != inheritBehaviour) {
                inheritBehaviour.prepareInherit(runWrapper.getInheritMap());
            }
            toPool(runWrapper);
        }
        else {
            log.info("{} taskName={} keep executed by worker={}", LogConstant.LOG_HEAD, taskName, Thread.currentThread().getName());
            runWrapper.run();
        }
    }

    /**
     * 提交给线程池，交由具体线程池执行
     */
    protected abstract <Ctx, Res> void toPool(RunWrapper<Ctx, Res> runWrapper);

    protected void beforeWork(Runnable r) {
        // 提交到线程池前，记录开始调度时间
        ((RunWrapper<?, ?>) r).markStartScheduleTime();
        ((RunWrapper<?, ?>) r).addHook(new Hooker(0, (unused) -> afterWork(r)));
    }

    protected void afterWork(Runnable r) {
        // 上报排队时间 & 执行时间
        RunWrapper<?, ?> runWrapper = (RunWrapper<?, ?>) r;
        runWrapper.markDoneTime();
        TaskMeta taskMeta = runWrapper.getTaskMeta();
        ScheduleRequest<?, ?> scheduleRequest = runWrapper.getScheduleRequest();
        long cost ;
        if ((cost = runWrapper.executeTimeout()) > 0) {
            // 上报任务执行超时
            SharpKnifeException timeoutException = new SharpKnifeException(
                    String.format("%s execute timeout taskName=%s cost=%s limit=%s startScheduleTime=%s startExecuteTime=%s doneTime=%s queueTime=%s",
                            LogConstant.LOG_HEAD,
                            taskMeta.getTaskName(),
                            cost,
                            taskMeta.getTimeout(),
                            runWrapper.getStartScheduleTime(),
                            runWrapper.getStartExecuteTime(),
                            runWrapper.getDoneTime(),
                            (runWrapper.getStartExecuteTime() - runWrapper.getStartScheduleTime())));
            taskMeta.getTaskNode().onError(scheduleRequest.getUserContext(), scheduleRequest.getUserResult(), timeoutException);
        }
        else {
            log.info("{} taskName={} has been executed", LogConstant.LOG_HEAD, taskMeta.getTaskName());
        }
    }

    public InheritBehaviour getInheritBehaviour() {
        return inheritBehaviour;
    }

    /**
     * 强制线程池命名
     *
     * @return 线程池名称
     */
    public abstract String getThreadPoolName();

}
