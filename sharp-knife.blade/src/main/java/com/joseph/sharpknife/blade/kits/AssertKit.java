package com.joseph.sharpknife.blade.kits;

import java.util.Collection;
import java.util.Map;

/**
 * 断言工具类
 *
 * @author Joseph
 * @since 2022/3/13
 */
public class AssertKit {

    /**
     * check条件为真时抛出异常
     *
     * @param b 表达式传递的布尔值
     * @param errorMsg 错误提示信息
     */
    public static void check(boolean b, String errorMsg) {
        if (b) {
            throw new RuntimeException(errorMsg);
        }
    }

    public static void notNull(Object o, String errorMsg) {
        if (null == o) {
            throw new RuntimeException(errorMsg);
        }
    }

    public static void notBlank(String s, String errorMsg) {
        if (null == s || s.length() == 0) {
            throw new RuntimeException(errorMsg);
        }
    }

    public static <T> void notEmpty(Collection<T> c, String errorMsg) {
        if (null == c || c.size() == 0) {
            throw new RuntimeException(errorMsg);
        }
    }

    public static <K, V> void notEmpty(Map<K, V> map, String errorMsg) {
        if (null == map || map.size() == 0) {
            throw new RuntimeException(errorMsg);
        }
    }

}
