package com.joseph.sharpknife.blade.ioc;

import com.joseph.common.kit.collections.CollectionsKit;
import com.joseph.sharpknife.blade.constnat.LogConstant;
import com.joseph.sharpknife.blade.unit.SchedulingUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import java.util.List;

/**
 * 监听SpringBoot框架初始化完成事件
 *
 * @author Joseph
 *  2022-02-01 17:38
 */
@Slf4j
public class SpringContextRefreshedListener implements ApplicationListener<ContextRefreshedEvent> {

    @Autowired
    private SpringTaskHolder taskHolder;


    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        /*
            SpringBoot初始化完成
            开始计算所有调度单元的入度信息
         */
        log.info("{} calculating indegree info!", LogConstant.LOG_HEAD);

        List<SchedulingUnit> units = taskHolder.getAllSchedulingUnits();
        if (CollectionsKit.isEmpty(units)) {
            log.info("{} taskHolder collect no scheduling units to handle!", LogConstant.LOG_HEAD);
            return;
        }

        for (SchedulingUnit unit : units) {
            log.info("{} taskType unit={} begin calculate indegree", LogConstant.LOG_HEAD, unit.getTaskType().getTypeName());
            unit.paramsCheck();
            unit.calcNodeInDegrees();
        }
        log.info("{} framework has been initialized! ready to fire", LogConstant.LOG_HEAD);
    }



}
