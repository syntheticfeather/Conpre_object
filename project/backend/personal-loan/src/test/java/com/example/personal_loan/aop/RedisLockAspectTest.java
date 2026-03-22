package com.example.personal_loan.aop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

@SpringBootTest(classes = { RedisLockAspectTest.TestConfig.class })
class RedisLockAspectTest {

    @Configuration
    static class TestConfig {
        @Bean
        TestService testService() {
            return new TestService();
        }
    }

    @Service
    static class TestService {
        private final AtomicInteger counter = new AtomicInteger();

        @RedisLocked(key = "'lock:test:' + #p0", waitTime = 0, leaseTime = 5, timeUnit = TimeUnit.SECONDS, returnNullOnFail = true)
        public Integer work(String id, CountDownLatch entered, CountDownLatch release) throws InterruptedException {
            entered.countDown();
            release.await(2, TimeUnit.SECONDS);
            return counter.incrementAndGet();
        }
    }

    @Autowired
    private TestService testService;

    @Test
    void returnsNullWhenLockNotAcquired() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        AtomicInteger firstResult = new AtomicInteger(-1);

        Thread t1 = new Thread(() -> {
            try {
                Integer result = testService.work("1", entered, release);
                firstResult.set(result == null ? -1 : result);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        });

        t1.start();
        entered.await(2, TimeUnit.SECONDS);

        Integer second = testService.work("1", new CountDownLatch(0), new CountDownLatch(0));
        assertNull(second);

        release.countDown();
        t1.join(2000);

        assertEquals(1, firstResult.get());
    }
}
