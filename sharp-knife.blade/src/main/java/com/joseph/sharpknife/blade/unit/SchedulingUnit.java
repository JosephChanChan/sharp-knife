package com.joseph.sharpknife.blade.unit;

import com.joseph.common.kit.ClassKit;
import com.joseph.common.kit.collections.CollectionsKit;
import com.joseph.sharpknife.blade.constnat.LogConstant;
import com.joseph.sharpknife.blade.pool.MonitoredThreadPool;
import com.joseph.sharpknife.blade.config.GlobalConfigHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.DependsOn;

import java.util.*;
import java.util.stream.Collectors;


/**
 * 调度单元是一类任务节点的集合
 *
 * @author Joseph
 */
@Slf4j
public class SchedulingUnit {

    /* -------------------------- 面向用户的定制化参数 Start ----------------------------- */

    private final TaskType taskType ;

    private MonitoredThreadPool executorPool ;

    /* -------------------------- 面向用户的定制化参数 End ----------------------------- */


    /**
     * 调度单元内节点数量编号
     */
    private int nodeCount = 0;

    /**
     * 调度单元的任务头结点
     */
    private List<TaskMeta> headNodes;

    /**
     * 邻接表
     * 调度单元包含的所有任务节点的邻居
     * taskName -> taskNodes
     */
    private Map<String, List<TaskMeta>> nodeNeighbours;

    /**
     * 节点入度信息
     */
    private Map<String, InDegreeInfo> nodeInDegrees;



    public SchedulingUnit(TaskType taskType) {
        this.taskType = taskType;
        headNodes = new ArrayList<>(8);
        nodeNeighbours = new HashMap<>(16);
    }

    public TaskType getTaskType() {
        return taskType;
    }

    public MonitoredThreadPool getExecutorPool() {
        return executorPool;
    }

    public int getNodeCount() {
        return nodeCount;
    }

    public List<TaskMeta> getHeadNodes() {
        return headNodes;
    }

    public List<TaskMeta> getNeighbours(String nodeName) {
        return nodeNeighbours.get(nodeName);
    }

    /**
     * 调度单元必要参数检查
     */
    public void paramsCheck() {
        if (null == taskType) {
            throw new IllegalArgumentException("taskType can't not be null!");
        }
        if (null == executorPool) {
            throw new IllegalArgumentException("executorPool can't not be null!");
        }
        if (CollectionsKit.isEmpty(headNodes)) {
            throw new IllegalArgumentException("headNodes can't be empty!");
        }
        taskType.paramsCheck();
    }

    /**
     * Spring容器初始化完成后获取线程池
     */
    public void readyExecutorPool() {
        this.executorPool = taskType.getExecutorPool();
    }

    /**
     * 提供全局配置给线程池
     */
    public void offerGlobalConfig2Pool(GlobalConfigHolder configHolder) {
        this.executorPool.setConfigHolder(configHolder);
    }

    /**
     * 任务节点初始化调度单元
     *
     * @param taskMeta taskMeta
     */
    public void buildAppend(TaskMeta taskMeta) {
        appendToHeads(taskMeta);
        appendToNeighbours(taskMeta);
    }

    private void appendToHeads(TaskMeta taskMeta) {
        DependsOn previousConfig = ClassKit.getAnnotation(taskMeta.getTaskNode(), DependsOn.class);
        if (null == previousConfig) {
            taskMeta.setHeadNode(true);
            headNodes.add(taskMeta);
        }
    }

    private void appendToNeighbours(TaskMeta taskMeta) {
        DependsOn previousConfig = ClassKit.getAnnotation(taskMeta.getTaskNode(), DependsOn.class);
        if (null != previousConfig) {
            for (String previous : previousConfig.value()) {
                nodeNeighbours.computeIfAbsent(previous, (previousName) -> new ArrayList<>()).add(taskMeta);
            }
        }
    }

    /**
     * 等所有任务节点处理完毕，计算调度单元内所有任务节点的入度
     */
    public void calcNodeInDegrees() {
        if (CollectionsKit.isEmpty(headNodes)) {
            return;
        }
        bfsCalcInDegrees();
        initNodeInDegrees();
    }

    public Map<String, InDegreeInfo> copyGraphInDegreeInfo() {
        if (CollectionsKit.isEmpty(nodeInDegrees)) {
            return null;
        }
        Map<String, InDegreeInfo> map = new HashMap<>(nodeInDegrees.size());
        for (Map.Entry<String, InDegreeInfo> entry : nodeInDegrees.entrySet()) {
            map.put(entry.getKey(), entry.getValue().deepCopy());
        }
        return map;
    }

    private void bfsCalcInDegrees() {
        int distance = 0;

        Queue<TaskMeta> queue = new LinkedList<>(headNodes);
        Set<String> vis = headNodes.stream().map(TaskMeta::getTaskName).collect(Collectors.toSet());

        while (!queue.isEmpty()) {
            int size = queue.size();
            int nodeNumber = 0;

            for (int i = 0; i < size && !queue.isEmpty(); i++) {
                this.nodeCount++;
                TaskMeta taskMeta = queue.poll();
                String taskName = taskMeta.getTaskName();
                InDegreeInfo inDegreeInfo = taskMeta.getInDegreeInfo();

                inDegreeInfo.calcNodeNumber(taskName, distance, nodeNumber++);

                if (log.isDebugEnabled()) {
                    log.debug("{} taskName={} calculating indegree!", LogConstant.LOG_HEAD, inDegreeInfo.getNodeNumber());
                }

                List<TaskMeta> neighbours = nodeNeighbours.get(taskName);

                if (CollectionsKit.isEmpty(neighbours)) {
                    continue;
                }

                neighbours.forEach(meta -> {
                    // 有一个节点可以访问到当前的taskNode，入度加1
                    meta.getInDegreeInfo().addCount();
                    if (!vis.contains(meta.getTaskName())) {
                        queue.add(meta);
                        vis.add(meta.getTaskName());
                    }
                });
            }
            distance++;
        }
    }

    private void initNodeInDegrees() {
        nodeInDegrees = new HashMap<>();
        for (Map.Entry<String, List<TaskMeta>> entry : nodeNeighbours.entrySet()) {
            for (TaskMeta taskMeta : entry.getValue()) {
                nodeInDegrees.putIfAbsent(taskMeta.getTaskName(), taskMeta.getInDegreeInfo());
            }
        }
    }


}
