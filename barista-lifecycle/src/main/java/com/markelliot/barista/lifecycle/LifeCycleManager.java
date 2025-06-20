/*
 * (c) Copyright 2025 Mark Elliot. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.markelliot.barista.lifecycle;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

/**
 * Manages a collection of {@link LifeCycleAware}s such that {@link #start()} will start each {@link LifeCycleAware} in
 * the order it was registered and {@link #stop()} will stop each {@link LifeCycleAware} in the same order.
 *
 * <p>Note that calling {@link #close()} will additionally invoke {@link LifeCycleContext#stop()} to terminate the
 * context.
 *
 * <p>Use {@link #builder()} to construct an instance of this class, and {@link LifeCycleContext#builder()} to construct
 * an instance of {@link LifeCycleContext} to pass to the builder.
 *
 * <p>When using {@link Builder#stopOnShutdown()}, the shutdown hook is configured to call {@link #close()} to ensure
 * the context's resources also shutdown gracefully.
 */
public final class LifeCycleManager {
    private final LifeCycleContext context;
    private final List<LifeCycleAware> managed;

    public LifeCycleManager(LifeCycleContext context, List<LifeCycleAware> managed) {
        this.context = context;
        this.managed = managed;
    }

    public void start() {
        managed.forEach(LifeCycleAware::start);
    }

    public void stop() {
        managed.forEach(LifeCycleAware::stop);
    }

    public void close() {
        stop();
        context.stop();
    }

    public static ContextBuildStage builder() {
        return new Builder();
    }

    public interface ContextBuildStage {
        ManageBuildStage context(LifeCycleContext context);
    }

    public interface ManageBuildStage extends FinalBuildStage {
        ManageBuildStage manage(LifeCycleAware lifeCycleAware);

        ManageBuildStage manage(Scheduled scheduled);

        FinalBuildStage stopOnShutdown();
    }

    public interface FinalBuildStage {
        LifeCycleManager build();
    }

    public static final class Builder implements ContextBuildStage, ManageBuildStage, FinalBuildStage {
        private LifeCycleContext context;
        private final List<LifeCycleAware> managed = new ArrayList<>();
        private boolean stopOnShutdown = false;
        private boolean built = false;

        private Builder() {}

        @Override
        public Builder context(LifeCycleContext context) {
            this.context = context;
            return this;
        }

        @Override
        public Builder manage(LifeCycleAware lifeCycleAware) {
            managed.add(lifeCycleAware);
            return this;
        }

        @Override
        public Builder manage(Scheduled scheduled) {
            managed.add(new ScheduledLifeCycleAware(scheduled, context));
            return this;
        }

        @Override
        public Builder stopOnShutdown() {
            this.stopOnShutdown = true;
            return this;
        }

        @Override
        public LifeCycleManager build() {
            if (built) {
                throw new IllegalStateException("Cannot build a LifeCycleManager.Builder more than once");
            }
            built = true;

            LifeCycleManager lcm = new LifeCycleManager(context, List.copyOf(managed));
            if (stopOnShutdown) {
                Runtime.getRuntime().addShutdownHook(new Thread(lcm::close, "LifeCycleManager-ShutdownHook"));
            }
            return lcm;
        }
    }

    private static class ScheduledLifeCycleAware implements LifeCycleAware {
        private final Scheduled scheduled;
        private final LifeCycleContext context;
        private volatile ScheduledFuture<?> future;

        ScheduledLifeCycleAware(Scheduled scheduled, LifeCycleContext context) {
            this.scheduled = scheduled;
            this.context = context;
        }

        @Override
        public void start() {
            if (future == null) {
                Scheduled.Schedule schedule = scheduled.schedule();
                future = context.executorService()
                        .scheduleWithFixedDelay(
                                schedule.runnable(), schedule.initialDelay(), schedule.delay(), schedule.timeUnit());
            }
        }

        @Override
        public void stop() {
            if (future != null) {
                future.cancel(true);
                future = null;
            }
        }
    }
}
