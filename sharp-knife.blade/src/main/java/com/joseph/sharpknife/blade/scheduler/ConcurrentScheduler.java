package com.joseph.sharpknife.blade.scheduler;

import com.joseph.sharpknife.blade.context.ExecutionContext;
import com.joseph.sharpknife.blade.context.ExecutionResult;
import com.joseph.sharpknife.blade.context.ScheduleRequest;
import com.joseph.sharpknife.blade.unit.SchedulingUnit;
import com.joseph.sharpknife.blade.unit.TaskMeta;

/**
 * @author Joseph
 */
public interface ConcurrentScheduler {

    <Ctx, Res> ExecutionResult scheduleSyn(ScheduleRequest<Ctx, Res> scheduleRequest);

}
