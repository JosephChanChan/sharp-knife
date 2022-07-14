package com.joseph.sharpknife.blade.context;

import com.joseph.sharpknife.blade.unit.InDegreeInfo;
import com.joseph.sharpknife.blade.unit.SchedulingUnit;
import com.joseph.sharpknife.blade.unit.TaskType;

import java.util.Map;


/**
 * 执行上下文
 */
public class ExecutionContext {

    /**
     * 链路图的入度信息
     * taskName -> inDegree
     */
    private Map<String, InDegreeInfo> graphInDegrees ;

    public void copyGraph(SchedulingUnit unit) {
        this.graphInDegrees = unit.copyGraphInDegreeInfo();
    }

    public int decreaseInDegree(String nextNode) {
        return graphInDegrees.get(nextNode).countDown();
    }

    public InDegreeInfo getInDegreeInfo(String node) {
        return graphInDegrees.get(node);
    }

}
