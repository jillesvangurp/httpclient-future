/**
 * Copyright (c) 2012, Jilles van Gurp
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.jillesvangurp.httpclientfuture;

import java.util.concurrent.atomic.AtomicLong;

import com.jillesvangurp.metrics.Countable;
import com.jillesvangurp.metrics.CounterRegistry;
import com.jillesvangurp.metrics.DurationCounter;

public class ConnectionMetrics {
    enum Counters implements Countable {
        activeConnections,scheduledConnections,successfulConnections,failedConnections,totalConnections,requests,tasks;
    }

    final AtomicLong activeConnections;
    final AtomicLong scheduledConnections;
    final DurationCounter successfulConnections;
    final DurationCounter failedConnections;
    final DurationCounter requests;
    final DurationCounter tasks;

    public ConnectionMetrics(CounterRegistry counterRegistry) {
        activeConnections = counterRegistry.getCounter(Counters.activeConnections);
        scheduledConnections = counterRegistry.getCounter(Counters.scheduledConnections);
        successfulConnections = counterRegistry.getDurationCounter(Counters.successfulConnections);
        failedConnections = counterRegistry.getDurationCounter(Counters.failedConnections);
        requests = counterRegistry.getDurationCounter(Counters.requests);
        tasks = counterRegistry.getDurationCounter(Counters.tasks);
    }

    public String metricsAsJson() {
        StringBuilder buf = new StringBuilder();
        buf.append("{\n");
        buf.append("  \"totalConnections\":" + requests.count() + ",\n");
        buf.append("  \"failedConnections\":" + failedConnections + ",\n");
        buf.append("  \"successfulConnections\":" + successfulConnections + ",\n");
        buf.append("  \"averageRequestDuration\":" + requests.averageDuration() + ",\n");
        buf.append("  \"averageTaskDuration\":" + tasks.averageDuration() + ",\n");
        buf.append("  \"activeConnections\":" + activeConnections + ",\n");
        buf.append("  \"scheduledConnections\":" + scheduledConnections + "\n");
        buf.append("}\n");

        return buf.toString();
    }

    public long activeConnections() {
        return activeConnections.get();
    }

    public long scheduledConnections() {
        return scheduledConnections.get();
    }

    @Override
    public String toString() {
        return metricsAsJson();
    }
}