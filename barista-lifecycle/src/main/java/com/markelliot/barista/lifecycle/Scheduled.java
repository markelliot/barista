package com.markelliot.barista.lifecycle;

import java.util.concurrent.TimeUnit;

/**
 * An alternative to {@link LifeCycleAware} that classes wishing to schedule a repeating task from
 * {@link LifeCycleManager#start()} until {@link LifeCycleManager#stop()} without doing the bookkeeping of a future.
 */
public interface Scheduled {
    Schedule schedule();

    record Schedule(Runnable runnable, long initialDelay, long delay, TimeUnit timeUnit) {}
}
