package com.hundunlab.site.module.user.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import static com.hundunlab.site.common.constant.redis.RedisConstant.FEEDBACK_CACHE_PATH;

@Slf4j
@Component
public class FeedbackRedisService {
    private final RedisTemplate<String, Object> redisTemplate;

    public FeedbackRedisService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void clearFeedbacksCache() {
        log.info("清除反馈缓存...");
        redisTemplate.delete(FEEDBACK_CACHE_PATH);
    }
}
