package com.joseph.sharpknife.blade.unit;

import com.joseph.sharpknife.blade.context.ScheduleRequest;
import com.joseph.sharpknife.blade.exception.SharpKnifeException;
import com.joseph.sharpknife.blade.constnat.LogConstant;
import com.joseph.sharpknife.blade.context.EventWaiter;
import com.joseph.sharpknife.blade.context.ExecutionResult;
import com.joseph.sharpknife.blade.context.InheritBehaviour;
import com.joseph.sharpknife.blade.kits.CollectionsKit;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

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

    private final InheritBehaviour inheritBehaviour ;

    /**
     * 每次run执行完后需要执行的回调动作
     */
    private List<Hooker> hooks ;

    /**
     * 每个RunWrapper自己维护的变量传承器
     */
    private Map<String, Object> inheritMap = new HashMap<>();



    public RunWrapper(TaskMeta taskMeta, ScheduleRequest<Ctx, Res> scheduleRequest, InheritBehaviour inheritBehaviour) {
        this.taskMeta = taskMeta;
        this.scheduleRequest = scheduleRequest;
        this.inheritBehaviour = inheritBehaviour;
    }

    public void addHook(Hooker hooker) {
        if (null == hooks) {
            hooks = new ArrayList<>(2);
        }
        hooks.add(hooker);
    }

    @Override
    public void run() {
        // 任务已从线程池取出，开始执行。记录排队时间
        markStartExecuteTime();
        // 从inheritMap继承父线程传递的变量
        inherit();
        // 清除inheritMap
        wipeInherit();

        TaskNode<Ctx, Res> taskNode = taskMeta.getTaskNode();
        BiConsumer<Ctx, Res> task = taskNode.getTask();
        Ctx userContext = scheduleRequest.getUserContext();
        Res userResult = scheduleRequest.getUserResult();
        ExecutionResult executionResult = scheduleRequest.getRequestPipeline().getExecutionResult();
        EventWaiter waiter = scheduleRequest.getRequestPipeline().getWaiter();
        try {
            if (handlePrediction(taskMeta, scheduleRequest)) {
                task.accept(userContext, userResult);
                taskNode.onSuccess(userContext, userResult);
            }
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
            // 回调函数
            handleHooks();
            // 到这里线程执行完成，准备回池子重新拿任务
            threadSelfClear();
        }
    }

    private void inherit() {
        if (null != inheritBehaviour) {
            inheritBehaviour.inheritFromParent(inheritMap);
        }
    }

    private void wipeInherit() {
        inheritMap.clear();
    }

    private boolean handlePrediction(TaskMeta taskMeta, ScheduleRequest<Ctx, Res> scheduleRequest) {
        boolean abort = scheduleRequest.isInterruptWhileError();
        boolean suppressStackTrace = scheduleRequest.isSuppressStackTrace();
        Ctx userContext = scheduleRequest.getUserContext();
        Res userResult = scheduleRequest.getUserResult();

        ExecutionResult executionResult = scheduleRequest.getRequestPipeline().getExecutionResult();
        EventWaiter waiter = scheduleRequest.getRequestPipeline().getWaiter();

        BiPredicate<Ctx, Res> prediction = taskMeta.getTaskNode().getPrediction();

        try {
            if (null != prediction && !prediction.test(userContext, userResult)) {
                log.info("{} taskName={} predicate false", LogConstant.LOG_HEAD, taskMeta.getTaskName());
                return false;
            }
        }
        catch (Exception e) {
            Exception exception = e;
            if (suppressStackTrace) {
                exception = SharpKnifeException.suppressStackTraces(exception);
            }
            log.error("{} taskType={}, taskName={} predicate error={}!",
                    LogConstant.LOG_HEAD, taskMeta.getTaskType().getTypeName(), taskMeta.getTaskName(), exception);
            if (abort) {
                executionResult.setError(exception);
                waiter.wakeUpWhileError();
            }
            return false;
        }
        return true;
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
                /*
                    MonitoredThreadPool.afterWork()
                        上报排队时间、执行时间
                    decreaseNextInDegree()
                        提交邻节点/keepRunning
                 */
                hooks.forEach(Hooker::invoke);
            }
            catch (Exception e) {
                // 这里捕获回调异常是防止异步线程消亡，免得需要再创建线程和内核上下文切换
                log.error("{} handleHooks error", LogConstant.LOG_HEAD, e);
            }
        }
    }

    /**
     * 回调用户代码，清理线程资源
     */
    private void threadSelfClear() {
        if (null != inheritBehaviour) {
            inheritBehaviour.threadSelfClear();
        }
    }

    public void markStartScheduleTime() {
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
