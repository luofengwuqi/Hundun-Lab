package com.hundunlab.site.framework.interception;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.util.Properties;

/**
 * SQL拦截器：捕获正在执行的SQL，存入ThreadLocal
 * 只记录带占位符的原始 SQL，避免密码、邮箱、手机号等敏感参数进入日志和审计表
 */
@Slf4j
@Intercepts({@Signature(
        type = StatementHandler.class,
        method = "prepare",
        args = {Connection.class, Integer.class}
)})
@Component
public class CurrentSqlInterceptor implements Interceptor {
    private static final ThreadLocal<String> CURRENT_SQL = new ThreadLocal<>();

    public static String getCurrentSql() {
        return CURRENT_SQL.get();
    }

    public static void clear() {
        CURRENT_SQL.remove();
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
        MetaObject metaObject = SystemMetaObject.forObject(statementHandler);

        BoundSql boundSql = (BoundSql) metaObject.getValue("delegate.boundSql");
        String originalSql = boundSql.getSql().trim().replaceAll("\\s+", " ");
        CURRENT_SQL.set(originalSql);

        try {
            return invocation.proceed();
        } finally {
            log.debug("SQL: {}", originalSql);
        }
    }

    @Override
    public Object plugin(Object target) {
        return target instanceof StatementHandler ? Plugin.wrap(target, this) : target;
    }

    @Override
    public void setProperties(Properties properties) {}
}
