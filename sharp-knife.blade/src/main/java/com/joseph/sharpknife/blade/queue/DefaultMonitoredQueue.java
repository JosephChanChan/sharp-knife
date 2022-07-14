package com.joseph.sharpknife.blade.queue;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author Joseph
 */
public class DefaultMonitoredQueue<E> extends MonitoredTaskQueue<E> {


    public DefaultMonitoredQueue(int countLimit) {
        this.countLimit = countLimit;
        this.queue = new LinkedBlockingQueue<>(countLimit);
    }

    public DefaultMonitoredQueue(BlockingQueue<E> queue) {
        this.countLimit = queue.remainingCapacity() + queue.size();
        this.queue = queue;
    }

    @Override
    double calcSaturation() {
        // 注意这里可能影响框架的并发，需要上锁拿队列的任务个数
        int taskCount = this.queue.size();
        return ((double) taskCount) / countLimit;
    }
}
