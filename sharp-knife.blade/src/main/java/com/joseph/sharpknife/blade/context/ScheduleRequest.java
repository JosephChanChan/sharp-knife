package com.joseph.sharpknife.blade.context;

import com.joseph.sharpknife.blade.exception.SharpKnifeException;
import com.joseph.sharpknife.blade.config.GlobalConfigHolder;
import com.joseph.sharpknife.blade.unit.SchedulingUnit;
import com.joseph.sharpknife.blade.unit.TaskType;
import lombok.Getter;

/**
 * @author Joseph
 *  2022-02-01
 */
@Getter
public class ScheduleRequest<Ctx, Res> {

    /**
     * 任务类型，必须有
     */
    private final TaskType taskType;

    /**
     * 执行上下文，可为空
     */
    private final Ctx userContext;

    /**
     * 执行结果对象，可为空
     */
    private final Res userResult;

    /**
     * 调度单元总的执行超时时间，毫秒。默认取{@link GlobalConfigHolder.timeoutTotal}。
     * 优先级：this.timeoutTotal > GlobalConfigHolder.timeoutTotal
     */
    private Integer timeoutTotal;

    /**
     * 当有一个节点执行抛出错误，中断所有任务，默认true
     */
    private boolean interruptWhileError = true;

    /**
     * 屏蔽详细的堆栈错误信息
     * 如果屏蔽了，在任务节点执行异常回调通知用户时onError方法传递的只有简易错误信息
     */
    private boolean suppressStackTrace = false;


    private RequestPipeline requestPipeline ;

    public ScheduleRequest(TaskType taskType, Ctx executionContext, Res executionResult) {
        this.taskType = taskType;
        this.userContext = executionContext;
        this.userResult = executionResult;
    }

    public void setTimeoutTotal(int timeoutTotal) {
        this.timeoutTotal = timeoutTotal;
    }

    public void setInterruptWhileError(boolean interruptWhileError) {
        this.interruptWhileError = interruptWhileError;
    }

    public void paramsCheck() {
        if (null == taskType) {
            throw new SharpKnifeException("taskType can't be null");
        }
        if (null == userContext) {
            throw new SharpKnifeException("userContext can't be null");
        }
        if (null == userResult) {
            throw new SharpKnifeException("userResult can't be null");
        }
    }

    public void buildPipeLine(SchedulingUnit schedulingUnit) {
        this.requestPipeline = new RequestPipeline(schedulingUnit, new ExecutionContext(), new ExecutionResult());
    }

}
