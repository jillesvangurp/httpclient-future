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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingHttpClientTaskLifecycleCallback implements HttpClientTaskLifecycleCallback {
    private static final Logger LOG = LoggerFactory.getLogger(LoggingHttpClientTaskLifecycleCallback.class);
    private final String name;

    public LoggingHttpClientTaskLifecycleCallback(String name) {
        this.name = name;
    }

    /* (non-Javadoc)
     * @see com.nokia.search.httpclientfuture.HttpClientTaskLifecycleCallback#scheduleRequest()
     */
    @Override
	public void scheduleRequest() {
        LOG.trace("schedule request " + name);
    }

    /* (non-Javadoc)
     * @see com.nokia.search.nosedos.TaskLifecycleCallback#startRequest(com.nokia.search.nosedos.HttpClientFutureTask)
     */
    @Override
	public void startRequest() {
        LOG.trace("start request " + name);
    }

    /* (non-Javadoc)
     * @see com.nokia.search.nosedos.TaskLifecycleCallback#success(com.nokia.search.nosedos.HttpClientFutureTask)
     */
    @Override
	public void success() {
        LOG.debug("successfully completed " + name);
    }

    /* (non-Javadoc)
     * @see com.nokia.search.nosedos.TaskLifecycleCallback#failure(com.nokia.search.nosedos.HttpClientFutureTask)
     */
    @Override
	public void failure(Throwable t) {
        LOG.debug("failed  " + name);
    }

    /* (non-Javadoc)
     * @see com.nokia.search.nosedos.TaskLifecycleCallback#cancelled(com.nokia.search.nosedos.HttpClientFutureTask)
     */
    @Override
	public void cancelled() {
        LOG.debug("cancelled  " + name);
    }
}