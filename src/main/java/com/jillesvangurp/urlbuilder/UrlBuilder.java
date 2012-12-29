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
package com.jillesvangurp.urlbuilder;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;

/**
 * Simple builder class to construct urls out of a base url, one or more post fixes, and query parameters.
 *
 * The builder class enables you to not worry about things like trailing slashes, url encoding, etc.
 *
 */
public class UrlBuilder {

    private final StringBuilder url;
    // preserve order in which parameters are added
    private final Map<String, String> params = new LinkedHashMap<String, String>();

    private UrlBuilder(String baseUrl) {
        url = new StringBuilder(baseUrl);
    }

    public static UrlBuilder url(String baseUrl) {
        return new UrlBuilder(baseUrl);
    }

    public static UrlBuilder url(String host, int port) {
        return new UrlBuilder("http://" + host + ":" + port);
    }

    public String build() {
    	StringBuilder result=new StringBuilder(url);
        if(params.size() > 0) {
        	result.append('?');

            for (Entry<String, String> entry: params.entrySet()) {
                String value = entry.getValue();
                result.append(entry.getKey() + '=' + value);
                result.append('&');
            }
            result.deleteCharAt(result.length()-1); // remove trailing &
        }
        return result.toString();
    }

    public URL buildUrl() {
        try {
            return new URL(build());
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public UrlBuilder append(String... postFix) {
        return append(true,postFix);
    }

    public UrlBuilder append(boolean encode,String... postFix) {
        for (String part : postFix) {
            if(StringUtils.isNotBlank(part)) {
                if (url.charAt(url.length() - 1) != '/' && !part.startsWith("/")) {
                    url.append('/');
                }
                if(encode) {
                    try {
                        url.append(URLEncoder.encode(part, "UTF-8"));
                    } catch (UnsupportedEncodingException e) {
                        throw new IllegalStateException(e);
                    }
                } else {
                    url.append(part);
                }
            }
        }
        return this;
    }

    public UrlBuilder queryParam(String name, Boolean value) {
        if(value != null) {
            return queryParam(name, value.toString());
        } else {
            return null;
        }
    }

    public UrlBuilder queryParam(String name, Number value) {
        if(value != null) {
            return queryParam(name, value.toString());
        } else {
            return null;
        }
    }

    public UrlBuilder queryParam(String name, String value) {
        return queryParam(name, value, true);
    }

    public UrlBuilder queryParam(String name, String value, boolean escape) {
        if (StringUtils.isNotEmpty(value)) {
            if (escape) {
                try {
                    value = URLEncoder.encode(value, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    throw new IllegalStateException(e);
                }
            }
            params.put(name, value);
        }
        return this;
    }
}
