package com.jillesvangurp.httpclientfuture;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.http.client.HttpClient;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.DecompressingHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;

/**
 * Helper class that allows you to construct an http client that is configured for durability and robustness.
 * HttpClient has a lot of configuration possibilities and out of the box features a very sensible configuration for
 * casual use. For non casual, production style usage, most users will want to tweak this configuration. This builder
 * class
 * makes this easy and allows you to specify timeouts, configure connection cleanup, and use a pooling connection
 * manager with
 * configured maximums for the total amount of connections and the per route connections.
 */
public class HttpClientBuilder {
    private int timeout = 3000;
    private int connectTimeout = 3000;
    private int maxConnectionsPerRoute = 10;
    private int maxConnections = 100;
    boolean handleRedirect = true;
    private ScheduledExecutorService executor = null;
    private int staleConnectionCleanupInterval;
    private int idleConnectionCloseTime;
    private boolean disableGzipCompression=false;

    private HttpClientBuilder() {
    }

    public static HttpClientBuilder client() {
        return new HttpClientBuilder();
    }

    /**
     * Configure timeouts.
     * @param socketTimeout timeout for receiving the first data after the connection has been established 
     * @param connectTimeout timeout for establishing a connection
     * @param tu
     * @return the builder
     */
    public HttpClientBuilder timeouts(int socketTimeout, int connectTimeout, TimeUnit tu) {
        this.timeout = (int) tu.toMillis(socketTimeout);
        this.connectTimeout = (int) tu.toMillis(connectTimeout);
        return this;
    }

    /**
     * Configure the maximum amount of pooled/open connections per route and in total.
     * @param max
     * @param maxPerRoute
     * @return the builder
     */
    public HttpClientBuilder connections(int max, int maxPerRoute) {
        maxConnections = max;
        maxConnectionsPerRoute = maxPerRoute;
        return this;
    }

    /**
     * Disable redirect following. Set this if you want to see 301 or 303 responses.
     * @return the builder
     */
    public HttpClientBuilder noRedirects() {
        handleRedirect = false;
        return this;
    }
    
    /**
     * By default this builder will use the decompressing httpclient. Use this if you don't want gzip compression.
     * @return the builder
     */
    public HttpClientBuilder disableCompression() {
        disableGzipCompression = true;
        return this;
    }

    /**
     * Configure an idle connection monitoring task that well periodically close idle and expired connections.
     * Note. this disables the default staleConnectionChecking.
     * Important, some webservers have configured timeouts on the server side. So, make sure you stay below this,
     * otherwise you may have connections in your
     * pool that are closed on the other side. Normally this behavior results in retries on the client side, which of
     * course affects
     * performance.
     * 
     * @param scheduledExecutorService
     *            executor that will schedule the thread. Since executors need to be properly shut down, you need to
     *            provide your own and make sure that
     *            happens. Executors.newSingleThreadScheduledExecutor() would provide a sensible default.
     * @param staleConnectionCleanupInterval
     *            interval that configures how often the the task should check
     * @param idleConnectionCloseTime
     *            maximum time that pooled connections are allowed to be idle before being closed
     * @param tu
     *            the timeunit for the two other parameters
     * @return the builder.
     */
    public HttpClientBuilder scheduleIdleConnectionMonitoring(ScheduledExecutorService scheduledExecutorService, int staleConnectionCleanupInterval,
            int idleConnectionCloseTime, TimeUnit tu) {
        this.executor = scheduledExecutorService;
        this.staleConnectionCleanupInterval = (int) tu.toMillis(staleConnectionCleanupInterval);
        this.idleConnectionCloseTime = (int) tu.toMillis(idleConnectionCloseTime);
        return this;
    }

    public HttpClient get() {
        HttpParams params = new BasicHttpParams();
        params.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, timeout);
        params.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, connectTimeout);
        params.setParameter(ClientPNames.HANDLE_REDIRECTS, handleRedirect);

        PoolingClientConnectionManager connectionManager = new PoolingClientConnectionManager();
        connectionManager.setDefaultMaxPerRoute(maxConnectionsPerRoute);
        connectionManager.setMaxTotal(maxConnections);
        if (executor == null) {
            params.setBooleanParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK, true);
        } else {
            IdleConnectionMonitor idleConnectionMonitor = new IdleConnectionMonitor(connectionManager, idleConnectionCloseTime);
            executor.scheduleWithFixedDelay(idleConnectionMonitor, staleConnectionCleanupInterval, staleConnectionCleanupInterval, TimeUnit.MILLISECONDS);
        }

        DefaultHttpClient httpClient = new DefaultHttpClient(connectionManager, params);

        if(disableGzipCompression) {
            return httpClient;
        } else {
            return new DecompressingHttpClient(httpClient);
        }
    }

    /**
     * Adapted from http://hc.apache.org/httpcomponents-client/tutorial/html/connmgmt.html .
     */
    private static class IdleConnectionMonitor implements Runnable {

        private final ClientConnectionManager connectionManager;
        private final int idleConnectionCloseTimeSeconds;

        public IdleConnectionMonitor(final ClientConnectionManager connMgr, int idleConnectionCloseTime) {
            this.connectionManager = connMgr;
            this.idleConnectionCloseTimeSeconds = idleConnectionCloseTime;
        }

        public final void run() {
            // Close expired connections
            this.connectionManager.closeExpiredConnections();
            this.connectionManager.closeIdleConnections(idleConnectionCloseTimeSeconds, TimeUnit.MILLISECONDS);
        }
    }
}
