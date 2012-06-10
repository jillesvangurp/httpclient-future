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

import java.util.concurrent.Future;

import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.protocol.HttpContext;

/**
 * Api for the Future supporting http client. Implementations are expected to use the httpclient ResponseHandler
 * mechanism, or a similar mechanism for producing the value T that is returned from The Future<T> that is returned by
 * both execute methods.
 * @param <T>
 */
public interface FutureSupportingHttpClient<T> {
	/**
	 * Schedule a request for execution and return a Future instance.
	 * @param request this needs to be an abortable request. However, since we also need to use the
	 * httpclient.execute(HttpUriRequest request, ResponseHandler<T> responseHandler) method, we need it to be an
	 * HttpUriRequest as well. HttpRequestBase implements both interfaces and all relevant request types extend it.
	 * @return A Future<T> that supports cancel() and get() operations. If you call get, a value T is returned that is produced by executing the request.
	 */
	Future<T> execute(HttpRequestBase request);

	/**
	 * Schedule a request for execution and return a Future instance.
	 * @param request this needs to be an abortable request. However, since we also need to use the
	 * httpclient.execute(HttpUriRequest request, ResponseHandler<T> responseHandler) method, we need it to be an
	 * HttpUriRequest as well. HttpRequestBase implements both interfaces and all relevant request types extend it.
	 * @param context
	 * @return A Future<T> that supports cancel() and get() operations. If you call get, a value T is returned that is produced by executing the request.
	 */
	Future<T> execute(HttpRequestBase request, HttpContext context);
}
