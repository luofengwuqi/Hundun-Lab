package com.hundunlab.site.common.context;

/**
 * 当前请求的认证状态。
 * 匿名访问是合法状态，不再用 id=0 / username="login" 之类的假身份冒充。
 */
public enum AuthState {
    /** 未携带令牌：匿名的合法访问 */
    ANONYMOUS,
    /** 令牌有效：已登录 */
    AUTHENTICATED,
    /** 携带了令牌但无效（签名错误 / 过期 / 已被吊销 / 用户不存在或被禁用） */
    INVALID
}
