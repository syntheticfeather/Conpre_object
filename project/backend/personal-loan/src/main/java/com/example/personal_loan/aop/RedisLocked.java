package com.example.personal_loan.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD) // 只能用于方法上
@Retention(RetentionPolicy.RUNTIME) // 运行时可见，AOP 可以在运行时动态代理
public @interface RedisLocked {

    // 锁的键，支持 SpEL 表达式
    String key();

    // 等待锁的超时时间，默认 0 表示不等待，直接返回 false
    long waitTime() default 0;

    // 锁的持有时间，默认 -1 表示使用看门狗机制（自动续期）
    long leaseTime() default -1;

    // 时间单位，默认秒
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    // 失败时返回的 HTTP 状态码，默认 423 表示锁冲突
    int failCode() default 423;

    // 失败时返回的错误信息，默认 "获取锁失败"
    String failMessage() default "获取锁失败";

    // 失败时是否返回 null，默认 false 表示抛出异常
    boolean returnNullOnFail() default false;
}
