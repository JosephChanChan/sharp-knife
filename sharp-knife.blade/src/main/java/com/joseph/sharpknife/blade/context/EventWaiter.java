package com.joseph.sharpknife.blade.context;

import com.joseph.sharpknife.blade.unit.TaskType;
import com.joseph.sharpknife.blade.constnat.LogConstant;
import com.joseph.sharpknife.blade.constnat.SchedulingStatus;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 事件等待
 */
@Slf4j
public class EventWaiter {

    /**
     * 调度单元的整体执行状态，see {@link SchedulingStatus}
     */
    private AtomicReference<String> status;

    /**
     * 所有节点是否执行完毕
     */
    private boolean finished = true;

    /**
     * 主线程是否在超时等待中被中断过
     */
    private boolean interrupted;

    private TaskType taskType;

    /**
     * 链路图内节点计数器
     */
    private AtomicInteger nodesCount;

    /**
     * 主线程等待
     */
    private CountDownLatch mainThreadLatch;

    public EventWaiter(int count, TaskType taskType) {
        this.status = new AtomicReference<>(SchedulingStatus.INIT);
        this.taskType = taskType;
        this.nodesCount = new AtomicInteger(count);
        this.mainThreadLatch = new CountDownLatch(1);
    }

    public void waiting() {
        doWaiting(0);
    }

    public void waiting(long millis) {
        doWaiting(millis);
    }

    private void doWaiting(long millis) {
        try {
            if (millis <= 0) {
                mainThreadLatch.await();
            }
            else {
                finished = mainThreadLatch.await(millis, TimeUnit.MILLISECONDS);
            }
        }
        catch (InterruptedException e) {
            finished = false;
            interrupted = true;
            Thread.currentThread().interrupt();
            log.error("{} Thread={} was wake up when waiting taskType={} finished",
                    LogConstant.LOG_HEAD, Thread.currentThread().getName(), taskType.getTypeName());
        }
    }

    /**
     * 主线程是否在等待任务执行完毕时被中断过
     *
     * @return 0/1
     */
    public boolean wasInterrupted() {
        return interrupted;
    }

    public boolean wasFinished() {
        return finished;
    }

    public void finishOneTask() {
        if (nodesCount.decrementAndGet() == 0) {
            log.info("{} taskType={} has been done!", LogConstant.LOG_HEAD, taskType.getTypeName());
            mainThreadLatch.countDown();
        }
    }

    public boolean advance2(String origStatus, String targetStatus) {
        return status.compareAndSet(origStatus, targetStatus);
    }

    /**
     * 状态推进到执行中，准备提交任务头结点
     */
    public void readyExecute() {
        advance2(SchedulingStatus.INIT, SchedulingStatus.EXECUTING);
    }

    /**
     * 工作线程遇到了错误，主动推进状态到PARTLY_DONE_ERROR，并唤醒主线程
     */
    public void wakeUpWhileError() {
        if (advance2(SchedulingStatus.EXECUTING, SchedulingStatus.PARTLY_DONE_ERROR)) {
            mainThreadLatch.countDown();
        }
        else {
            // 不应该出现的情况
            log.error("{} working thread={} push status to PARTLY_DONE_ERROR fail! statusNow={}",
                    LogConstant.LOG_HEAD, Thread.currentThread().getName(), status.get());
        }
    }

    public boolean statusIsExecuting() {
        return Objects.equals(SchedulingStatus.EXECUTING, status.get());
    }

    public boolean isGlobalTimeout() {
        return Objects.equals(SchedulingStatus.PARTLY_DONE, status.get());
    }

    public boolean isEncounterError() {
        return Objects.equals(SchedulingStatus.PARTLY_DONE_ERROR, status.get());
    }

    public String getStatus() {
        return status.get();
    }

}
