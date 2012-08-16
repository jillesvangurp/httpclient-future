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

import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.http.client.methods.HttpUriRequest;

public class HttpClientFutureTask<V> extends FutureTask<V> {

    private final HttpUriRequest request;

    private final AtomicBoolean cancelled;

    private final HttpClientCallable<V> callable;

    private final HttpClientTaskLifecycleCallback callback;

    public HttpClientFutureTask(final HttpUriRequest request, HttpClientCallable<V> httpCallable, AtomicBoolean cancelled, HttpClientTaskLifecycleCallback callback) {
        super(httpCallable);
        this.request = request;
        this.callable = httpCallable;
        this.cancelled = cancelled;
        this.callback = callback;
        callback.scheduleRequest();
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        cancelled.set(true);
        if (mayInterruptIfRunning) {
            request.abort();
        }
        callback.cancelled();

        return super.cancel(mayInterruptIfRunning);
    }

    public AtomicBoolean cancelled() {
        return cancelled;
    }

    public long scheduledTime() {
        return callable.scheduled;
    }

    public long startedTime() {
        return callable.started;
    }

    public long endedTime() {
        return callable.ended;
    }

    public long requestDuration() {
        if (endedTime() > 0) {
            return endedTime() - startedTime();
        } else {
            return 0;
        }
    }

    public long taskDuration() {
        if (endedTime() > 0) {
            return endedTime() - scheduledTime();
        } else {
            return 0;
        }
    }

    @Override
    public String toString() {
        return request.getRequestLine().getUri();
    }

    @Override
    protected void done() {
    }
}