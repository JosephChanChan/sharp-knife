package com.joseph.sharpknife.blade.context;

import java.util.HashMap;
import java.util.Map;

/**
 * 父子线程变量传递行为定义
 *
 * @author Joseph
 */
public abstract class InheritBehaviour {

    /**
     * 父线程准备变量，传递给异步子线程。
     *
     * @param map 变量容器
     */
    public abstract void prepareInherit(Map<String, Object> map);

    /**
     * 子线程从父线程中继承变量
     *
     * @param map 变量容器
     */
    public abstract void inheritFromParent(Map<String, Object> map);

    /**
     * 有时候子线程执行完后，需要清理从父线程继承来的变量。
     * 方法在子线程执行完任务节点后回调。
     */
    public abstract void threadSelfClear();
}
