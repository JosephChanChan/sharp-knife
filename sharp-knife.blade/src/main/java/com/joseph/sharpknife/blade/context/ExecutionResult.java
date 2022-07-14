package com.joseph.sharpknife.blade.context;

import com.joseph.sharpknife.blade.unit.TaskType;
import lombok.Data;


/**
 * @author Joseph
 */
@Data
public class ExecutionResult {

    /**
     * 执行过程中的错误
     * 设置error时禁止指令重排序
     */
    private volatile Throwable error;

    public boolean hasError() {
        return null != error;
    }

}
