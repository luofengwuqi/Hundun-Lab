package com.hundunlab.site.module.auth.service.impl;

import com.hundunlab.site.common.context.BaseContext;
import com.hundunlab.site.common.exception.MyException.*;
import com.hundunlab.site.common.util.email.EmailCodeUtil;
import com.hundunlab.site.common.util.JwtUtil;
import com.hundunlab.site.common.util.PasswordUtil;
import com.hundunlab.site.module.user.cache.UserRedisService;
import com.hundunlab.site.module.user.mapper.UserMapper;
import com.hundunlab.site.module.auth.pojo.dto.LoginEmailCodeDTO;
import com.hundunlab.site.module.auth.pojo.dto.LoginEmailPassWordDTO;
import com.hundunlab.site.module.auth.pojo.dto.LoginPhonePasswordDTO;
import com.hundunlab.site.module.auth.pojo.dto.LoginUsernamePasswordRequestDTO;
import com.hundunlab.site.module.user.pojo.Entity.User;
import com.hundunlab.site.module.user.pojo.VO.LoginRequestVO;
import com.hundunlab.site.module.auth.service.LoginService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static com.hundunlab.site.common.constant.redis.RedisConstant.*;
import static com.hundunlab.site.common.constant.status.UserStatusConstant.STATUS_USER_BAN;

@Slf4j
@Service
public class LoginServiceImpl implements LoginService {

    private final UserRedisService userRedisService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final EmailCodeUtil emailCodeUtil;

    public LoginServiceImpl(UserMapper userMapper, RedisTemplate<String, Object> redisTemplate, StringRedisTemplate stringRedisTemplate, UserRedisService userRedisService, JwtUtil jwtUtil, EmailCodeUtil emailCodeUtil) {
        this.userMapper = userMapper;
        this.redisTemplate = redisTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
        this.userRedisService = userRedisService;
        this.jwtUtil = jwtUtil;
        this.emailCodeUtil = emailCodeUtil;
    }

    @Override
    public LoginRequestVO loginPhonePassword(LoginPhonePasswordDTO dto) {
        //检验存在性
        User u = userMapper.selectUserByPhone(dto.getPhone());
        if( u == null ){
            throw new UserNotFoundException("貌似没有这个手机号的...");
        }
        banCheck(u);
        //校验密码
        if(!PasswordUtil.check(dto.getPassword(),u.getPasswordHash())){
            throw new PasswordException("密码输错了...");
        }
        //创建token
        String token = jwtUtil.makeToken(Math.toIntExact(u.getId()),u.getUsername());
        saveTokenInRedis(token,u.getId());
        //4 存进缓存
        userRedisService.saveUserInRedis(u);
        //塞进vo
        return new LoginRequestVO(token,u);
    }

    @Override
    public LoginRequestVO loginUsernamePassword(LoginUsernamePasswordRequestDTO l) {

        User u = getValidUserByUserName(l.getUsername());
        banCheck(u);
        //校验密码
        String password = l.getPassword() ;
        if (!PasswordUtil.check(password,u.getPasswordHash())) {
            throw new LoginException("密码错误");
        }
        log.info("已经通过密码校验");
        //设置loginTime
        u.setLastLoginAt(LocalDateTime.now());
        userMapper.updateUserByUserName(u);
        //生成token 并存入redis(方便过期)
        String token = jwtUtil.makeToken(Math.toIntExact(u.getId()), u.getUsername());
        saveTokenInRedis(token,u.getId());
        //处理vo
        LoginRequestVO vo = new LoginRequestVO();
        vo.setToken(token);
        vo.setUser(u);
        //存进缓存
        userRedisService.saveUserInRedis(u);
        return vo;

    }

