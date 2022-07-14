package com.joseph.sharpknife.blade.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author Joseph
 *  2022-01-02 15:22
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "sharp.knife.global")
public class GlobalConfigHolder {

    /**
     * 每个任务节点的默认执行时间，毫秒
     */
    private int timeoutPerTask = 200;

    /**
     * 一个调度单元总的执行超时，毫秒
     */
    private int timeoutTotal = 60000;

    /**
     * 开启全局等待超时
     */
    private boolean enableTimeoutTotal = true;

}
