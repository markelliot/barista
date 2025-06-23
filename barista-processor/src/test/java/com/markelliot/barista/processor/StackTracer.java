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

package com.markelliot.barista.processor;

import java.time.Duration;

public final class StackTracer implements AutoCloseable {
    private final Thread stackTraceThread;

    public StackTracer(Duration period) {
        stackTraceThread = new Thread(() -> {
            while (!Thread.interrupted()) {
                dumpThreads();
                try {
                    Thread.sleep(period.getSeconds());
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        stackTraceThread.start();
    }

    @Override
    public void close() {
        stackTraceThread.interrupt();
    }

    private static void dumpThreads() {
        StringBuilder sb = new StringBuilder();
        Thread.getAllStackTraces().forEach((t, trace) -> {
            sb.append(t.getName())
                    .append(" {id: ")
                    .append(t.getId())
                    .append(", isDaemon: ")
                    .append(t.isDaemon())
                    .append(", isInterrupted: ")
                    .append(t.isInterrupted())
                    .append("}\n");
            for (StackTraceElement traceElement : trace) {
                sb.append("\tat ").append(traceElement).append("\n");
            }
            sb.append("\n");
        });
        System.err.println("-------- Begin Stack Trace --------\n" + sb + "-------- End Stack Trace --------\n");
    }
}
