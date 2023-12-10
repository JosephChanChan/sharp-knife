package com.joseph.sharpknife.blade.scheduler;

import com.joseph.sharpknife.blade.context.ExecutionResult;
import com.joseph.sharpknife.blade.context.ScheduleRequest;

/**
 * @author Joseph
 */
public interface ConcurrentScheduler {

    <Ctx, Res> ExecutionResult scheduleSyn(ScheduleRequest<Ctx, Res> scheduleRequest);

}
