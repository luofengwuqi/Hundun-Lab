package com.hundunlab.site.common.context;

public class BaseContext {
    private static final ThreadLocal<Long> idThreadLocal = new ThreadLocal<>();
    private static final ThreadLocal<String> usernameThreadLocal = new ThreadLocal<>();

    // 现有的用户ID管理方法
    public static void setCurrentId(Long id) {
        idThreadLocal.set(id);
    }

    public static Long getCurrentId() {
        return idThreadLocal.get();
    }

    public static void removeCurrentId() {
        idThreadLocal.remove();
    }

    // 新增的用户名管理方法
    public static void setCurrentUsername(String username) {
        usernameThreadLocal.set(username);
    }

    public static String getCurrentUsername() {
        return usernameThreadLocal.get();
    }

    public static void removeCurrentUsername() {
        usernameThreadLocal.remove();
    }

    // 清理所有线程局部变量
    public static void clearAll() {
        idThreadLocal.remove();
        usernameThreadLocal.remove();
    }
}
