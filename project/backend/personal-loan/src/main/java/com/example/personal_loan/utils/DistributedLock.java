package com.example.personal_loan.utils;

import java.util.concurrent.TimeUnit;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

@Component
public class DistributedLock {

    private final RedissonClient redissonClient;

    public DistributedLock(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * 获取分布式锁
     * @param key 锁的键
     * @param expireTime 过期时间（秒）
     * @return 是否获取成功
     */
    public boolean lock(String key, int expireTime) {
        // 1. 获取锁对象
        RLock lock = redissonClient.getLock(key);
        try {
            // 2. 尝试加锁
            // 参数说明：waitTime=0 (不等待，抢不到直接返回false)，leaseTime=expireTime (锁持有时间)
            // 注意：如果你希望利用看门狗机制（自动续期），可以将 leaseTime 设为 -1，或者不传这个参数使用 lock.lock()
            return lock.tryLock(0, expireTime, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // 如果线程在等待锁的过程中被中断，恢复中断状态
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public boolean lockWithWatchdog(String key) {
        RLock lock = redissonClient.getLock(key);
        try {
            // 不传时间参数，默认过期时间为 30秒
            // 看门狗会每隔 10秒 检查一次，如果线程还持有锁，就自动续期到 30秒
            lock.lock(); 
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 释放分布式锁
     * @param key 锁的键
     * @return 是否释放成功
     */
    public boolean unlock(String key) {
        RLock lock = redissonClient.getLock(key);
        if (!lock.isHeldByCurrentThread()) {
            return false;
        }

        try {
            lock.unlock();
            return true;
        } catch (IllegalMonitorStateException e) {
            return false;
        }
    }
}
