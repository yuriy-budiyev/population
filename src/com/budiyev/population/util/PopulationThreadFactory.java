package com.budiyev.population.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class PopulationThreadFactory implements ThreadFactory {
    private static final AtomicInteger THREAD_COUNTER = new AtomicInteger();
    private final Thread.UncaughtExceptionHandler mUncaughtExceptionHandler;

    public PopulationThreadFactory(Thread.UncaughtExceptionHandler uncaughtExceptionHandler) {
        mUncaughtExceptionHandler = uncaughtExceptionHandler;
    }

    @Override
    public Thread newThread(Runnable runnable) {
        THREAD_COUNTER.compareAndSet(Integer.MAX_VALUE, 0);
        Thread thread = new Thread(runnable,
                "Population-background-thread-" + THREAD_COUNTER.incrementAndGet());
        thread.setUncaughtExceptionHandler(mUncaughtExceptionHandler);
        if (!thread.isDaemon()) {
            thread.setDaemon(true);
        }
        if (thread.getPriority() != Thread.NORM_PRIORITY) {
            thread.setPriority(Thread.NORM_PRIORITY);
        }
        return thread;
    }
}
