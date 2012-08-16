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

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.protocol.HttpContext;

final class HttpClientCallable<V> implements Callable<V> {

    private final ResponseHandler<V> responseHandler;

    private final HttpUriRequest request;

    private final HttpClient httpclient;

    final AtomicBoolean cancelled = new AtomicBoolean(false);

    private final HttpContext context;

    final long scheduled = System.currentTimeMillis();
    long started = -1;
    long ended = -1;

    private final HttpClientTaskLifecycleCallback callback;

    private final ConnectionMetrics metrics;

    HttpClientCallable(HttpClient httpClient, ResponseHandler<V> responseHandler, HttpUriRequest request, HttpContext context, HttpClientTaskLifecycleCallback callback, ConnectionMetrics metrics) {
        this.httpclient = httpClient;
        this.responseHandler = responseHandler;
        this.request = request;
        this.context = context;
        this.callback = callback;
        this.metrics = metrics;
    }

    @Override
	public V call() throws Exception {
        if (!cancelled.get()) {
            try {
                metrics.activeConnections.incrementAndGet();
                started = System.currentTimeMillis();
                try {
                    callback.startRequest();
                    metrics.scheduledConnections.decrementAndGet();
                    V result = httpclient.execute(request, responseHandler, context);
                    ended = System.currentTimeMillis();
                    metrics.successfulConnections.increment(started);
                    callback.success();
                    return result;
                } catch (Exception e) {
                    metrics.failedConnections.increment(started);
                    ended = System.currentTimeMillis();
                    callback.failure(e);
                    throw e;
                }
            } finally {
                metrics.requests.increment(started);
                metrics.tasks.increment(started);
                metrics.activeConnections.decrementAndGet();
            }
        } else {
            throw new IllegalStateException("call has been cancelled for request " + request.getURI());
        }
    }
}