package com.markelliot.barista.lifecycle;

/**
 * Provides an easy integration with LifeCycleManager such that {@link #start()} will be called whenever
 * {@code LifeCycleManager#start()} is called and {@link #stop()} whenever {@code LifeCycleManager#stop()} is called.
 *
 * <p>Use {@link #wrap(Runnable, Runnable)} to create a {@link LifeCycleAware} for classes where it is either infeasible
 * or undesirable to directly implement this class.
 */
public interface LifeCycleAware {
    void start();

    void stop();

    static LifeCycleAware wrap(Runnable start, Runnable stop) {
        return new DefaultLifeCycleAware(start, stop);
    }

    class DefaultLifeCycleAware implements LifeCycleAware {
        private final Runnable start;
        private final Runnable stop;

        private DefaultLifeCycleAware(Runnable start, Runnable stop) {
            this.start = start;
            this.stop = stop;
        }

        @Override
        public void start() {
            start.run();
        }

        @Override
        public void stop() {
            stop.run();
        }
    }
}
