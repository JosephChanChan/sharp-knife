package com.joseph.sharpknife.blade.pool;

import com.joseph.common.kit.AssertKit;

import java.util.HashMap;
import java.util.Map;

/**
 * 线程池管理器
 *
 * @author Joseph
 */
public class SharpKnifePoolManager {

    private static final SharpKnifePoolManager MANAGER = new SharpKnifePoolManager();

    private final Map<String, MonitoredThreadPool> pools = new HashMap<>();

    private SharpKnifePoolManager() {}

    public static SharpKnifePoolManager getManager() {
        return MANAGER;
    }

    public void putPool(String poolName, MonitoredThreadPool pool) {
        AssertKit.notBlank(poolName, "poolName can't be blank");
        AssertKit.notNull(pool, "pool can't be null");

        MonitoredThreadPool old = pools.putIfAbsent(poolName, pool);
        AssertKit.check(null != old, String.format("MonitoredThreadPool=%s should be unique", poolName));
    }

    public MonitoredThreadPool getPool(String poolName) {
        return pools.get(poolName);
    }





}
