package com.joseph.sharpknife.blade.config;

import com.joseph.sharpknife.blade.ioc.*;
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
    public ThreadPoolCollector threadPoolCollector() {
        return new ThreadPoolCollector();
    }

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
    public FrameworkInitializer springContextRefreshedListener() {
        return new FrameworkInitializer();
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
