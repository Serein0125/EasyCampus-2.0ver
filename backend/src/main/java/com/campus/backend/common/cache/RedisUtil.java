package com.campus.backend.common.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "true", matchIfMissing = false)
public class RedisUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(RedisUtil.class);
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    // ============================== 基础操作 ==============================
    
    public boolean set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            return true;
        } catch (Exception e) {
            logger.error("Redis设置键值失败: key={}", key, e);
            return false;
        }
    }
    
    public boolean set(String key, Object value, long time, TimeUnit timeUnit) {
        try {
            if (time > 0) {
                redisTemplate.opsForValue().set(key, value, time, timeUnit);
            } else {
                set(key, value);
            }
            return true;
        } catch (Exception e) {
            logger.error("Redis设置键值失败: key={}, time={}", key, time, e);
            return false;
        }
    }
    
    public Object get(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            logger.error("Redis获取键值失败: key={}", key, e);
            return null;
        }
    }
    
    public <T> T get(String key, Class<T> clazz) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value != null && clazz.isInstance(value)) {
                return clazz.cast(value);
            }
            return null;
        } catch (Exception e) {
            logger.error("Redis获取键值失败: key={}", key, e);
            return null;
        }
    }
    
    public boolean delete(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.delete(key));
        } catch (Exception e) {
            logger.error("Redis删除键失败: key={}", key, e);
            return false;
        }
    }
    
    public boolean delete(List<String> keys) {
        try {
            Long count = redisTemplate.delete(keys);
            return count != null && count > 0;
        } catch (Exception e) {
            logger.error("Redis批量删除键失败: keys={}", keys, e);
            return false;
        }
    }
    
    public boolean hasKey(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            logger.error("Redis检查键是否存在失败: key={}", key, e);
            return false;
        }
    }
    
    public boolean expire(String key, long time, TimeUnit timeUnit) {
        try {
            if (time > 0) {
                return Boolean.TRUE.equals(redisTemplate.expire(key, time, timeUnit));
            }
            return false;
        } catch (Exception e) {
            logger.error("Redis设置过期时间失败: key={}, time={}", key, time, e);
            return false;
        }
    }
    
    public Long getExpire(String key, TimeUnit timeUnit) {
        try {
            return redisTemplate.getExpire(key, timeUnit);
        } catch (Exception e) {
            logger.error("Redis获取过期时间失败: key={}", key, e);
            return null;
        }
    }
    
    // ============================== Hash操作 ==============================
    
    public boolean hSet(String key, String hashKey, Object value) {
        try {
            redisTemplate.opsForHash().put(key, hashKey, value);
            return true;
        } catch (Exception e) {
            logger.error("Redis设置Hash失败: key={}, hashKey={}", key, hashKey, e);
            return false;
        }
    }
    
    public Object hGet(String key, String hashKey) {
        try {
            return redisTemplate.opsForHash().get(key, hashKey);
        } catch (Exception e) {
            logger.error("Redis获取Hash失败: key={}, hashKey={}", key, hashKey, e);
            return null;
        }
    }
    
    public Map<Object, Object> hGetAll(String key) {
        try {
            return redisTemplate.opsForHash().entries(key);
        } catch (Exception e) {
            logger.error("Redis获取所有Hash失败: key={}", key, e);
            return null;
        }
    }
    
    public boolean hDelete(String key, Object... hashKeys) {
        try {
            redisTemplate.opsForHash().delete(key, hashKeys);
            return true;
        } catch (Exception e) {
            logger.error("Redis删除Hash失败: key={}, hashKeys={}", key, hashKeys, e);
            return false;
        }
    }
    
    // ============================== Set操作 ==============================
    
    public boolean sAdd(String key, Object... values) {
        try {
            redisTemplate.opsForSet().add(key, values);
            return true;
        } catch (Exception e) {
            logger.error("Redis添加Set失败: key={}", key, e);
            return false;
        }
    }
    
    public Set<Object> sMembers(String key) {
        try {
            return redisTemplate.opsForSet().members(key);
        } catch (Exception e) {
            logger.error("Redis获取Set成员失败: key={}", key, e);
            return null;
        }
    }
    
    public boolean sIsMember(String key, Object value) {
        try {
            return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(key, value));
        } catch (Exception e) {
            logger.error("Redis检查Set成员失败: key={}", key, e);
            return false;
        }
    }
    
    // ============================== List操作 ==============================
    
    public boolean lPush(String key, Object value) {
        try {
            redisTemplate.opsForList().leftPush(key, value);
            return true;
        } catch (Exception e) {
            logger.error("Redis左推List失败: key={}", key, e);
            return false;
        }
    }
    
    public boolean rPush(String key, Object value) {
        try {
            redisTemplate.opsForList().rightPush(key, value);
            return true;
        } catch (Exception e) {
            logger.error("Redis右推List失败: key={}", key, e);
            return false;
        }
    }
    
    public Object lPop(String key) {
        try {
            return redisTemplate.opsForList().leftPop(key);
        } catch (Exception e) {
            logger.error("Redis左弹List失败: key={}", key, e);
            return null;
        }
    }
    
    public Object rPop(String key) {
        try {
            return redisTemplate.opsForList().rightPop(key);
        } catch (Exception e) {
            logger.error("Redis右弹List失败: key={}", key, e);
            return null;
        }
    }
    
    // ============================== 分布式锁 ==============================
    
    public boolean tryLock(String lockKey, String requestId, long expireTime, TimeUnit timeUnit) {
        try {
            String script = "if redis.call('setnx', KEYS[1], ARGV[1]) == 1 then " +
                           "redis.call('pexpire', KEYS[1], ARGV[2]) " +
                           "return 1 " +
                           "else " +
                           "return 0 " +
                           "end";
            
            DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
            redisScript.setScriptText(script);
            redisScript.setResultType(Long.class);
            
            Long result = redisTemplate.execute(
                    redisScript,
                    Collections.singletonList(lockKey),
                    requestId,
                    String.valueOf(timeUnit.toMillis(expireTime))
            );
            
            return result != null && result == 1;
        } catch (Exception e) {
            logger.error("Redis获取锁失败: lockKey={}", lockKey, e);
            return false;
        }
    }
    
    public boolean releaseLock(String lockKey, String requestId) {
        try {
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                           "return redis.call('del', KEYS[1]) " +
                           "else " +
                           "return 0 " +
                           "end";
            
            DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
            redisScript.setScriptText(script);
            redisScript.setResultType(Long.class);
            
            Long result = redisTemplate.execute(
                    redisScript,
                    Collections.singletonList(lockKey),
                    requestId
            );
            
            return result != null && result == 1;
        } catch (Exception e) {
            logger.error("Redis释放锁失败: lockKey={}", lockKey, e);
            return false;
        }
    }
    
    // ============================== 批量操作 ==============================
    
    public void multiSet(Map<String, Object> map) {
        try {
            redisTemplate.opsForValue().multiSet(map);
        } catch (Exception e) {
            logger.error("Redis批量设置失败", e);
        }
    }
    
    public List<Object> multiGet(List<String> keys) {
        try {
            return redisTemplate.opsForValue().multiGet(keys);
        } catch (Exception e) {
            logger.error("Redis批量获取失败", e);
            return null;
        }
    }
    
    // ============================== 统计操作 ==============================
    
    public Long increment(String key, long delta) {
        try {
            return redisTemplate.opsForValue().increment(key, delta);
        } catch (Exception e) {
            logger.error("Redis自增失败: key={}, delta={}", key, delta, e);
            return null;
        }
    }
    
    public Long decrement(String key, long delta) {
        try {
            return redisTemplate.opsForValue().decrement(key, delta);
        } catch (Exception e) {
            logger.error("Redis自减失败: key={}, delta={}", key, delta, e);
            return null;
        }
    }
}