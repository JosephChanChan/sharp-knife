package com.joseph.sharpknife.blade.scheduler;

import com.joseph.common.kit.NumberKit;
import com.joseph.common.kit.collections.CollectionsKit;
import com.joseph.sharpknife.blade.config.GlobalConfigHolder;
import com.joseph.sharpknife.blade.context.*;
import com.joseph.sharpknife.blade.constnat.LogConstant;
import com.joseph.sharpknife.blade.constnat.SchedulingStatus;
import com.joseph.sharpknife.blade.exception.SharpKnifeException;
import com.joseph.sharpknife.blade.ioc.SpringTaskHolder;
import com.joseph.sharpknife.blade.kits.Clock;
import com.joseph.sharpknife.blade.unit.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;

/**
 * @author Joseph
 */
@Slf4j
public abstract class AbstractConcurrentScheduler implements ConcurrentScheduler {

    @Autowired
    protected SpringTaskHolder taskHolder;
    @Autowired
    protected GlobalConfigHolder globalConfigHolder;


    /**
     * 提交调度请求，并发调度任务节点
     * 完成调度后用户应检查ExecutionResult对象的结果
     */
    public <Ctx, Res> ExecutionResult scheduleSyn(ScheduleRequest<Ctx, Res> scheduleRequest) {
        scheduleRequest.paramsCheck();

        SchedulingUnit unit = getSchedulingUnit(scheduleRequest.getTaskType());
        TaskType taskType = unit.getTaskType();

        scheduleRequest.buildPipeLine(unit);
        EventWaiter waiter = scheduleRequest.getRequestPipeline().getWaiter();
        ExecutionContext executionContext = scheduleRequest.getRequestPipeline().getExecutionContext();
        ExecutionResult executionResult = scheduleRequest.getRequestPipeline().getExecutionResult();

        long start = Clock.millisNow();
        executionContext.copyGraph(unit);

        waiter.readyExecute();

        submitHeads(unit, scheduleRequest);

        waiting(scheduleRequest);

        handleFinished(scheduleRequest);
        long end = Clock.millisNow();
        log.info("{} taskType={} cost millis={} and executionStatus={}",
                LogConstant.LOG_HEAD, taskType.getTypeName(), Clock.millisDiff(start, end), waiter.getStatus());
        return executionResult;
    }

    public <Ctx, Res> void submitTask(boolean async, TaskMeta taskMeta, SchedulingUnit unit, ScheduleRequest<Ctx, Res> scheduleRequest) {
        try {
            doSubmit(async, taskMeta, unit, scheduleRequest);
        }
        catch (Exception e) {
            // 框架内的异常都在此处收敛捕获
            log.error("{} ConcurrentScheduler doSubmit error", LogConstant.LOG_HEAD, e);
        }
    }

    public <Ctx, Res> void submitNeighbours(TaskMeta taskMeta, SchedulingUnit unit, ScheduleRequest<Ctx, Res> scheduleRequest) {
        TaskType taskType = taskMeta.getTaskType();

        List<TaskMeta> neighbours = unit.getNeighbours(taskMeta.getTaskName());
        if (CollectionsKit.isEmpty(neighbours)) {
            log.info("{} taskType={}, taskName={} have no neighbours",
                    LogConstant.LOG_HEAD, taskType.getTypeName(), taskMeta.getTaskName());
            return;
        }
        boolean async = neighbours.size() > 1;
        for (TaskMeta neighbour : neighbours) {
            submitTask(async, neighbour, unit, scheduleRequest);
        }
    }

    public <Ctx, Res> void decreaseNextInDegree(TaskMeta taskMeta, SchedulingUnit unit, ScheduleRequest<Ctx, Res> scheduleRequest) {
        ExecutionContext executionContext = scheduleRequest.getRequestPipeline().getExecutionContext();
        List<TaskMeta> neighbours = unit.getNeighbours(taskMeta.getTaskName());
        if (CollectionsKit.isNotEmpty(neighbours)) {
            int inDegree ;
            for (TaskMeta neighbourNode : neighbours) {
                inDegree = executionContext.decreaseInDegree(neighbourNode.getTaskName());
                if (inDegree < 0) {
                    // 不应该发生的情况
                    throw new SharpKnifeException(String.format("%s, currentNode=%s reduce a neighbour=%s error! inDegree=%s",
                            LogConstant.LOG_HEAD, taskMeta.getTaskName(), neighbourNode.getTaskName(), inDegree));
                }
                log.info("{} currentNode={} reduce a neighbour={} inDegree={}",
                        LogConstant.LOG_HEAD, taskMeta.getTaskName(), neighbourNode.getTaskName(), inDegree);
            }
        }
    }

