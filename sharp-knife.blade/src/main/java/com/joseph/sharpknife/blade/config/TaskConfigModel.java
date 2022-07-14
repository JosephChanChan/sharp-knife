package com.joseph.sharpknife.blade.config;

import com.joseph.sharpknife.blade.unit.TaskType;
import lombok.Data;

/**
 * @author Joseph
 */
@Data
public class TaskConfigModel {

    private String taskName;

    private String taskType;

    private TaskType taskTypeModel;

    private int timeout;


}
