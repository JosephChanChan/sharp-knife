package com.joseph.sharpknife.blade.exception;

/**
 * @author Joseph
 */
public class SharpKnifeException extends RuntimeException {

    private static final StackTraceElement[] EMPTY_TRACES = new StackTraceElement[0];
    String errorMsg;

    public SharpKnifeException(String errorMsg) {
        super(errorMsg);
        this.errorMsg = errorMsg;
    }

    public SharpKnifeException(String errorMsg, Throwable cause) {
        super(errorMsg, cause);
        setStackTrace(new StackTraceElement[0]);
        this.errorMsg = errorMsg;
    }

    public SharpKnifeException(String errorMsg, Throwable cause, boolean suppress) {
        super(errorMsg, cause);
        if (suppress) {
            cause.setStackTrace(EMPTY_TRACES);
            this.setStackTrace(EMPTY_TRACES);
        }
        this.errorMsg = errorMsg;
    }

    public static Exception suppressStackTraces(Exception e) {
        return new SharpKnifeException(String.format("suppressed exception! original error msg=%s", e.getMessage()), e, true);
    }

}
