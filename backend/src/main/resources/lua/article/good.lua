-- 参数接收
local good_user_key = KEYS[1]      -- "article:good:user:1001:12345" (包含userId)
local good_count_key = KEYS[2]     -- "article:good:count:1001"
local bad_user_key = KEYS[3]       -- "article:bad:user:1001:12345"
local bad_count_key = KEYS[4]      -- "article:bad:count:1001"

-- 执行业务
-- 1 检测用户是否已经点过赞
local is_good = redis.call('GET', good_user_key)

-- 2 如果已经点过赞,则取消点赞
if is_good then
    redis.call('DEL', good_user_key)
    redis.call('DECR', good_count_key)
    return 0
end

-- 3 检查是否点过踩,如果有则移除踩
local is_bad = redis.call('GET', bad_user_key)
if is_bad then
    redis.call('DEL', bad_user_key)
    redis.call('DECR', bad_count_key)
end

-- 4 添加点赞
redis.call('SET', good_user_key, 1)
redis.call('INCR', good_count_key)
return 1
