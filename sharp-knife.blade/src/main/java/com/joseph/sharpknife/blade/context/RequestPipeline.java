package com.joseph.sharpknife.blade.context;

import com.joseph.sharpknife.blade.unit.SchedulingUnit;
import lombok.Getter;

/**
 * @author Joseph
 */
@Getter
public class RequestPipeline {

    /**
     * 等待器
     */
    private EventWaiter waiter;

    private ExecutionContext executionContext;

    private ExecutionResult executionResult;

    public RequestPipeline (SchedulingUnit schedulingUnit, ExecutionContext executionContext, ExecutionResult executionResult) {
        this.waiter = new EventWaiter(schedulingUnit.getNodeCount(), schedulingUnit.getTaskType());
        this.executionContext = executionContext;
        this.executionResult = executionResult;
    }


}
