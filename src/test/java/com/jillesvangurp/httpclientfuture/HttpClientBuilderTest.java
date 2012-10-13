package com.jillesvangurp.httpclientfuture;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DecompressingHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.testng.annotations.Test;

@Test
public class HttpClientBuilderTest {
    public void shouldConstructHttpClient() {
        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        try {
            HttpClient httpClient = HttpClientBuilder.client().connections(6, 3).timeouts(2, 3, TimeUnit.SECONDS)
                    .scheduleIdleConnectionMonitoring(scheduledExecutorService, 4, 5, TimeUnit.SECONDS).get();
            
            assertThat(httpClient.getParams().getIntParameter(CoreConnectionPNames.SO_TIMEOUT, -1), is(2000));
            assertThat(httpClient.getParams().getIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, -1), is(3000));
            assertThat("should be wrapped with DecompressingHttpClient", httpClient instanceof DecompressingHttpClient);
        } finally {
            scheduledExecutorService.shutdownNow();
        }
    }
}
