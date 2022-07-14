package com.joseph.sharpknife.blade.unit;

import com.joseph.sharpknife.blade.context.ExecutionContext;

import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

/**
 * 任务节点的抽象。所有需要交由框架并发调度的任务需要实现该接口
 * 任务节点使用@DependensOn注解编排，该注解帮助解决节点编排的有环图问题
 * 任务节点构成的链路图，需要是单向无环图
 *
 * @author Joseph
 */
public interface TaskNode<Ctx, Res> {

    /**
     * 断言判断是否执行任务
     *
     * @return predication
     */
    BiPredicate<Ctx, Res> getPrediction();

    /**
     * 任务节点需要执行的任务
     *
     * @return task
     */
    BiConsumer<Ctx, Res> getTask();

    /**
     * 任务节点执行成功的回调通知
     */
    void onSuccess(Ctx userContext, Res userResult);

    /**
     * 任务节点执行错误的回调通知
     */
    void onError(Ctx userContext, Res userResult, Exception e);

}
