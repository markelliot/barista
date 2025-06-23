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
