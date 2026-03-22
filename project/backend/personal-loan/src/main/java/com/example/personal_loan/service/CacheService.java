package com.example.personal_loan.service;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.personal_loan.utils.BloomFilter;
import com.example.personal_loan.utils.RedisUtil;

@Service
public class CacheService {

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private BloomFilter bloomFilter;

    @Autowired
    private RedissonClient redissonClient;

    private final Random random = new Random();
    private final int BASE_EXPIRE_TIME = 3600; // 基础过期时间1小时
    private final int RANDOM_OFFSET = 300; // 随机偏移量0-300秒

    /**
     * 通用缓存查询模板
     * @param key 缓存Key
     * @param dbQuery 数据库查询函数 (Lambda)
     * @param expireSeconds 过期时间
     * @param <T> 返回类型
     * @return
     */
    public <T> T getOrLoad(String key, Supplier<T> dbQuery, int expireSeconds) {
        // 1. 布隆过滤器 (防穿透)
        if (!bloomFilter.mightContain(key)) {
            return null;
        }

        // 2. 查询缓存 (击穿防护的第一层)
        String cacheKey = "cache:" + key;
        T value = (T) redisUtil.get(cacheKey);
        if (value != null) {
            return value;
        }

        // 3. 缓存重建 (击穿防护的第二层 - 加锁)
        String lockKey = "lock:" + key;
        RLock lock = redissonClient.getLock(lockKey);
        
        try {
            // 尝试加锁，最多等待 3 秒，锁自动释放时间 10 秒
            if (lock.tryLock(3, 10, TimeUnit.SECONDS)) {
                // 双重检查 (Double Check)
                value = (T) redisUtil.get(cacheKey);
                if (value != null) {
                    return value;
                }

                // 执行数据库查询
                value = dbQuery.get();
                
                // 写回缓存
                if (value != null) {
                    redisUtil.set(cacheKey, value, expireSeconds, TimeUnit.SECONDS);
                }
                return value;
            } else {
                // 获取锁失败，说明有其他线程正在重建
                // 可以选择：1. 睡眠后重试 2. 直接查数据库 3. 返回空
                // 这里选择直接查数据库 (避免用户等待太久)
                return dbQuery.get(); 
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            // 获取锁被中断，直接查数据库
            return dbQuery.get();
        } finally {
            // 释放锁 (注意：这里要判断是否持有锁，防止释放别人的锁)
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 设置缓存，使用随机过期时间防止雪崩
     */
    public void set(String key, Object value) {
        int expireTime = BASE_EXPIRE_TIME + random.nextInt(RANDOM_OFFSET);
        redisUtil.set(key, value, expireTime, TimeUnit.SECONDS);
        // 添加到布隆过滤器
        bloomFilter.add(key);
    }

    /**
     * 删除缓存
     */
    public void delete(String key) {
        redisUtil.delete(key);
        // 注意：布隆过滤器无法删除元素，这是其特性
    }
}
