package com.joseph.sharpknife.blade.unit;

import com.joseph.sharpknife.blade.config.TaskConfigModel;
import lombok.Data;

/**
 * 任务元信息，对任务节点实例进行包装
 *
 * @author Joseph
 */
@Data
public class TaskMeta {

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 任务节点的执行超时时间，取自：
     * 1.用户自定义 2.全局配置
     */
    private int timeout;

    /**
     * 任务节点
     */
    private TaskNode taskNode;

    /**
     * 任务类型
     */
    private TaskType taskType;

    /**
     * 任务节点的入度信息
     */
    private InDegreeInfo inDegreeInfo;

    /**
     * 头结点标识
     */
    private boolean isHeadNode;

    public TaskMeta(TaskNode taskNode, TaskConfigModel configModel) {
        this.taskNode = taskNode;
        this.taskName = configModel.getTaskName();
        this.timeout = configModel.getTimeout();
        this.taskType = configModel.getTaskTypeModel();
        this.inDegreeInfo = new InDegreeInfo();
    }


}
