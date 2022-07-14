package com.joseph.sharpknife.blade.scheduler;

import com.joseph.sharpknife.blade.context.*;
import com.joseph.sharpknife.blade.unit.*;
import lombok.extern.slf4j.Slf4j;


/**
 * 默认实现的任务并发调度器
 *
 * @author Joseph
 */
@Slf4j
public class DefaultConcurrentScheduler extends AbstractConcurrentScheduler {


    @Override
    protected <Ctx, Res> void doSubmit(boolean async, TaskMeta taskMeta, SchedulingUnit unit, ScheduleRequest<Ctx, Res> scheduleRequest) {
        EventWaiter waiter = scheduleRequest.getRequestPipeline().getWaiter();
        TaskType taskType = taskMeta.getTaskType();
        String taskName = taskMeta.getTaskName();

        if (!canLaunchNode(taskMeta, scheduleRequest)) {
            return;
        }

        if (!handlePrediction(taskMeta, scheduleRequest)) {
            return;
        }

        RunWrapper<Ctx, Res> runWrapper = new RunWrapper<>(taskMeta, scheduleRequest);
        runWrapper.markScheduleStartTime();
        // 责任链模式回调
        runWrapper.addHook(new Hooker(1, unused -> decreaseNextInDegree(taskMeta, unit, scheduleRequest)));
        runWrapper.addHook(new Hooker(2, unused -> submitNeighbours(taskMeta, unit, scheduleRequest)));

        // 提交前看一次状态
        if (!stillScheduling(waiter, taskType.getTypeName(), taskName)) {
            return;
        }
        unit.getExecutorPool().handle(async, runWrapper);
    }



}
