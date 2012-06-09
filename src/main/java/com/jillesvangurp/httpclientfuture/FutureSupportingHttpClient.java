package com.jillesvangurp.httpclientfuture;

import java.util.concurrent.Future;

import org.apache.http.client.methods.HttpRequestBase;

public interface FutureSupportingHttpClient<T> {
	Future<T> execute(HttpRequestBase request);
}
