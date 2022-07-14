package com.joseph.sharpknife.blade.annotations;

import com.joseph.sharpknife.blade.config.GlobalConfigHolder;
import com.joseph.sharpknife.blade.unit.TaskType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 任务配置信息
 *
 * @author Joseph
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface TaskConfig {

    /**
     * 任务节点的任务名称
     */
    String taskName();

    /**
     * 任务类型bean名称，需要是Spring托管单例bean
     */
    String taskType();

    /**
     * 任务节点执行超时时间，单位毫秒
     * 从任务节点提交给线程池开始计算到任务节点完成并执行onSuccess回调。
     * 优先级：this.timeout > {@link GlobalConfigHolder.timeoutPerTask}
     */
    int timeout() default 200;

}
