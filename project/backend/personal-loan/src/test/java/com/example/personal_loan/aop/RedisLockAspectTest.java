package com.example.personal_loan.aop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.stereotype.Service;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringJUnitConfig(classes = RedisLockAspectTest.TestConfig.class)
class RedisLockAspectTest {

    @Configuration
    @EnableTransactionManagement
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class TestConfig {

        @Bean
        TestService testService() {
            return new TestService();
        }

        @Bean
        RedissonClient redissonClient() {
            var locks = new ConcurrentHashMap<String, org.redisson.api.RLock>();
            return org.mockito.Mockito.mock(RedissonClient.class, invocation -> {
                String methodName = invocation.getMethod().getName();
                if ("getLock".equals(methodName)) {
                    String key = (String) invocation.getArguments()[0];
                    return locks.computeIfAbsent(key, ignored -> buildMockLock());
                }
                throw new UnsupportedOperationException("RedissonClient." + methodName + " is not supported in this test");
            });
        }

        @Bean
        RedisLockAspect redisLockAspect(RedissonClient redissonClient) {
            return new RedisLockAspect(redissonClient);
        }

        private static RLock buildMockLock() {
            ReentrantLock lock = new ReentrantLock();
            return org.mockito.Mockito.mock(RLock.class, invocation -> {
                String methodName = invocation.getMethod().getName();
                Object[] args = invocation.getArguments();
                return switch (methodName) {
                    case "tryLock" -> {
                        if (args.length == 0) {
                            yield lock.tryLock();
                        }
                        if (args.length == 2) {
                            yield lock.tryLock((long) args[0], (TimeUnit) args[1]);
                        }
                        if (args.length == 3) {
                            yield lock.tryLock((long) args[0], (TimeUnit) args[2]);
                        }
                        throw new UnsupportedOperationException("RLock.tryLock args=" + args.length);
                    }
                    case "isHeldByCurrentThread" -> lock.isHeldByCurrentThread();
                    case "unlock" -> {
                        lock.unlock();
                        yield null;
                    }
                    default -> throw new UnsupportedOperationException("RLock." + methodName + " is not supported in this test");
                };
            });
        }
    }

    @Service
    static class TestService {
        private final AtomicInteger counter = new AtomicInteger();

        public int getCounter() {
            return counter.get();
        }

        @RedisLocked(key = "'lock:test:' + #p0", waitTime = 0, leaseTime = 5, timeUnit = TimeUnit.SECONDS, returnNullOnFail = true)
        public Integer work(String id, CountDownLatch entered, CountDownLatch release) throws InterruptedException {
            entered.countDown();
            release.await(2, TimeUnit.SECONDS);
            return counter.incrementAndGet();
        }

        @RedisLocked(key = "'lock:test-wait:' + #p0", waitTime = 500, leaseTime = 5, timeUnit = TimeUnit.MILLISECONDS, returnNullOnFail = true)
        public Integer workWithWait(String id, CountDownLatch entered, CountDownLatch release) throws InterruptedException {
            entered.countDown();
            release.await(2, TimeUnit.SECONDS);
            return counter.incrementAndGet();
        }

        @RedisLocked(key = "'lock:test-throw:' + #p0", waitTime = 0, leaseTime = 5, timeUnit = TimeUnit.SECONDS, returnNullOnFail = false, failCode = 423, failMessage = "lock-conflict")
        public Integer workThrowWhenLocked(String id, CountDownLatch entered, CountDownLatch release) throws InterruptedException {
            entered.countDown();
            release.await(2, TimeUnit.SECONDS);
            return counter.incrementAndGet();
        }
    }

    @Autowired
    private TestService testService;

    @Test
    void returnsNullWhenLockNotAcquired() throws Exception {
        int before = testService.getCounter();
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

        assertEquals(before + 1, firstResult.get());
    }

    @Test
    void secondCallWaitsAndThenSucceedsWhenWaitTimeIsSet() throws Exception {
        int before = testService.getCounter();
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        var executor = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> first = executor.submit(() -> testService.workWithWait("1", entered, release));
            entered.await(2, TimeUnit.SECONDS);

            Future<Integer> second = executor.submit(() -> testService.workWithWait("1", new CountDownLatch(0), new CountDownLatch(0)));

            release.countDown();

            assertEquals(before + 1, first.get(2, TimeUnit.SECONDS));
            assertEquals(before + 2, second.get(2, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void throwsBusinessExceptionWhenConfiguredToThrowOnFail() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        Thread t1 = new Thread(() -> {
            try {
                testService.workThrowWhenLocked("1", entered, release);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        });

        t1.start();
        entered.await(2, TimeUnit.SECONDS);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> testService.workThrowWhenLocked("1", new CountDownLatch(0), new CountDownLatch(0)));

        RuntimeException unwrapped = unwrap(ex);
        assertEquals(com.example.personal_loan.exception.BusinessException.class, unwrapped.getClass());
        assertEquals("lock-conflict", unwrapped.getMessage());

        release.countDown();
        t1.join(2000);
    }

    private static RuntimeException unwrap(RuntimeException ex) {
        if (ex instanceof UndeclaredThrowableException ute && ute.getUndeclaredThrowable() instanceof RuntimeException rte) {
            return rte;
        }
        if (ex.getCause() instanceof RuntimeException rte) {
            return rte;
        }
        return ex;
    }
}
