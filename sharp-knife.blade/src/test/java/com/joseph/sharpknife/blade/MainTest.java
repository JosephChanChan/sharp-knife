package com.joseph.sharpknife.blade;

import com.joseph.sharpknife.blade.exception.SharpKnifeException;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Joseph
 */
@Slf4j
public class MainTest {

    public static void main(String[] args) {
        MainTest m = new MainTest();
        m.exception();
    }

    void exception() {
        try {
            int i = 1/0;
        }
        catch (Exception e) {
            Exception exception = e;
            exception = new SharpKnifeException(String.format("suppressed exception! original error msg=%s", exception.getMessage()), exception, true);
            log.error("e=", exception);
        }
    }
}
