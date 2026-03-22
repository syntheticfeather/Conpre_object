package com.example.personal_loan.utils;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class BloomFilter {

    private final RedisTemplate<String, Object> redisTemplate;
    private final String BLOOM_FILTER_KEY = "bloom:filter";
    private final int BIT_SIZE = 1024 * 1024 * 16; // 16MB
    private final List<HashFunction> hashFunctions;

    public BloomFilter(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.hashFunctions = new ArrayList<>();
        // 初始化多个哈希函数
        for (int i = 0; i < 3; i++) {
            this.hashFunctions.add(new HashFunction(BIT_SIZE, i));
        }
    }

    public void add(String value) {
        for (HashFunction hashFunction : hashFunctions) {
            long index = hashFunction.hash(value);
            redisTemplate.opsForValue().setBit(BLOOM_FILTER_KEY, index, true);
        }
    }

    public boolean mightContain(String value) {
        for (HashFunction hashFunction : hashFunctions) {
            long index = hashFunction.hash(value);
            Boolean result = redisTemplate.opsForValue().getBit(BLOOM_FILTER_KEY, index);
            if (result == null || !result) {
                return false;
            }
        }
        return true;
    }

    private static class HashFunction {
        private final int size;
        private final int seed;

        public HashFunction(int size, int seed) {
            this.size = size;
            this.seed = seed;
        }

        public long hash(String value) {
            long hash = 0;
            for (int i = 0; i < value.length(); i++) {
                hash = seed * hash + value.charAt(i);
            }
            return Math.abs(hash) % size;
        }
    }
}
