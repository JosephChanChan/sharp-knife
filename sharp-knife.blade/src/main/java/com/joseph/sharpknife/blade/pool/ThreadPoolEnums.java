package com.joseph.sharpknife.blade.pool;

/**
 * @author Joseph
 */
public enum ThreadPoolEnums {

    DEFAULT("DEFAULT"),
    HYSTRIX("HYSTRIX")
    ;

    String type;

    ThreadPoolEnums(String type) {
        this.type = type;
    }


}
