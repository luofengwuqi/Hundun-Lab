package com.hundunlab.site.framework.interception;

import com.hundunlab.site.common.constant.status.UserStatusConstant;
import com.hundunlab.site.common.response.Result;
import com.hundunlab.site.common.context.BaseContext;
import com.hundunlab.site.common.util.JwtUtil;
import com.hundunlab.site.module.user.pojo.Entity.User;
import com.hundunlab.site.module.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

import static com.hundunlab.site.common.constant.redis.RedisConstant.TOKEN_PATH;

/**
 * 登录校验过滤器（修复核心问题：放行注册接口、规范响应、处理空指针、避免重复注册）
 */
@Slf4j
@Component // 仅用Spring管理，去掉@WebFilter避免重复注册
public class LoginFilter implements Filter {

    private final UserService userService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    //预先声明直接放行的请求
    private static final List<String> SKIP_URIS = Arrays.asList(
            "/user/login",                        // /user/login 下的所有接口
            "/user/register"                              // 注册接口
    );
    //对应 SKIP_URIS 中需要限制方法的路径（索引对应
    private static final List<String> SKIP_URIS_METHODS = Arrays.asList(
            "",                   // /user/login/*：允许所有方法
            "POST"               // 注册接口：仅允许 POST
    );

    public LoginFilter(RedisTemplate<String, Object> redisTemplate, UserService userService,
                       JwtUtil jwtUtil, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        try {
            doFilterInternal(request, response, chain);
        } finally {
            BaseContext.clearAll();
            log.debug("已清理线程上下文");
        }
    }

    private void doFilterInternal(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        log.debug("进入登录校验过滤器");
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        String uri = req.getRequestURI();
        String method = req.getMethod();
        log.debug("当前请求URI：{}", uri);
        log.debug("当前请求方法：{}", method);

        // ========== 1. 放行无需登录的接口（核心修复：加入注册接口） ==========
        // 仅放行浏览器 CORS 预检（带 Origin 的 OPTIONS），其余 OPTIONS 仍需鉴权
        if ("OPTIONS".equalsIgnoreCase(method) && req.getHeader("Origin") != null) {
            log.debug("CORS 预检请求放行");
            chain.doFilter(request, response);
            return;
        }
        if (isSkipUrl(uri,method) && !uri.equals("/user/login/logout")) {
            log.debug("检查到无需登录的请求（登录/注册），直接放行");
            BaseContext.setCurrentUsername("login");
            BaseContext.setCurrentId(0L);
            chain.doFilter(request, response);
            log.debug("无需登录的请求已放行");
            return;
        }

        // ========== 2. 校验 token ==========
        // 优先从 Authorization 头获取（标准格式：Bearer <token>）
        String authorization = req.getHeader("Authorization");
        String token;
                
        if (authorization != null && authorization.startsWith("Bearer ")) {
            token = authorization.substring(7); // 去掉 "Bearer " 前缀
        } else {
            // 兼容旧的 token 头方式
            token = req.getHeader("token");
        }

        // 2.1 token 为空 → 返回 401（规范响应流）
        if (token == null || token.isEmpty()) {
            log.debug("检查到 token 为空，返回 401");
            returnJson(res, 401, "未登录");
            return;
        }

        // 2.2 解析token → 异常则返回401
        Claims claims;
        try {
            claims = jwtUtil.solveToken(token);
        } catch (Exception e) {
            log.warn("解析token失败: {}", e.getMessage());
            returnJson(res, 401, "登录已过期");
            return;
        }
        //2.3 检验token在redis是否过期
        Integer id = claims.get("id", Integer.class);
        String redisToken = null;
        boolean redisAvailable = true;
        try{
            redisToken  = (String) redisTemplate.opsForValue().get(TOKEN_PATH + id);
        }catch (Exception e){
            // Redis 故障时降级为仅校验 JWT 签名，避免全站用户被锁在系统外
            // 代价：Redis 恢复前无法通过登出/服务端主动失效来吊销 token
            redisAvailable = false;
            log.error("Redis 不可用，降级为仅校验 JWT 签名: {}", e.getMessage());
        }
        if (redisAvailable && (redisToken == null || !redisToken.equals(token))) {
            log.debug("检查到token已过期，返回401");
            returnJson(res, 401, "登录已过期...");
            return;
        }

        // ========== 3. 解析token中的用户信息（处理空指针+封禁检查+防止普通用户越级访问） ==========
        log.debug("token解析成功，提取用户信息");
        String username = claims.get("username", String.class);

        // 空指针防护：id/username为空则返回401
        if (id == null || username == null) {
            log.debug("token中用户信息缺失");
            returnJson(res, 401, "登录信息异常");
            return;
        }

        // 所有需登录接口统一做用户存在性与封禁校验（selectUserById 带 redis 缓存，不会每请求打库）
        // 防止被ban/被删除的用户持旧token继续访问任意接口（token吊销的第二道防线）
        User user = userService.selectUserById(Long.valueOf(id));
        if (user == null) {
            log.warn("token中的用户已不存在:{}", username);
            returnJson(res, 401, "登录信息异常");
            return;
        }
        Integer status = user.getStatus();
        if (status == null || status < 0) {
            log.warn("用户{}已被禁用，拒绝访问", username);
            returnJson(res, 403, "账号已被禁用");
            return;
        }

        //防止普通用户越级访问（第一道防线，细粒度角色校验由 @RequireRole 切面完成）
        if (uri.startsWith("/admin")
                && status != UserStatusConstant.STATUS_ADMIN
                && status != UserStatusConstant.STATUS_SUPER_ADMIN) {
            log.warn("{}发生越级访问!!!",username);
            returnJson(res, 403, "越级访问!!!");
            return;
        }



        // 存入线程上下文
        BaseContext.setCurrentId(Long.valueOf(id));
        BaseContext.setCurrentUsername(username);
        log.debug("用户信息已存入线程上下文，放行请求");

        // ========== 4. 放行请求 ==========
        //更新redis中token过期时间（Redis 故障时跳过，不影响请求）
        if (redisAvailable) {
            try {
                redisTemplate.expire(TOKEN_PATH + id, 30, java.util.concurrent.TimeUnit.MINUTES);
            } catch (Exception e) {
                log.warn("刷新 token 过期时间失败: {}", e.getMessage());
            }
        }
        chain.doFilter(request, response);
        log.debug("通过Filter验证 请求已放行");
    }

