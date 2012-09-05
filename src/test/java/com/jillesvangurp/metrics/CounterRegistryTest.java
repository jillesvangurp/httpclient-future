package com.jillesvangurp.metrics;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.concurrent.atomic.AtomicLong;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public final class CounterRegistryTest {
    private CounterRegistry registry;

    private enum TestCounters implements Countable {
        foo,bar;
    }

    @BeforeMethod
    public void before() {
        registry = new CounterRegistry();
    }

    @Test
    public void shouldRegisterAndIncrementCounter() {

        AtomicLong counter1 = registry.getCounter(TestCounters.foo);
        counter1.incrementAndGet();
        AtomicLong counter2 = registry.getCounter(TestCounters.foo);
        assertThat(counter2, is(counter1));
        assertThat(counter2.get(), is(counter1.get()));
    }

    @Test
    public void shouldMeasureDurationAndCount() {
        DurationCounter counter = registry.getDurationCounter(TestCounters.foo);
        counter.increment(System.currentTimeMillis()-1500);
        counter.increment(System.currentTimeMillis()-500);
        assertThat(registry.getDurationCounter(TestCounters.foo).count(), is(2l));
        assertThat(registry.getDurationCounter(TestCounters.foo).averageDuration(), is(1000l));
    }
}
