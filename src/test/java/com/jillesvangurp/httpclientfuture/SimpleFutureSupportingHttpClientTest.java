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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class SimpleFutureSupportingHttpClientTest {

	private SimpleFutureSupportingHttpClient<String> client;
	private ExecutorService newFixedThreadPool;
	private DefaultHttpClient httpclient;

	@BeforeMethod
	public void before() {
		httpclient = new DefaultHttpClient();
		newFixedThreadPool = Executors.newFixedThreadPool(10);
		client = new SimpleFutureSupportingHttpClient<String>(httpclient, newFixedThreadPool, new StringHandler());
	}

	@AfterMethod
	public void after() {
		httpclient.getConnectionManager().shutdown();
		newFixedThreadPool.shutdownNow();
	}

	@Test
	public void shouldDoRequest() throws InterruptedException, ExecutionException {
		Future<String> future = client.execute(new HttpGet("http://www.nu.nl"));
		String response = future.get();
		Assert.assertNotNull(response);
	}

	@Test(expectedExceptions=CancellationException.class)
	public void shouldCancelRequest() throws InterruptedException, ExecutionException {
		Future<String> future = client.execute(new HttpGet("http://www.nu.nl"));
		Thread.sleep(50);
		Assert.assertTrue(future.cancel(true));

		future.get();
	}

	@Test(expectedExceptions=ExecutionException.class)
	public void shouldFailWithError() throws InterruptedException, ExecutionException {
		Future<String> future = client.execute(new HttpGet("http://www.domain.cmo"));
		future.get();
	}

	private final class StringHandler implements ResponseHandler<String> {
		@Override
		public String handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			response.getEntity().writeTo(bos);
			return bos.toString();
		}
	}
}
