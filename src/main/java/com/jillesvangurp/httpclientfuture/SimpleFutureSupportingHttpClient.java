package com.jillesvangurp.httpclientfuture;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;

/**
 * Allows treatment of the result of an httpclient call as a Future. This class uses an executor and a response handler
 * that you provide in the constructor to execute the request in a separate thread.
 *
 * If you call cancel, the call is aborted; or if it hasn't started yet it will never launch. You should of course align the
 * number of threads in the executor with the number of connections available to httpclient.
 *
 * @param <T> the type of the value returned by the response handler you use
 */
public class SimpleFutureSupportingHttpClient<T> implements FutureSupportingHttpClient<T> {
	final HttpClient httpclient;

	private final Executor executor;

	private final ResponseHandler<T> responseHandler;

	public SimpleFutureSupportingHttpClient(HttpClient httpclient, Executor executor, ResponseHandler<T> responseHandler) {
		this.httpclient = httpclient;
		this.executor = executor;
		this.responseHandler = responseHandler;
	}

	@Override
	public Future<T> execute(final HttpRequestBase request) {
		HttpClientCallable callable = new HttpClientCallable(httpclient, responseHandler, request);
		HttpClientFutureTask httpRequestFutureTask = new HttpClientFutureTask(request, callable, callable.cancelled());
		executor.execute(httpRequestFutureTask);
		return httpRequestFutureTask;
	}

	private class HttpClientFutureTask extends FutureTask<T> {

		private final HttpUriRequest request;

		private final AtomicBoolean cancelled;

		public HttpClientFutureTask(final HttpUriRequest request, HttpClientCallable httpCallable,
				AtomicBoolean cancelled) {
			super(httpCallable);
			this.request = request;
			this.cancelled = cancelled;
		}

		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			cancelled.set(true);
			if (mayInterruptIfRunning) {
				request.abort();
			}

			return super.cancel(mayInterruptIfRunning);
		}
	}

	private class HttpClientCallable implements Callable<T> {
		private final ResponseHandler<T> responseHandler;

		private final HttpUriRequest request;

		private final HttpClient httpclient;

		private final AtomicBoolean cancelled = new AtomicBoolean(false);

		private HttpClientCallable(HttpClient httpClient, ResponseHandler<T> responseHandler, HttpUriRequest request) {
			this.httpclient = httpClient;
			this.responseHandler = responseHandler;
			this.request = request;
		}

		@Override
		public T call() throws Exception {
			if (!cancelled.get()) {
				return httpclient.execute(request, responseHandler);
			}
			else {
				throw new IllegalStateException("call has been cancelled for request " + request.getURI());
			}
		}

		public AtomicBoolean cancelled() {
			return cancelled;
		}
	}
}
