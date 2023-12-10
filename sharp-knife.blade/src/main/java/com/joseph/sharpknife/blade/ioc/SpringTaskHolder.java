package com.joseph.sharpknife.blade.ioc;

import com.joseph.sharpknife.blade.unit.TaskType;
import com.joseph.sharpknife.blade.unit.SchedulingUnit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * 基于Spring的任务节点容器
 *
 * @author Joseph
 */
public class SpringTaskHolder {

    /**
     * 根据任务特征分类的调度单元
     * 在用户向框架提交任务特征时获取调度单元执行
     */
    private final Map<TaskType, SchedulingUnit> schedulingUnitMap;

    public SpringTaskHolder() {
        schedulingUnitMap = new HashMap<>();
    }

    public SchedulingUnit getUnit(TaskType taskType) {
        return schedulingUnitMap.get(taskType);
    }

    /**
     * 往容器中新增一堆映射。如果key映射的value是空的话就成功映射unit。
     * 否则返回旧的unit，什么都不做。
     *
     * @param taskType taskType
     * @param unit unit
     * @return unit
     */
    public SchedulingUnit computeIfAbsent(TaskType taskType, SchedulingUnit unit) {
        return schedulingUnitMap.computeIfAbsent(taskType, (taskTypeKey) -> unit);
    }

    /**
     * 获取所有调度单元
     *
     * @return list of units
     */
    public List<SchedulingUnit> getAllSchedulingUnits() {
        return new ArrayList<>(schedulingUnitMap.values());
    }
}
