package com.example.personal_loan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.personal_loan.utils.RedisUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest
public class redisTest {

    @Autowired
    private RedisUtil redisUtil;

    @Test
    public void redisConnectTest() {
        log.info("redis connection test");
        redisUtil.set("test", "testValue");
        String value = (String) redisUtil.get("test");
        log.info("value: " + value);
        assertEquals("testValue", value);
    }
}