    /**
     * 具体调度器提交任务的实现
     */
    protected abstract <Ctx, Res> void doSubmit(
            boolean async, TaskMeta taskMeta, SchedulingUnit unit, ScheduleRequest<Ctx, Res> scheduleRequest);

    /**
     * 执行单元内的所有任务头结点
     */
    private <Ctx, Res> void submitHeads(SchedulingUnit unit, ScheduleRequest<Ctx, Res> scheduleRequest) {
        List<TaskMeta> headNodes = unit.getHeadNodes();
        for (TaskMeta headNode : headNodes) {
            submitTask(true, headNode, unit, scheduleRequest);
        }
    }

    private <Ctx, Res> void waiting(ScheduleRequest<Ctx, Res> scheduleRequest) {
        ExecutionResult executionResult = scheduleRequest.getRequestPipeline().getExecutionResult();
        EventWaiter waiter = scheduleRequest.getRequestPipeline().getWaiter();

        if (executionResult.hasError()) {
            return;
        }

        long waitMillis = NumberKit.gt0(scheduleRequest.getTimeoutTotal()) ?
                scheduleRequest.getTimeoutTotal() :
                globalConfigHolder.isEnableTimeoutTotal() ? globalConfigHolder.getTimeoutTotal() : 0;

        if (waitMillis > 0) {
            waiter.waiting(waitMillis);
        }
        else {
            waiter.waiting();
        }
    }

    private <Ctx, Res> void handleFinished(ScheduleRequest<Ctx, Res> scheduleRequest) {
        TaskType taskType = scheduleRequest.getTaskType();
        ExecutionResult executionResult = scheduleRequest.getRequestPipeline().getExecutionResult();
        EventWaiter eventWaiter = scheduleRequest.getRequestPipeline().getWaiter();

        if (!Objects.equals(SchedulingStatus.EXECUTING, eventWaiter.getStatus())) {
            log.info("{} schedulingStatus was {} not expected executing",
                    LogConstant.LOG_HEAD, eventWaiter.getStatus());
            return;
        }

        String targetStatus = SchedulingStatus.DONE;
        if (!eventWaiter.wasFinished() || eventWaiter.wasInterrupted()) {
            targetStatus = SchedulingStatus.PARTLY_DONE;
        }
        else if (scheduleRequest.isInterruptWhileError() && executionResult.hasError()) {
            targetStatus = SchedulingStatus.PARTLY_DONE_ERROR;
        }
        if (!eventWaiter.advance2(SchedulingStatus.EXECUTING, targetStatus)) {
            log.error("{} schedulingStatus convert fail! mainThread={}, taskType={}, statusNow={}, targetStatus={}",
                    LogConstant.LOG_HEAD, Thread.currentThread().getName(), taskType.getTypeName(), eventWaiter.getStatus(), targetStatus);
        }
    }

    private SchedulingUnit getSchedulingUnit(TaskType taskType) {
        SchedulingUnit unit = taskHolder.getUnit(taskType);

        if (null == unit) {
            throw new SharpKnifeException(String.format("taskType=%s get no correspond SchedulingUnit",
                    taskType.getTypeName()));
        }
        unit.paramsCheck();
        return unit;
    }

    protected boolean stillScheduling(EventWaiter waiter, String taskType, String taskName) {
        boolean go ;
        if (!(go = waiter.statusIsExecuting())) {
            switch (waiter.getStatus()) {
                case SchedulingStatus.PARTLY_DONE:
                    log.error("{} taskType={}, taskName={} was aborted cause global time out",
                            LogConstant.LOG_HEAD, taskType, taskName);
                    break;

                case SchedulingStatus.PARTLY_DONE_ERROR:
                    log.error("{} taskType={}, taskName={} was aborted cause encounter error",
                            LogConstant.LOG_HEAD, taskType, taskName);
                    break;

                default:
                    log.error("{} taskType={}, taskName={} was aborted cause waiter status={}",
                            LogConstant.LOG_HEAD, taskType, taskName, waiter.getStatus());
            }
        }
        return go;
    }

    protected <Ctx, Res> boolean handlePrediction(TaskMeta taskMeta, ScheduleRequest<Ctx, Res> scheduleRequest) {
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

    protected <Ctx, Res> boolean canLaunchNode(TaskMeta taskMeta, ScheduleRequest<Ctx, Res> scheduleRequest) {
        if (!taskMeta.isHeadNode()) {
            String taskName = taskMeta.getTaskName();
            ExecutionContext executionContext = scheduleRequest.getRequestPipeline().getExecutionContext();
            InDegreeInfo inDegreeInfo = executionContext.getInDegreeInfo(taskName);
            if (!inDegreeInfo.canLaunch()) {
                log.info("{} taskName={} remain inDegree={} can't launch",
                        LogConstant.LOG_HEAD, taskName, inDegreeInfo.getRemainInDegrees());
                return false;
            }
        }
        return true;
    }

}
