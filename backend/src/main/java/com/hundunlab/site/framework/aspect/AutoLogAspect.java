package com.hundunlab.site.framework.aspect;

import com.hundunlab.site.common.anno.AutoLog;
import com.hundunlab.site.common.context.BaseContext;
import com.hundunlab.site.common.util.IpUtil;
import com.hundunlab.site.framework.interception.CurrentSqlInterceptor;
import com.hundunlab.site.module.user.mapper.CommonMapper;
import com.hundunlab.site.module.user.pojo.Entity.OperateLog;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Objects;

@Slf4j
@Component
@Aspect
public class AutoLogAspect {

    @Autowired
    private CommonMapper cm;

    @Autowired
    private IpUtil ipUtil;

    @Pointcut("execution(* com.hundunlab.site.module.*.mapper.*.*(..)) && @annotation(com.hundunlab.site.common.anno.AutoLog)")
    public void autoLogPointCut() {
    }

    @Around("autoLogPointCut()")
    public Object autoLog(ProceedingJoinPoint pjp) throws Throwable {
        log.info("自动记录日志启动");
        OperateLog o = new OperateLog();
        o.setOperateUserId(BaseContext.getCurrentId());
        o.setOperateUserName(BaseContext.getCurrentUsername());
        o.setOperateAt(LocalDateTime.now());

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (Objects.nonNull(attributes)) {
            HttpServletRequest request = attributes.getRequest();
            o.setIpAddress(ipUtil.getClientIp(request));
        }

        MethodSignature signature = (MethodSignature) pjp.getSignature();
        AutoLog annotation = signature.getMethod().getAnnotation(AutoLog.class);
        if (Objects.nonNull(annotation)) {
            o.setOperateType(annotation.operateType());
            o.setModuleName(annotation.moduleName());
            o.setOperateFunctionName(pjp.getSignature().getName());
        }

        Long begin = System.currentTimeMillis();
        try {
            Object result = pjp.proceed();
            o.setOperateStatus(1);
            return result;
        } catch (Throwable e) {
            // 原异常不包装、不吞掉，保留真实堆栈
            o.setOperateStatus(0);
            throw e;
        } finally {
            Long end = System.currentTimeMillis();
            o.setOperateTime(end - begin + "ms");
            o.setExecutedSql(CurrentSqlInterceptor.getCurrentSql());
            // 写日志失败不能影响原业务异常
            try {
                cm.addLog(o);
                log.info("已写入日志{}", o);
            } catch (Exception logException) {
                log.error("写入操作日志失败", logException);
            } finally {
                CurrentSqlInterceptor.clear();
            }
        }
    }
}
