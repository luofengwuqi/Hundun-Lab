package com.hundunlab.site.common.constant.redis;

public class ArticleRedisConstant {
    //所有文章的缓存article:list
    public static final String ARTICLE_LIST = "article:list";
    //所有文章缓存的过期时间
    public static final Integer ARTICLE_LIST_EXPIRE = 30;
    //用户点赞文章缓存 article:good:user:${articleId}:${userId}
    public static final String ARTICLE_GOOD_USER = "article:good:user:";
    //文章点赞数缓存 article:good:count:${articleId}
    public static final String ARTICLE_GOOD_COUNT = "article:good:count:";
    //用户点踩文章缓存 article:bad:user:${articleId}:${userId}
    public static final String ARTICLE_BAD_USER = "article:bad:user:";
    //文章点踩数缓存 article:bad:count:${articleId}
    public static final String ARTICLE_BAD_COUNT = "article:bad:count:";

    /*
        点赞业务逻辑:
        用户点下了点赞按键 分为以下几种:
        1 用户既未点赞 也没有点踩
        (在redis中查询不到bad:user:${articleId}:${userId}以及article:good:user:${articleId}:${userId})
            1.1执行正常点赞逻辑:
                1.1.1使得article:good:count:${articleId}对应的value+1
                1.1.2创建article:good:user:${articleId}:${userId} value为1
        2 用户点过赞了
        (在redis中查询不到bad:user:${articleId}:${userId} 能查询到article:good:user:${articleId}:${userId})
            2.1执行取消点赞逻辑:
                2.1.1使得article:good:count:${articleId}对应的value-1
                2.1.2删除article:good:user:${articleId}:${userId}
        3 用户点过踩了
        (在redis中查询不到good:user:${articleId}:${userId} 能查询到article:bad:user:${articleId}:${userId})
            3.1执行取消点踩+执行点赞逻辑:
                3.1.1使得article:good:count:${articleId}对应的value+1
                3.1.2使得article:bad:count:${articleId}对应的value-1
                3.1.3删除article:bad:user:${articleId}:${userId}
                3.1.4创建article:good:user:${articleId}:${userId} value为1

   */
}
