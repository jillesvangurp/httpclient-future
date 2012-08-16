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
package com.jillesvangurp.metrics;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class CounterRegistry {
    private final Map<Countable, AtomicLong> counters = new ConcurrentHashMap<Countable, AtomicLong>();
    private final Map<Countable, DurationCounter> durationCounters = new ConcurrentHashMap<Countable, DurationCounter>();

    public AtomicLong getCounter(Countable countable) {
        AtomicLong l = counters.get(countable);
        if(l == null) {
            synchronized(this) {
                if(durationCounters.containsKey(countable)) {
                    throw new IllegalArgumentException("Countable " + countable.name() + " is already registered via getDurationCounter()");
                }
                if(l == null) {
                    l = new AtomicLong();
                    counters.put(countable, l);
                }
            }
        }
        return l;
    }

    public DurationCounter getDurationCounter(Countable countable) {
        DurationCounter durationCounter = durationCounters.get(countable);
        if(durationCounter == null) {
            synchronized(this) {
                if(counters.containsKey(countable)) {
                    throw new IllegalArgumentException("Countable " + countable.name() + " is already registered via getCounter()");
                }
                if(durationCounter == null) {
                    durationCounter = new DurationCounter();
                    durationCounters.put(countable, durationCounter);
                }
            }
        }
        return durationCounter;
    }
}
