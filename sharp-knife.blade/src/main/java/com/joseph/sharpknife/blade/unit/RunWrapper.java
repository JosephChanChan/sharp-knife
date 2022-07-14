package com.joseph.sharpknife.blade.unit;

import com.joseph.common.kit.collections.CollectionsKit;
import com.joseph.sharpknife.blade.constnat.LogConstant;
import com.joseph.sharpknife.blade.context.EventWaiter;
import com.joseph.sharpknife.blade.context.ExecutionResult;
import com.joseph.sharpknife.blade.context.ScheduleRequest;
import com.joseph.sharpknife.blade.exception.SharpKnifeException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * @author Joseph
 */
@Slf4j
@Getter
public class RunWrapper<Ctx, Res> implements Runnable {

    private TaskMeta taskMeta;

    /**
     * 提交到线程池开始调度时间，包括线程池中排队和执行时间
     */
    private long startScheduleTime;

    /**
     * 工作线程开始执行时间
     * 排队时间=startExecuteTime-startScheduleTime
     */
    private long startExecuteTime;

    /**
     * 任务完成时间
     */
    private long doneTime;

    private final ScheduleRequest<Ctx, Res> scheduleRequest;

    private List<Hooker> hooks ;

    public RunWrapper(TaskMeta taskMeta, ScheduleRequest<Ctx, Res> scheduleRequest) {
        this.taskMeta = taskMeta;
        this.scheduleRequest = scheduleRequest;
    }

    public void addHook(Hooker hooker) {
        if (null == hooks) {
            hooks = new ArrayList<>(2);
        }
        hooks.add(hooker);
    }

    @Override
    public void run() {
        TaskNode<Ctx, Res> taskNode = taskMeta.getTaskNode();
        BiConsumer<Ctx, Res> task = taskNode.getTask();
        Ctx userContext = scheduleRequest.getUserContext();
        Res userResult = scheduleRequest.getUserResult();
        ExecutionResult executionResult = scheduleRequest.getRequestPipeline().getExecutionResult();
        EventWaiter waiter = scheduleRequest.getRequestPipeline().getWaiter();
        try {
            task.accept(userContext, userResult);
            taskNode.onSuccess(userContext, userResult);
        }
        catch (Exception e) {
            // 这里有必要感知异常，分开用户代码的异常和框架的异常

            Exception exception = e;
            if (scheduleRequest.isSuppressStackTrace()) {
                exception = SharpKnifeException.suppressStackTraces(exception);
            }
            // 异常回调通知
            exceptionNotify(exception, taskNode);
            // 终止链路图
            if (scheduleRequest.isInterruptWhileError()) {
                executionResult.setError(exception);
                waiter.wakeUpWhileError();
            }
            // 统计异常数&熔断机制

        }
        finally {
            // 任务节点计数器减一
            waiter.finishOneTask();
            handleHooks();
        }
    }

    private void exceptionNotify(Exception e, TaskNode<Ctx, Res> taskNode) {
        try {
            taskNode.onError(scheduleRequest.getUserContext(), scheduleRequest.getUserResult(), e);
        }
        catch (Exception callbackException) {
            // 异常回调通知还出错误...
            log.error("{} taskName={} exception callback notify error!", LogConstant.LOG_HEAD, callbackException);
        }
    }

    public TaskMeta getTaskMeta() {
        return this.taskMeta;
    }

    public String getTaskName() {
        return this.taskMeta.getTaskName();
    }

    /**
     * 处理回调
     */
    protected void handleHooks() {
        if (CollectionsKit.isNotEmpty(hooks)) {
            hooks.sort(Comparator.comparingInt(Hooker::getOrder));
            try {
                hooks.forEach(Hooker::invoke);
            }
            catch (Exception e) {
                // 这里捕获回调异常是防止异步线程消亡，免得需要再创建线程和内核上下文切换
                log.error("{} handleHooks error", LogConstant.LOG_HEAD, e);
            }
        }
    }

    public void markScheduleStartTime() {
        this.startScheduleTime = System.currentTimeMillis();
    }

    public void markStartExecuteTime() {
        this.startExecuteTime = System.currentTimeMillis();
    }

    public void markDoneTime() {
        this.doneTime = System.currentTimeMillis();
    }

    public long executeTimeout() {
        int limit = taskMeta.getTimeout();
        long cost = doneTime - startScheduleTime;
        return cost >= limit ? cost : 0;
    }
}
