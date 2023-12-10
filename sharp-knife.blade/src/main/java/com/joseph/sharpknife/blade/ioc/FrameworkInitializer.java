package com.joseph.sharpknife.blade.ioc;

import com.joseph.sharpknife.blade.kits.CollectionsKit;
import com.joseph.sharpknife.blade.config.GlobalConfigHolder;
import com.joseph.sharpknife.blade.constnat.LogConstant;
import com.joseph.sharpknife.blade.unit.SchedulingUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 监听SpringBoot框架初始化完成事件
 *
 * @author Joseph
 *  2022-02-01 17:38
 */
@Slf4j
@Component
public class FrameworkInitializer implements ApplicationRunner {

    /**
     * 在SpringApplication中会创建两个FrameworkInitializer对象，先后执行一次run方法
     * 需要保证run只执行一次
     */
    private static volatile boolean INITIALIZED = false;


    @Autowired
    private SpringTaskHolder taskHolder;

    @Autowired
    private GlobalConfigHolder globalConfigHolder;


    @Override
    public void run(ApplicationArguments args) {
        if (INITIALIZED) {
            return;
        }

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
            unit.readyExecutorPool();
            unit.paramsCheck();
            unit.offerGlobalConfig2Pool(globalConfigHolder);
            unit.calcNodeInDegrees();
        }
        INITIALIZED = true;
        log.info("{} framework has been initialized! ready to fire", LogConstant.LOG_HEAD);
    }
}
