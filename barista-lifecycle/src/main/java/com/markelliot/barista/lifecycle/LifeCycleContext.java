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

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Contains resources to used by {@link com.markelliot.barista.lifecycle.LifeCycleAware}.
 *
 * <p>When {@link #stop()} is invoked, all contained resources will be aggressively stopped. Executor services will be
 * called with {@link java.util.concurrent.ExecutorService#shutdownNow()} to ensure speedy closure.
 */
public interface LifeCycleContext {
    ScheduledExecutorService executorService();

    void stop();

    static ExecutorServiceBuildStage builder() {
        return new Builder();
    }

    interface ExecutorServiceBuildStage {
        FinalBuildStage executorService(ScheduledExecutorService executorService);

        /** Sets the executor service to a new scheduled thread pool with core pool size 1. */
        FinalBuildStage useDefaultExecutorService();
    }

    interface FinalBuildStage {
        LifeCycleContext build();
    }

    final class Builder implements ExecutorServiceBuildStage, FinalBuildStage {
        private ScheduledExecutorService executorService;

        private Builder() {}

        @Override
        public Builder executorService(ScheduledExecutorService executorService) {
            this.executorService = executorService;
            return this;
        }

        @Override
        public FinalBuildStage useDefaultExecutorService() {
            this.executorService = Executors.newScheduledThreadPool(1);
            return this;
        }

        @Override
        public LifeCycleContext build() {
            return new DefaultLifeCycleContext(executorService);
        }

        private record DefaultLifeCycleContext(ScheduledExecutorService executorService) implements LifeCycleContext {
            @Override
            public void stop() {
                executorService.shutdownNow();
            }
        }
    }
}
