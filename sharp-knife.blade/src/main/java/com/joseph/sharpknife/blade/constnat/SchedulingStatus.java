package com.joseph.sharpknife.blade.constnat;

import com.joseph.sharpknife.blade.context.ScheduleRequest;

/**
 * @author Joseph
 *  2022-02-01 22:41
 */
public class SchedulingStatus {

    /**
     * 调度单元刚初始化完毕，还开始执行
     */
    public static final String INIT = "INITIALIZED";

    /**
     * 调度单元中的任务节点开始执行
     */
    public static final String EXECUTING = "EXECUTING";

    /**
     * 调度单元的所有任务节点执行完毕，
     * 如果设置了{@link ScheduleRequest.interruptWhileError = false}，
     * 可能整个执行链路中有节点出错但被忽略了。
     * done和partlyDone是互斥关系
     */
    public static final String DONE = "DONE";

    /**
     * 执行完部分任务节点，整个链路图的执行时间超过了设置的总体执行超时时间，主线程主动醒来，剩余任务节点被抛弃
     */
    public static final String PARTLY_DONE = "PARTLY_DONE";

    /**
     * 执行完部分节点，遭遇错误而终止剩余流程。
     * 开启了{@link ScheduleRequest.interruptWhileError = true}框架感知到节点执行出错时，
     * 中断剩余的任务节点执行（正在执行的节点不受影响），唤醒主线程，剩余任务节点被抛弃
     */
    public static final String PARTLY_DONE_ERROR = "PARTLY_DONE_ERROR";


}
