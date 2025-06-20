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

    final class DefaultLifeCycleAware implements LifeCycleAware {
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
