/*
 * Population
 * Copyright (C) 2016 Yuriy Budiyev [yuriy.budiyev@yandex.ru]
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
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
        Thread thread = new Thread(runnable, "Population-background-thread-" + THREAD_COUNTER.incrementAndGet());
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
