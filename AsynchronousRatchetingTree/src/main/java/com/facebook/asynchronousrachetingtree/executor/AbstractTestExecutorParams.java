package com.facebook.asynchronousrachetingtree.executor;

public class AbstractTestExecutorParams implements ExecutionParams {

    private int n=0;
    private int activeCount=0;
    private boolean debug=false;

    public AbstractTestExecutorParams(int n, int activeCount, boolean debug)
    {
        this.n=n;
        this.activeCount=activeCount;
        this.debug=debug;
    }

    public int getN() {
        return n;
    }

    public boolean isDebug() {
        return debug;
    }

    public int getActiveCount() {
        return activeCount;
    }
}
