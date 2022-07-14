package com.joseph.sharpknife.blade.kits;

/**
 * @author Joseph
 *  2022-02-01 22:19
 */
public class Clock {

    public static long millisNow() {
        return System.currentTimeMillis();
    }

    public static long millisDiff(long t1, long t2) {
        return t2 - t1;
    }
}
