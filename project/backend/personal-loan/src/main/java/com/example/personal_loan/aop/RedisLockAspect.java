package com.example.personal_loan.aop;

import java.lang.reflect.Method;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.annotation.Order;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import com.example.personal_loan.exception.BusinessException;

@Aspect
@Component
@Order(1) // 确保切面优先级，如果有事务切面，锁切面通常要在事务切面之前
public class RedisLockAspect {

    private final RedissonClient redissonClient;

    // 表达式解析器，用于解析 SpEL 表达式
    private final ExpressionParser parser = new SpelExpressionParser();
    private final DefaultParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();

    public RedisLockAspect(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Around("@annotation(redisLocked)")
    public Object around(ProceedingJoinPoint joinPoint, RedisLocked redisLocked) throws Throwable {
        // 获取方法签名,ProceedingJoinPoint 代表了被拦截的方法本身
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // 解析锁的 Key
        String lockKey = evaluateLockKey(redisLocked.key(), method, joinPoint.getArgs());
        RLock lock = redissonClient.getLock(lockKey);

        // 获取锁
        boolean acquired = tryAcquire(lock, redisLocked);
        // 失败处理
        if (!acquired) {
            if (redisLocked.returnNullOnFail()) {
                return null; // 策略1：返回 null
            }
            // 策略2：抛出异常
            throw new BusinessException(redisLocked.failCode(), redisLocked.failMessage());
        }

        try {
            return joinPoint.proceed();  // 执行被拦截的方法，业务逻辑
        } finally { // 确保了无论业务成功还是失败，锁一定会被释放
            if (lock.isHeldByCurrentThread()) { // 检查锁是否由当前线程持有
                try {
                    lock.unlock(); // 释放锁
                } catch (IllegalMonitorStateException ignored) {
                   //  忽略异常，即使发生异常（例如锁已经过期被 Redis 自动删除），也不会影响主流程。
                }
            }
        }
    }

    // 尝试获取锁,根据注解的参数来判断是否等待锁和锁的持有时间
    private boolean tryAcquire(RLock lock, RedisLocked redisLocked) {
        long waitTime = redisLocked.waitTime();
        long leaseTime = redisLocked.leaseTime();
        var unit = redisLocked.timeUnit();

        try {
            if (leaseTime > 0) {
                // 有指定的持有时间，使用 tryLock 方法,锁会在指定时间后自动释放，不启用看门狗。
                return lock.tryLock(waitTime, leaseTime, unit);
            }
            if (waitTime > 0) {
                // 有指定的等待时间且 leaseTime <= 0，使用 tryLock 方法,用 Redisson 的默认 leaseTime（30秒）并启用看门狗。
                return lock.tryLock(waitTime, unit);
            }
            // 无指定时间，直接尝试获取锁
            return lock.tryLock();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private String evaluateLockKey(String expression, Method method, Object[] args) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        String[] parameterNames = nameDiscoverer.getParameterNames(method);
        if (parameterNames != null) {
            for (int i = 0; i < parameterNames.length && i < args.length; i++) {
                context.setVariable(parameterNames[i], args[i]);
            }
        }
        for (int i = 0; i < args.length; i++) {
            context.setVariable("p" + i, args[i]);
            context.setVariable("a" + i, args[i]);
        }
        Object value = parser.parseExpression(expression).getValue(context);
        if (value == null) {
            throw new BusinessException(400, "锁key为空");
        }
        return String.valueOf(value);
    }
}