    @Override
    public LoginRequestVO loginEmailCode(LoginEmailCodeDTO dto) {
        //1查询是否存在
        User u = userMapper.selectUserByEmail(dto.getEmail());
        if(u==null){
            throw new UserNotFoundException("没找到这个邮箱...");
        }
        banCheck(u);
        //2校验验证码（恒定时间比较，防时序侧信道穷举验证码）
        String trueCode = stringRedisTemplate.opsForValue().get(EMAIL_CODE_PATH+dto.getEmail());
        if(trueCode == null){
            throw new RuntimeException("验证码似乎过期了...");
        }
        if (!java.security.MessageDigest.isEqual(
                dto.getCode().getBytes(java.nio.charset.StandardCharsets.UTF_8),
                trueCode.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
            throw new PasswordException("验证码不对的...");
        }
        //3通过 生成token 删除redis存的验证码 封装vo返回
        String token = jwtUtil.makeToken(Math.toIntExact(u.getId()),u.getUsername());
        saveTokenInRedis(token,u.getId());
//        删除redis里面的验证码
        stringRedisTemplate.delete(EMAIL_CODE_PATH+dto.getEmail());
        LoginRequestVO vo = new LoginRequestVO();
        vo.setToken(token);
        vo.setUser(u);
        //4 存进缓存
        userRedisService.saveUserInRedis(u);
        return vo;
    }

    @Override
    public void sendEmailCode(String email) {
        //1校验邮箱是否存在
        User u = userMapper.selectUserByEmail(email);
        if(u==null){
            throw new UserNotFoundException("邮箱不存在...");
        }
        //2生成验证码并把他塞进redis
        String code = emailCodeUtil.generateCode();
        //3发送验证码
        boolean result =  emailCodeUtil.sendEmailCode(email,code);
        if(!result){
            throw new SendCodeException("验证码发送失败了QAQ(疑似并不是程序的问题...)");
        }
        //4执行无误 塞进redis
        stringRedisTemplate.opsForValue().set(EMAIL_CODE_PATH+email,code,EMAIL_CODE_EXPIRE_TIME,TimeUnit.MINUTES);

    }

    @Override
    public LoginRequestVO loginEmailPassword(LoginEmailPassWordDTO dto) {
        //1校验邮箱是否存在
        User u = userMapper.selectUserByEmail(dto.getEmail());
        if(u==null){
            throw new UserNotFoundException("邮箱不存在...");
        }
        banCheck(u);
        //2校验密码
        if(!PasswordUtil.check(dto.getPassword(),u.getPasswordHash())){
            throw new PasswordException("密码输错了...");
        }
        //3通过 封装vo返回
        String token = jwtUtil.makeToken(Math.toIntExact(u.getId()),u.getUsername());
        saveTokenInRedis(token,u.getId());
        LoginRequestVO vo = new LoginRequestVO();
        vo.setToken(token);
        vo.setUser(u);
        //4 存进缓存
        userRedisService.saveUserInRedis(u);
        //5 返回
        return vo;
    }

    @Override
    public void logout() {
        Long currentId = BaseContext.getCurrentId();
        userRedisService.clearUserToken(currentId);
        userRedisService.clearUserdata(currentId);
    }

    //共用方法：在 redis 中存储 token
    public void saveTokenInRedis(String token,Long userId)
    {
        //安全：完整token禁止进日志，防止日志泄露导致会话冒用
        log.info("开始在redis存储token, userId:{}", userId);
        redisTemplate.opsForValue().set(TOKEN_PATH+userId,token,TOKEN_EXPIRE_TIME, TimeUnit.MINUTES);
    }

    //辅助方法 校验用户存在性userService携带过来的...
    public User getValidUserByUserName(String username)
    {
        log.debug("单个用户校验存在性开始");
        User u = userMapper.getUserByUserName(username);
        if(u==null)
        {
            throw new UserNotFoundException("用户不存在");
        }
        log.debug("已经查询到当前使用而存在的用户信息:{}",u);
        return u;
    }


    public void banCheck(User user){
        log.info("开始校验用户是否被禁用");
        int status = user.getStatus();
        log.info("用户状态为:{}",status);

        if(status <= STATUS_USER_BAN)
            throw new LoginException("你的账户被ban了(#`Д´)ﾉ!(tell me why)");

        log.info("校验通过");
    }


}

