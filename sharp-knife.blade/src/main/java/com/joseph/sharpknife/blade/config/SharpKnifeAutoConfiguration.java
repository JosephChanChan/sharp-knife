package com.joseph.sharpknife.blade.config;

import com.joseph.sharpknife.blade.ioc.SchedulingBuilder;
import com.joseph.sharpknife.blade.ioc.SpringContextRefreshedListener;
import com.joseph.sharpknife.blade.ioc.SpringCoordinator;
import com.joseph.sharpknife.blade.ioc.SpringTaskHolder;
import com.joseph.sharpknife.blade.scheduler.ConcurrentScheduler;
import com.joseph.sharpknife.blade.scheduler.DefaultConcurrentScheduler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Joseph
 */
@Configuration
public class SharpKnifeAutoConfiguration {


    @Bean
    public SchedulingBuilder schedulingBuilder(SpringCoordinator springCoordinator,
                                               SpringTaskHolder springTaskHolder,
                                               GlobalConfigHolder globalConfigHolder) {
        return new SchedulingBuilder(springTaskHolder, springCoordinator, globalConfigHolder);
    }

    @Bean
    public SpringCoordinator springCoordinator() {
        return new SpringCoordinator();
    }

    @Bean
    public SpringTaskHolder springTaskHolder() {
        return new SpringTaskHolder();
    }

    @Bean
    public SpringContextRefreshedListener springContextRefreshedListener() {
        return new SpringContextRefreshedListener();
    }

    @Bean
    public GlobalConfigHolder globalConfigHolder() {
        return new GlobalConfigHolder();
    }

    @Bean
    public ConcurrentScheduler defaultConcurrentScheduler() {
        return new DefaultConcurrentScheduler();
    }

}
