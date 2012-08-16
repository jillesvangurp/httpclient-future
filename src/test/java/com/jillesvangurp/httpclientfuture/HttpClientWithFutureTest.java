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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.thread.ExecutorThreadPool;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class HttpClientWithFutureTest {

    private ExecutorService webServerExecutor;
    private HttpClientWithFuture<Boolean> client;
    private ExecutorService clientThreadPool;
    private final int port=6666;
    private WebServer webServer;
    private final int threads = 5;

    @BeforeClass
    public void beforeClass() {
        webServerExecutor = Executors.newSingleThreadExecutor();
        webServer = new WebServer();
        webServerExecutor.execute(webServer);
        clientThreadPool = Executors.newFixedThreadPool(threads);
        PoolingClientConnectionManager conman = new PoolingClientConnectionManager();
        conman.setDefaultMaxPerRoute(threads);
        conman.setMaxTotal(threads);
        DefaultHttpClient httpclient = new DefaultHttpClient(conman);
        client = new HttpClientWithFuture<Boolean>(httpclient, clientThreadPool, new ResponseHandler<Boolean>() {
            @Override
			public Boolean handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
                return response.getStatusLine().getStatusCode() == 200;
            }
        });
    }

    @AfterClass
    public void afterClass() throws Exception {
        try {
            webServer.shutDown();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
        webServerExecutor.shutdownNow();
        webServerExecutor.awaitTermination(1, TimeUnit.SECONDS);
        clientThreadPool.shutdownNow();
        clientThreadPool.awaitTermination(1, TimeUnit.SECONDS);
    }

    @BeforeMethod
    public void beforeMethod() {
        awaitActiveConnectionsFinished();
    }

    @Test
    public void shouldQueryServer() throws InterruptedException, ExecutionException, TimeoutException {
        Future<Boolean> future = client.execute(new HttpGet(UrlBuilder.url("localhost", port).append("ping").queryParam("sleep", "1").queryParam("req", "shouldQueryServer").build()));

        Boolean result = future.get();

        assertThat("should have returned with an OK",result, is(true));
    }

    @Test(expectedExceptions=TimeoutException.class)
    public void shouldCancel() throws Exception {
        Future<Boolean> future = client.execute(new HttpGet(UrlBuilder.url("localhost", port).append("ping").queryParam("sleep", "150").build()));
        future.get(100, TimeUnit.MILLISECONDS);
    }

    @Test(invocationCount=3)
    public void shouldInvokeMultiple() throws ExecutionException, TimeoutException {
        long before = SimpleServlet.counter.get();
        HttpGet req1 = new HttpGet(UrlBuilder.url("localhost", port).append("ping").queryParam("sleep", "10").queryParam("req", "shouldInvokeMultiple_1").build());
        HttpGet req2 = new HttpGet(UrlBuilder.url("localhost", port).append("ping").queryParam("sleep", "10").queryParam("req", "shouldInvokeMultiple_2").build());
        HttpGet req3 = new HttpGet(UrlBuilder.url("localhost", port).append("ping").queryParam("sleep", "3000").queryParam("req", "shouldInvokeMultiple_3").build());
        try {
            List<Future<Boolean>> futures = client.executeMultiple(null, 100, TimeUnit.MILLISECONDS, req1,req2,req3);
            int cancelled = 0;
            for (Future<Boolean> future : futures) {
                if(future.isCancelled()) {
                    cancelled++;
                } else {
                    boolean done = future.isDone();
                    if(done) {
                        future.get(1, TimeUnit.MILLISECONDS);
                    }

                }
            }
            assertThat(cancelled, is(1));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        long after = SimpleServlet.counter.get();
        assertThat(after, is(before+2));
    }

    @Test
    public void shouldCompleteServerSideAfterCancel() throws Exception {
        // this is quite sensitive to timings, therefore we try several requests and then assert that some requests were allowed to complete.
        int completed=0;
        for(int i=0;i< 10; i++) {
            long before = SimpleServlet.counter.get();
            Future<Boolean> future = client.execute(new HttpGet(UrlBuilder.url("localhost", port).append("ping").queryParam("sleep", "50").queryParam("req", "shouldCompleteServerSideAfterCancel_"+i).build()));
            // give httpclient some time to get around to firing the request
            Thread.sleep(20);
            future.cancel(true);
            awaitActiveConnectionsFinished();
            // give response handler some time to wrap things up
            Thread.sleep(20);
            long after = SimpleServlet.counter.get();
            completed += after-before;
        }
        assertThat("some requests should have completed, despite the cancel",completed, greaterThan(2));
    }

    private void awaitActiveConnectionsFinished() {
        while(client.metrics().activeConnections() > 0) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class SimpleServlet extends HttpServlet {
        private static final long serialVersionUID = 3705413796805824807L;
        public static final AtomicLong counter = new AtomicLong(0);

        @Override
        protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            String sleep = req.getParameter("sleep");
            if(sleep != null) {
                Long time = Long.valueOf(sleep);
                try {
                    Thread.sleep(time);
                } catch (InterruptedException e) {
                    System.err.println("interrupted");
                }
            }
            resp.setStatus(200);
            PrintWriter writer = resp.getWriter();
            writer.print(req.getRequestURL().toString());
            writer.flush();
            counter.getAndIncrement();
        }
    }

    private final class WebServer implements Runnable {
        private Server server;

        @Override
		public void run() {
            try {
                server = new Server(port);
                ServletContextHandler servletContextHandler = new ServletContextHandler(server, "/", true, false);
                servletContextHandler.addServlet(SimpleServlet.class, "/ping");
                server.setThreadPool(new ExecutorThreadPool(10, 10, 100, TimeUnit.SECONDS));
                server.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void shutDown() throws Exception {
            server.stop();
        }
    }
}
