package com.ljz.agent.model;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class HttpRequestFuture implements Future<AgentServerResponse> {

    private Runnable listener;

    private Executor executor;

    //private CountDownLatch countDownLatch = new CountDownLatch(1);

    private AgentServerResponse response;

    public void addListener(Runnable runnable, Executor executor) {
        this.listener = runnable;
        this.executor = executor;
    }

    public void done(AgentServerResponse response){
        this.response = response;
        //countDownLatch.countDown();
        executor.execute(listener);
    }

    @Override
    public AgentServerResponse get() throws InterruptedException, ExecutionException {
        //countDownLatch.await();
        return response;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public AgentServerResponse get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return null;
    }
}
