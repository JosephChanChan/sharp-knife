package com.joseph.sharpknife.blade.unit;

import com.joseph.sharpknife.blade.pool.MonitoredThreadPool;
import org.springframework.util.StringUtils;

/**
 * 任务类型是一系列具有共同任务的特征身份。
 * 例如商品列表类型的任务节点都会有一个ProductListTaskType的身份特征。
 * 帮助用户向框架直接提交一个任务特征，框架自动寻找这类任务节点并发调度。
 *
 * @author Joseph
 */
public abstract class TaskType {

    /**
     * 获取任务类型名称
     */
    public abstract String getTypeName();

    /**
     * 同类身份特征的任务要用同一种线程池执行
     *
     * @return thread pool
     */
    public abstract MonitoredThreadPool getExecutorPool();

    /**
     * 检查任务类型名称
     */
    public void paramsCheck() {
        if (StringUtils.isEmpty(getTypeName())) {
            throw new IllegalArgumentException("TaskType name can't be blank!");
        }
    }

    @Override
    public int hashCode() {
        String name = getTypeName();
        return name.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof TaskType)) {
            return false;
        }
        return ((TaskType) other).getTypeName().equals(this.getTypeName());
    }
}
