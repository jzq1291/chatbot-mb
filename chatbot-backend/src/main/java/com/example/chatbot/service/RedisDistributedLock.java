package com.example.chatbot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Redis分布式锁实现（简化版）
 * 使用Redis的SET命令实现加锁，使用Lua脚本实现解锁
 * 
 * 优势：
 * 1. 代码更简洁，易于理解和维护
 * 2. 性能更好，减少了Lua脚本的执行开销
 * 3. 符合Redis官方推荐的分布式锁实现方式
 * 
 * 使用场景：
 * 1. 防止重复处理（如定时任务在多实例部署时，确保只有一个实例执行）
 * 2. 并发控制（如对热门知识的访问计数，需要保证原子性）
 * 3. 资源互斥（如对某些共享资源的访问控制）
 */
@Service
@RequiredArgsConstructor
public class RedisDistributedLock {
    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> unlockScript;
    
    private static final long DEFAULT_TIMEOUT = 30; // 默认锁超时时间（秒）
    
    /**
     * 尝试获取分布式锁（使用默认超时时间）
     * 
     * 使用场景示例：
     * 1. 定时任务调度：确保分布式环境下只有一个实例执行定时任务
     * 2. 热门知识更新：防止多个请求同时更新热门知识列表
     * 3. 缓存更新：防止缓存击穿，确保只有一个请求去数据库加载数据
     *
     * @param lockKey 锁的key，建议使用业务前缀，如：task:schedule:lock
     * @return 锁的值（用于解锁），如果获取锁失败返回null
     */
    public String tryLock(String lockKey) {
        return tryLock(lockKey, DEFAULT_TIMEOUT, TimeUnit.SECONDS);
    }
    
    /**
     * 尝试获取分布式锁（可指定超时时间）
     * 
     * 实现原理：
     * 1. 使用Redis的SET命令的NX和PX选项实现原子性加锁
     * 2. NX：只有当key不存在时才设置成功
     * 3. PX：设置过期时间（毫秒），防止死锁
     * 4. 使用UUID作为锁的值，确保锁的唯一性
     *
     * @param lockKey 锁的key
     * @param timeout 超时时间
     * @param unit 时间单位
     * @return 锁的值（用于解锁），如果获取锁失败返回null
     */
    public String tryLock(String lockKey, long timeout, TimeUnit unit) {
        // 生成唯一的锁值，用于标识锁的持有者
        String lockValue = UUID.randomUUID().toString();
        
        // 使用SET命令的NX和PX选项实现原子性加锁
        // NX：只有当key不存在时才设置成功
        // PX：设置过期时间（毫秒）
        Boolean result = redisTemplate.opsForValue()
            .setIfAbsent(lockKey, lockValue, timeout, unit);
        
        // 返回锁的值，如果获取成功返回lockValue，失败返回null
        return Boolean.TRUE.equals(result) ? lockValue : null;
    }
    
    /**
     * 释放分布式锁
     * 
     * 实现原理：
     * 1. 使用Lua脚本保证原子性
     * 2. 验证锁的值是否匹配，确保只能由锁的持有者释放锁
     * 3. 删除锁的key
     *
     * @param lockKey 锁的key
     * @param lockValue 锁的值（必须是获取锁时返回的值）
     * @return 是否成功释放锁
     */
    public boolean unlock(String lockKey, String lockValue) {
        // 使用Lua脚本执行解锁操作，保证原子性
        // KEYS[1] = lockKey
        // ARGV[1] = lockValue
        Long result = redisTemplate.execute(
            unlockScript,
            Collections.singletonList(lockKey),
            lockValue
        );
        return result != null && result == 1;
    }
} 