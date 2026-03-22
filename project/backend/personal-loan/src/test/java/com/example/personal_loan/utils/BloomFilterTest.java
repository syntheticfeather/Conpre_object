package com.example.personal_loan.utils;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class BloomFilterTest {

    @Autowired
    private BloomFilter bloomFilter;

    @Test
    void testAddAndMightContain() {
        // 测试添加元素后能检测到
        String key = "user:123";
        bloomFilter.add(key);
        assertTrue(bloomFilter.mightContain(key));
    }

    @Test
    void testNonExistentKey() {
        // 测试不存在的元素
        String nonExistentKey = "user:-999";
        assertFalse(bloomFilter.mightContain(nonExistentKey));
    }

    @Test
    void testMultipleElements() {
        // 测试多个元素
        String[] keys = {"user:1", "user:2", "user:3"};
        for (String key : keys) {
            bloomFilter.add(key);
        }
        
        for (String key : keys) {
            assertTrue(bloomFilter.mightContain(key));
        }
        
        assertFalse(bloomFilter.mightContain("user:999"));
    }
}
