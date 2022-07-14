package com.joseph.sharpknife.blade.unit;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 任务节点的入度信息，代表需要解锁的k个前继节点，才能执行当前节点
 *
 * @author Joseph
 */
public class InDegreeInfo {

    /**
     * 节点编号，由任务名称+图距离+图距离内的编号。
     * SearchProductIds-0-0
     */
    private String nodeNumber;

    /**
     * 节点的入度信息
     */
    private AtomicInteger inDegrees;

    public InDegreeInfo() {
        inDegrees = new AtomicInteger();
    }

    /**
     * 原子性地将入度自减一
     *
     * @return 返回自减一后的入度
     */
    public int countDown() {
        return inDegrees.decrementAndGet();
    }

    /**
     * 原子性地将入度自增一
     *
     * @return 返回自增后的入度
     */
    public int addCount() {
        return inDegrees.incrementAndGet();
    }

    /**
     * 判断TaskMeta是否可以执行
     * 当节点的入度为0时就可以触发了
     *
     * @return 0/1
     */
    public boolean canLaunch() {
        return inDegrees.get() == 0;
    }

    public void calcNodeNumber(String taskName, int distanceFromHead, int number) {
        nodeNumber = taskName.concat("-").concat(String.valueOf(distanceFromHead)).concat("-").concat(String.valueOf(number));
    }

    public String getNodeNumber() {
        return nodeNumber;
    }

    public int getRemainInDegrees() {
        return inDegrees.get();
    }

    public InDegreeInfo deepCopy() {
        InDegreeInfo copy = new InDegreeInfo();
        copy.nodeNumber = this.nodeNumber;
        copy.inDegrees = new AtomicInteger(this.inDegrees.get());
        return copy;
    }

    @Override
    public int hashCode() {
        return nodeNumber.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof InDegreeInfo)) {
            return false;
        }
        return ((InDegreeInfo) o).getNodeNumber().equals(this.nodeNumber);
    }
}
