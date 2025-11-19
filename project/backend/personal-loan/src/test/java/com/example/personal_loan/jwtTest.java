package com.example.personal_loan;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.personal_loan.entity.User;
import com.example.personal_loan.service.UserService;
import com.example.personal_loan.utils.JwtUtil;
import com.example.personal_loan.utils.RedisUtil;
import static com.example.personal_loan.utils.RedisUtil.JWT_REFRESH_CACHE_TOKEN_PREFIX_STRING;

import lombok.extern.slf4j.Slf4j;

@SpringBootTest
@Slf4j
public class jwtTest {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private UserService userService;

    /*
     * 对RefreahToken和AccessToken进行测试
     */
    @Test
    public void testJwt() {
        String userId = "1";
        long expirationTime = 5 * 1000;
        String refreshToken = jwtUtil.generateTestRefreshToken(userId, expirationTime);
        log.info("refreshToken: " + refreshToken);
        redisUtil.set(JWT_REFRESH_CACHE_TOKEN_PREFIX_STRING + userId, refreshToken, 5, TimeUnit.SECONDS);
        /*
         * 第一次用refreshtoken更新accesstoken
         */
        refreshToken = (String) redisUtil.get(JWT_REFRESH_CACHE_TOKEN_PREFIX_STRING + userId);
        if (refreshToken.isEmpty() || !jwtUtil.validateToken(refreshToken)) {
            log.info("refreshToken is invalid");
            return;
        }
        log.info("success refreshToken");
        User user = userService.getUserById(jwtUtil.getUserIdFromRefreshToken(refreshToken));
        String accessToken = jwtUtil.generateAccessToken(user.getPhone(), userId);
        log.info("accessToken: " + accessToken);
        /*
         * 第二次用accesstoken更新refreshtoken
         */
        if (refreshToken == null) {
            log.info("refreshToken is null");
            log.info("redis cache empty");
            return;
        }
        try {
            Thread.sleep(6 * 1000); // 测试refreshtoken失效时间
        } catch (InterruptedException e) {
            log.info("InterruptedException");
            e.printStackTrace();
        }
        Assertions.assertFalse(jwtUtil.validateToken(refreshToken));
        log.info("refreshToken is invalid");
    }
}
