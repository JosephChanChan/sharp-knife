package com.joseph.sharpknife.blade.unit;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author Joseph
 */
public class Hooker {

    int order;
    Consumer<Void> consumer ;

    public Hooker(int order, Consumer<Void> consumer) {
        this.order = order;
        this.consumer = consumer;
    }

    public void invoke() {
        consumer.accept(null);
    }

    public int getOrder() {
        return order;
    }
}
