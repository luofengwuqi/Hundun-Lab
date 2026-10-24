package com.hundunlab.site.module.user.cache;

import com.hundunlab.site.module.user.pojo.Entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.hundunlab.site.common.constant.redis.RedisConstant.*;

@Slf4j
@Component
public class UserRedisService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public UserRedisService(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void clearUsersCache() {
        String pattern = USERS_CACHE_PATH + "*";
        log.info("清除用户查询缓存，pattern:{}", pattern);

        try {
            // 使用 SCAN 分批删除，避免 KEYS 阻塞 Redis
            ScanOptions options = ScanOptions.scanOptions().match(pattern).count(1000).build();
            int deleted = 0;
            List<String> batch = new ArrayList<>(500);
            try (Cursor<String> cursor = redisTemplate.scan(options)) {
                while (cursor.hasNext()) {
                    batch.add(cursor.next());
                    if (batch.size() >= 500) {
                        redisTemplate.delete(batch);
                        deleted += batch.size();
                        batch.clear();
                    }
                }
            }
            if (!batch.isEmpty()) {
                redisTemplate.delete(batch);
                deleted += batch.size();
            }
            log.info("已删除 {} 个用户查询缓存 key", deleted);
        } catch (Exception e) {
            log.error("清除用户查询缓存失败", e);
        }
    }

    public void clearUserToken(Long userId) {
        String key = TOKEN_PATH + userId;
        log.info("清除用户token缓存，key:{}", key);

        try {
            redisTemplate.delete(key);
            log.info("已删除用户查询缓存 key:{}", key);
        } catch (Exception e) {
            log.error("清除用户token失败", e);
        }
    }

    public void clearUserdata(Long userId) {
        String key = USER_DATA_PATH + userId;
        log.info("清除用户data缓存，key:{}", key);
        try {
            redisTemplate.delete(key);
            log.info("已删除用户查询缓存 key:{}", key);
        } catch (Exception e) {
            log.error("清除用户data失败", e);
        }
    }

    public void saveUserInRedis(User u) {
        try{
            redisTemplate.opsForValue().set(USER_DATA_PATH + u.getId(), u, TOKEN_EXPIRE_TIME, TimeUnit.MINUTES);
        }catch (Exception e){
            // 缓存写失败不影响主流程，Redis 故障时降级为直接查库
            log.warn("保存用户数据到redis失败: {}", e.getMessage());
        }
    }

    public User getUserInRedis(Long userId) {
        try{
            Object obj = redisTemplate.opsForValue().get(USER_DATA_PATH + userId);
            if (obj == null) {
                return null;
            }
            if (obj instanceof User) {
                return (User) obj;
            }
            return objectMapper.convertValue(obj, User.class);
        }catch (Exception e){
            // 缓存读失败时返回 null，由调用方回源数据库
            log.warn("从redis获取用户数据失败: {}", e.getMessage());
            return null;
        }
    }

}
