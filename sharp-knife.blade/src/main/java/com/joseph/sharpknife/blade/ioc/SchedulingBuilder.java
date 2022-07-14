package com.joseph.sharpknife.blade.ioc;

import com.joseph.common.kit.AssertKit;
import com.joseph.common.kit.ClassKit;
import com.joseph.sharpknife.blade.config.GlobalConfigHolder;
import com.joseph.sharpknife.blade.annotations.TaskConfig;
import com.joseph.sharpknife.blade.config.TaskConfigModel;
import com.joseph.sharpknife.blade.constnat.LogConstant;
import com.joseph.sharpknife.blade.exception.SharpKnifeException;
import com.joseph.sharpknife.blade.unit.SchedulingUnit;
import com.joseph.sharpknife.blade.unit.TaskMeta;
import com.joseph.sharpknife.blade.unit.TaskNode;
import com.joseph.sharpknife.blade.unit.TaskType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.DependsOn;
import org.springframework.util.StringUtils;

/**
 * 调度框架收集任务节点
 *
 * @author Joseph
 */
@Slf4j
public class SchedulingBuilder implements BeanPostProcessor {

    private final SpringTaskHolder taskHolder ;

    private final GlobalConfigHolder globalConfig ;

    private final SpringCoordinator springCoordinator ;

    public SchedulingBuilder(SpringTaskHolder taskHolder, SpringCoordinator springCoordinator, GlobalConfigHolder globalConfig) {
        this.taskHolder = taskHolder;
        this.globalConfig = globalConfig;
        this.springCoordinator = springCoordinator;
    }

    /**
     * 在spring bean实例化后，afterProperties函数后，init-method函数后调用。
     * 包装TaskNode节点返回。
     *
     * @param bean bean
     * @param beanName beanName
     * @return TaskNode
     * @throws BeansException BeansException
     */
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (!(bean instanceof TaskNode)) {
            return bean;
        }
        TaskConfig taskConfig = ClassKit.getAnnotation(bean, TaskConfig.class);
        AssertKit.notNull(taskConfig, String.format("%s, beanName=%s is a TaskNode but have no TaskConfig!",
                LogConstant.LOG_HEAD, beanName));

        log.info("{} beanName={} is a taskNode", LogConstant.LOG_HEAD, beanName);

        // depends on check
        dependsOnCheck((TaskNode) bean);

        TaskConfigModel configModel = parseConfig(taskConfig);
        TaskNode taskNode = (TaskNode) bean;
        TaskMeta taskMeta = new TaskMeta(taskNode, configModel);

        TaskType taskType = configModel.getTaskTypeModel();
        SchedulingUnit unit = taskHolder.getUnit(taskType);
        if (null == unit) {
            unit = new SchedulingUnit(taskType);
        }
        unit.buildAppend(taskMeta);
        taskHolder.computeIfAbsent(taskType, unit);
        springCoordinator.maintainTask(taskMeta);
        return bean;
    }

    private TaskConfigModel parseConfig(TaskConfig taskConfig) {
        TaskConfigModel configModel = new TaskConfigModel();
        configModel.setTaskName(taskConfig.taskName());
        configModel.setTaskType(taskConfig.taskType());
        configModel.setTimeout(taskConfig.timeout());

        AssertKit.notBlank(configModel.getTaskName(), "taskName of a TaskConfig can't be blank!");
        AssertKit.notBlank(configModel.getTaskType(), "taskType of a TaskConfig can't be blank!");
        AssertKit.check(configModel.getTimeout() <= 0, "timeout millis of a TaskConfig must be a positive int!");
        AssertKit.check(springCoordinator.alreadyContain(configModel.getTaskName()),
                String.format("taskName=%s of taskType=%s conflict with other taskMeta", configModel.getTaskName(), configModel.getTaskType()));

        try {
            TaskType taskType = springCoordinator.getBean(configModel.getTaskType(), TaskType.class);
            configModel.setTaskTypeModel(taskType);
        }
        catch (BeansException e) {
            throw new SharpKnifeException(
                    String.format("taskType=%s must is a Spring singleton bean!", configModel.getTaskType()), e);
        }
        return configModel;
    }

    private void dependNodeCheck(String previousNodeName) {
        try {
            springCoordinator.getBean(previousNodeName, TaskNode.class);
        }
        catch (BeansException e) {
            // 没有找到bean或无法实例化，在启动时就该暴露出错误给用户
            log.error("SchedulingBuilder.dependNodeCheck error! TaskNode {} not found or can't instance!", previousNodeName);
            throw e;
        }
    }

    private void dependsOnCheck(TaskNode taskNode) {
        DependsOn dependsOnConfig = ClassKit.getAnnotation(taskNode, DependsOn.class);
        if (null != dependsOnConfig) {
            for (String nodeName : dependsOnConfig.value()) {
                dependNodeCheck(nodeName);
            }
        }
    }


}
