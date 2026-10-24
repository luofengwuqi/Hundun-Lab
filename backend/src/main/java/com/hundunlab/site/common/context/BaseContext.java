package com.hundunlab.site.common.context;

import com.hundunlab.site.common.response.ErrorCode;

/**
 * 请求级线程上下文。
 * <p>
 * 语义约定（重要）：
 * <ul>
 *   <li>未登录 = 匿名，{@link #getCurrentId()} 返回 {@code null}，禁止再用 0 或 "login" 占位</li>
 *   <li>调用方必须先判空或调用 {@link #isAuthenticated()}，不要依赖「id 一定存在」</li>
 *   <li>必须在请求结束时 {@link #clearAll()}，否则线程池复用会串号</li>
 * </ul>
 */
public final class BaseContext {

    private static final ThreadLocal<Long> ID = new ThreadLocal<>();
    private static final ThreadLocal<String> USERNAME = new ThreadLocal<>();
    private static final ThreadLocal<Integer> STATUS = new ThreadLocal<>();
    private static final ThreadLocal<AuthState> AUTH_STATE = new ThreadLocal<>();
    private static final ThreadLocal<ErrorCode> AUTH_ERROR = new ThreadLocal<>();

    private BaseContext() {
    }

    public static void setCurrentId(Long id) {
        ID.set(id);
    }

    public static Long getCurrentId() {
        return ID.get();
    }

    public static void setCurrentUsername(String username) {
        USERNAME.set(username);
    }

    public static String getCurrentUsername() {
        return USERNAME.get();
    }

    /**
     * 用户状态（1-用户 / 2-管理员 / 3-超管 / 负数-禁用），由鉴权过滤器一次查出，避免拦截器重复打库
     */
    public static void setCurrentStatus(Integer status) {
        STATUS.set(status);
    }

    public static Integer getCurrentStatus() {
        return STATUS.get();
    }

    public static void setAuthState(AuthState state) {
        AUTH_STATE.set(state);
    }

    /**
     * 未显式设置时按匿名处理，避免上游漏设导致「误判为已登录」
     */
    public static AuthState getAuthState() {
        AuthState state = AUTH_STATE.get();
        return state == null ? AuthState.ANONYMOUS : state;
    }

    /**
     * 记录令牌校验失败的具体原因（过期 / 无效 / 账号禁用），供拦截器返回准确错误码
     */
    public static void setAuthError(ErrorCode errorCode) {
        AUTH_ERROR.set(errorCode);
    }

    public static ErrorCode getAuthError() {
        return AUTH_ERROR.get();
    }

    /**
     * 是否已登录。只有 id 与 username 都存在才算，防止只设了一半的中间态
     */
    public static boolean isAuthenticated() {
        return ID.get() != null && USERNAME.get() != null;
    }

    public static void clearAll() {
        ID.remove();
        USERNAME.remove();
        STATUS.remove();
        AUTH_STATE.remove();
        AUTH_ERROR.remove();
    }
}
