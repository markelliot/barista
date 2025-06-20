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

import java.util.concurrent.TimeUnit;

/**
 * An alternative to {@link LifeCycleAware} that classes wishing to schedule a repeating task from
 * {@link LifeCycleManager#start()} until {@link LifeCycleManager#stop()} without doing the bookkeeping of a future.
 */
public interface Scheduled {
    Schedule schedule();

    record Schedule(Runnable runnable, long initialDelay, long delay, TimeUnit timeUnit) {}
}