    /**
     * 封装JSON响应（规范流处理，避免响应丢失）
     * @param res 响应对象
     * @param code 状态码
     * @param message 提示信息
     */
    private void returnJson(HttpServletResponse res, int code, String message) throws IOException {
        res.setStatus(code);
        res.setContentType("application/json;charset=utf-8");
        // 使用 Jackson 序列化，避免 message 中的引号破坏 JSON 结构
        String json = objectMapper.writeValueAsString(Result.fail(code, message));

        PrintWriter writer = res.getWriter();
        writer.write(json);
        writer.flush(); // 强制刷新流，确保响应发送
        writer.close(); // 关闭流，避免资源泄漏
    }

    @Override
    public void destroy() {}

    @Override
    public void init(FilterConfig config)  {
//        log.info("初始化登录过滤器");
    }

    //辅助方法 判断是否是已经声明可以跳过的请求
    private boolean isSkipUrl(String uri,String method) {
        for(int i = 0 ; i < SKIP_URIS.size(); i++)
        {
            String skipUri = SKIP_URIS.get(i);
            // 判断是否为子路径匹配（如 /user/login/email-password 匹配 /user/login）
            boolean isMatch = uri.equals(skipUri) || uri.startsWith(skipUri + "/");
            
            if(!isMatch)
            {//不匹配 下一条校验
                continue;
            }
            //通过uri校验 接着校验方法对不对(POST之类)
            String currentMethod = SKIP_URIS_METHODS.get(i);
            if(currentMethod.isEmpty())
            {//没有做额外限定
                return true;
            }
            else
            {//有额外的限定 比如注册是POST
                if(method.equals(currentMethod))
                {
                    return true;
                }
            }

        }
        return false;
    }
}