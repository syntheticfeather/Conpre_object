package com.example.personal_loan.utils;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DistributedLockTest {

    @Autowired
    private DistributedLock distributedLock;

    @Test
    void testLockAndUnlock() {
        String lockKey = "lock:test";
        
        // 测试获取锁
        boolean locked = distributedLock.lock(lockKey, 10);
        assertTrue(locked);
        
        // 测试释放锁
        boolean unlocked = distributedLock.unlock(lockKey);
        assertTrue(unlocked);
    }

    @Test
    void testLockWithTimeout() throws InterruptedException {
        String lockKey = "lock:timeout";
        
        // 第一次获取锁
        boolean locked1 = distributedLock.lock(lockKey, 2);
        assertTrue(locked1);
        
        // 第二次尝试获取锁（应该失败）
        boolean locked2 = distributedLock.lock(lockKey, 1);
        assertFalse(locked2);
        
        // 等待锁过期
        Thread.sleep(2500);
        
        // 再次尝试获取锁（应该成功）
        boolean locked3 = distributedLock.lock(lockKey, 2);
        assertTrue(locked3);
        
        distributedLock.unlock(lockKey);
    }
}
