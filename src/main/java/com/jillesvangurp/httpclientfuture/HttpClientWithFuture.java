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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.protocol.HttpContext;

import com.jillesvangurp.metrics.CounterRegistry;

/**
 * Allows treatment of the result of an httpclient call as a Future, including allowing it to be cancelled or timing out. This class uses an executor and a response handler
 * that you provide in the constructor to execute the request in a separate thread.
 * If you call cancel, the call is aborted; or if it hasn't started yet it will never launch. You should of course align
 * the
 * number of threads in the executor with the number of connections available to httpclient.
 *
 * @param <T>
 *            the type of the value returned by the response handler you use
 */
public final class HttpClientWithFuture<T> {
    final HttpClient httpclient;

    private final ExecutorService executorService;

    private final ResponseHandler<T> responseHandler;

    // FIXME inject this
    private final ConnectionMetrics metrics = new ConnectionMetrics(new CounterRegistry());

    /**
     * Create a new client instance. The instance is thread safe and you should only need one. You may want to create
     * multiple clients for each web service type instead of having them share the executor pool. That way, you have
     * more fine-grained control over the number of requests flowing in each direction.
     *
     * @param httpclient
     *            you should tune your httpclient instance to match your needs. You should align the max number
     *            of connections in the pool and the number of threads in the executor; it doesn't make sense to have
     *            more threads
     *            than connections and if you have less connections than threads, the threads will just end up blocking
     *            on getting
     *            a connection from the pool.
     * @param executorService
     *            any executorService will do here. E.g. Executors.newFixedThreadPool(numberOfThreads)
     * @param responseHandler
     *            a httpclient response handler. This object is responsible for handling responses and
     *            extracting whichever value T you need. It must be thread-safe.
     */
    public HttpClientWithFuture(HttpClient httpclient, ExecutorService executorService, ResponseHandler<T> responseHandler) {
        this.httpclient = httpclient;
        this.executorService = executorService;
        this.responseHandler = responseHandler;
    }

    public HttpClientFutureTask<T> execute(final HttpRequestBase request) throws InterruptedException {
        return execute(request, null, null);
    }

    public List<Future<T>> executeMultiple(HttpRequestBase...requests) throws InterruptedException {
        return executeMultiple(null, -1, null, requests);
    }

    public List<Future<T>> executeMultiple(HttpContext context, long timeout, TimeUnit timeUnit, HttpRequestBase...requests) throws InterruptedException {
        metrics.scheduledConnections.incrementAndGet();
        List<Callable<T>> callables = new ArrayList<Callable<T>>();
        for (HttpRequestBase request: requests) {
            LoggingHttpClientTaskLifecycleCallback callback = new LoggingHttpClientTaskLifecycleCallback(request.getURI().toString());
            HttpClientCallable<T> callable = new HttpClientCallable<T>(httpclient, responseHandler, request, context, callback,metrics);
            callables.add(callable);
        }
        if(timeout > 0) {
            return executorService.invokeAll(callables, timeout, timeUnit);
        } else {
            return executorService.invokeAll(callables);
        }

    }


    public HttpClientFutureTask<T> execute(HttpRequestBase request, HttpContext context, HttpClientTaskLifecycleCallback callback) throws InterruptedException {
        metrics.scheduledConnections.incrementAndGet();
        if(callback == null) {
            callback = new LoggingHttpClientTaskLifecycleCallback(request.getURI().toString());
        }
        HttpClientCallable<T> callable = new HttpClientCallable<T>(httpclient, responseHandler, request, context, callback,metrics);
        HttpClientFutureTask<T> httpRequestFutureTask = new HttpClientFutureTask<T>(request, callable, callable.cancelled,callback);

        executorService.execute(httpRequestFutureTask);

        return httpRequestFutureTask;
    }

    public ConnectionMetrics metrics() {
        return metrics;
    }
}
