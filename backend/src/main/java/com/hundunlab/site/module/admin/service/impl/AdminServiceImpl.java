package com.hundunlab.site.module.admin.service.impl;

//import com.hundunlab.site.common.constant.ImportantConstant;
import com.hundunlab.site.common.context.BaseContext;
import com.hundunlab.site.common.exception.MyException.*;
import com.hundunlab.site.common.util.PasswordUtil;
import com.hundunlab.site.common.util.RandomCodeUtil;
import com.hundunlab.site.module.user.cache.UserRedisService;
import com.hundunlab.site.module.user.mapper.AdminMapper;
import com.hundunlab.site.module.user.mapper.UserMapper;
import com.hundunlab.site.module.user.pojo.DTO.GetLogsDTO;
import com.hundunlab.site.module.user.pojo.DTO.UpdateUserDTO;
import com.hundunlab.site.module.user.pojo.Entity.OperateLog;
import com.hundunlab.site.module.user.pojo.VO.PageVO;
import com.hundunlab.site.module.admin.pojo.DTO.AddAdminDTO;
import com.hundunlab.site.module.admin.pojo.DTO.GetUsersDTO;
import com.hundunlab.site.module.user.pojo.Entity.User;
import com.hundunlab.site.module.admin.service.AdminService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.hundunlab.site.common.constant.redis.RedisConstant.*;
import static com.hundunlab.site.common.constant.status.UserStatusConstant.*;

@Slf4j
@Service
public class AdminServiceImpl implements AdminService {

    private static final String PENDING_ADMIN_PREFIX = "管理员待注册";

    @Autowired
    private AdminMapper am;
    @Autowired
    private UserMapper um;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRedisService userRedisService;


    @Override
    public Integer getUserNumber() {
        return am.getUserNumber();
    }



    @Transactional
    @Override
    public Integer changeStatus(String username) {
        //1检验用户是否存在
        User operateUser = um.getUserByUserName( username);
        if(operateUser == null)
        {
            throw new UserNotFoundException("用户不存在");
        }
        Integer status = operateUser.getStatus();
        //2 显式校验角色层级，禁止越级操作
        User currentUser = getCurrentActor();
        if (!canManage(currentUser.getStatus(), status)) {
            throw new InsufficientPermissionsException("权限不够的...(就算你是鹈鹕也不能吃同事/w\\)");
        }
        //3执行修改
        User changedUser = changeStatus(operateUser,status);
        um.updateUserByUserName(changedUser);
        log.info("用户状态已修改");
        log.info("用户状态:{}", operateUser.getStatus());

        //4 状态变化后立即吊销该用户token并清除其数据缓存，防止被禁用用户持旧token继续访问
        userRedisService.clearUserToken(operateUser.getId());
        userRedisService.clearUserdata(operateUser.getId());

        //清除用户查询缓存
        userRedisService.clearUsersCache();

        return operateUser.getStatus();
    }
    //辅助方法:使用定义好的常量修改用户的状态
    private User changeStatus(User operateUser, Integer status) {
        switch (status){
            case STATUS_USER:{
                operateUser.setStatus(STATUS_USER_BAN);
                break;
            }
            case STATUS_USER_BAN:{
                operateUser.setStatus(STATUS_USER);
                break;
            }
            case STATUS_ADMIN_BAN:{
                operateUser.setStatus(STATUS_ADMIN);
                break;
            }
            case STATUS_ADMIN:{
                operateUser.setStatus(STATUS_ADMIN_BAN);
                break;
            }
            case STATUS_SUPER_ADMIN:{
                throw new InsufficientPermissionsException("你干嘛 不许你这样对芜柒 sama");
            }
            default:{
                log.warn("用户状态未定义 系统已经将状态设为默认态");
                operateUser.setStatus(STATUS_USER_BAN);
                break;
            }
        }
        return operateUser;
    }

    @Override
    @Transactional
    public void addAdmin(AddAdminDTO dto) {
        //1 检查是否已有用户（统一报错文案，避免接口被用于枚举已注册用户名）
        User existUser = um.getUserByUserName(dto.getUsername());
        if(existUser != null){
            throw new UserExistedException("注册信息不可用，请更换后重试");
        }
        //2 检查邀请码
        String importantPassword = am.selectInvitePassword(dto.getImportantPassword());
        log.info("检索到的邀请码#{}",importantPassword);
        if(importantPassword==null){
            throw new LoginException("你貌似没有被邀请 你干嘛啊??");
        }
        log.info("重要密码已校验通过");
        //3 构造User并插入用户表
        User u = new User();
        u.setUsername(dto.getUsername());
        u.setPasswordHash(PasswordUtil.addMi(dto.getPassword()));
        u.setStatus(STATUS_ADMIN);
        u.setEmail(dto.getEmail());
        u.setPhone(dto.getPhone());
        u.setAvatarUrl(dto.getAvatarUrl());
        u.setLastLoginAt(LocalDateTime.now());
        um.insertUser(u);
        //4 移除占位管理员（优先匹配新格式，兼容历史两字符后缀格式）
        User tempUser = um.getUserByUserName(PENDING_ADMIN_PREFIX + importantPassword);
        if (tempUser == null) {
            tempUser = um.getUserByUserName(PENDING_ADMIN_PREFIX
                    + importantPassword.substring(0, Math.min(2, importantPassword.length())));
        }
        if(tempUser == null){
            log.warn("没有找到占位管理员,疑似出现命名碰撞,请手动处理");
            return;
        }
        um.deleteUserById(tempUser.getId());

        //Last 清除用户查询缓存
        userRedisService.clearUsersCache();
        userRedisService.clearUserdata(tempUser.getId());



    }

    @Override
    public PageVO<User> getUsers(GetUsersDTO dto) {
        log.info("开始分页查询用户");
        log.info("分页参数:{}",dto);
        
        //生成缓存 key
        String cacheKey = generateUsersCacheKey(dto);
        log.info("缓存 key:{}", cacheKey);
        
        //1. 先尝试从 Redis 获取缓存
        Object cachedData = redisTemplate.opsForValue().get(cacheKey);
        if (cachedData != null) {
            log.info("命中缓存，从 Redis 获取数据");
            try {
                String json = (String) cachedData;
                if (json.isEmpty()) {
                    return new PageVO<>(0L, null);
                }
                return objectMapper.readValue(json, 
                    objectMapper.getTypeFactory().constructParametricType(PageVO.class, User.class));
            } catch (JsonProcessingException e) {
                log.error("反序列化缓存数据失败", e);
            }
        }
        
        log.info("缓存未命中，查询数据库");
        
        //2. 缓存未命中，查询数据库
        PageHelper.startPage(dto.getPage(),dto.getSize());
        List<User> userList = am.getUsers(dto.getName(),dto.getStatus(),dto.getBeginTime(),dto.getEndTime(),dto.getLoginDays());
        if (userList.isEmpty()){
            //解决穿透问题 如果是空 那么直接在redis中设置一个空值
            redisTemplate.opsForValue().set(cacheKey, "", NULL_EXPIRE_TIME, TimeUnit.MINUTES);
            throw new UserNotFoundException("没有查到结果哦");
        }
        Page<User> p = (Page<User>)userList;
        PageVO<User> pageVO = new PageVO<>(p.getTotal(),p.getResult());

        //3. 将结果存入 Redis 缓存
        try {
            String json = objectMapper.writeValueAsString(pageVO);
            redisTemplate.opsForValue().set(cacheKey, json, USERS_CACHE_EXPIRE_TIME, TimeUnit.MINUTES);
            log.info("已将查询结果存入 Redis 缓存，过期时间：{}分钟", USERS_CACHE_EXPIRE_TIME);
        } catch (JsonProcessingException e) {
            log.error("序列化缓存数据失败", e);
        }
        
        return pageVO;
    }
    
    /**
     * 生成用户查询缓存 key
     * 根据查询条件生成唯一的 key
     */
    private String generateUsersCacheKey(GetUsersDTO dto) {
        StringBuilder keyBuilder = new StringBuilder(USERS_CACHE_PATH);
        keyBuilder.append("page_").append(dto.getPage()).append("_");
        keyBuilder.append("size_").append(dto.getSize()).append("_");
        
        if (dto.getName() != null && !dto.getName().isEmpty()) {
            keyBuilder.append("name_").append(dto.getName()).append("_");
        }
        
        if (dto.getStatus() != null) {
            keyBuilder.append("status_").append(dto.getStatus()).append("_");
        }
        
        if (dto.getBeginTime() != null) {
            keyBuilder.append("begin_").append(dto.getBeginTime().toString()).append("_");
        }
        
        if (dto.getEndTime() != null) {
            keyBuilder.append("end_").append(dto.getEndTime().toString()).append("_");
        }
        
        if (dto.getLoginDays() != null) {
            keyBuilder.append("logindays_").append(dto.getLoginDays()).append("_");
        }
        
        return keyBuilder.toString();
    }

    @Override
//todo:为分页查询日志添加缓存
    public PageVO<OperateLog> getLogs(GetLogsDTO dto) {
        log.info("{}",dto);
        PageHelper.startPage(dto.getPage(),dto.getSize());
        List<OperateLog> l = am.getLogs(dto);
        Page<OperateLog> p = (Page<OperateLog>) l;

        return new PageVO<>(p.getTotal(),p.getResult());
    }

    @Override
    public void deleteUserById(Long id) {
        log.info("{}",id);
        User u =  um.selectUserById(id);
        if(u == null){
            throw new UserNotFoundException("用户不存在");
        }
        User currentUser = getCurrentActor();
        if (!canManage(currentUser.getStatus(), u.getStatus())) {
            throw new InsufficientPermissionsException("不许你这样对管理员 sama");
        }
        um.deleteUserById(id);

        //删除后立即吊销token与数据缓存，防止已删除用户持旧token继续访问
        userRedisService.clearUserToken(id);
        userRedisService.clearUserdata(id);

        //清除用户查询缓存
        userRedisService.clearUsersCache();
    }

    @Override
    public void updateUser(UpdateUserDTO dto) {
        // 只能修改层级更低的用户，禁止普通管理员修改管理员/超级管理员
        User target = um.selectUserById(dto.getOldId());
        if (target == null) {
            throw new UserNotFoundException("用户不存在");
        }
        User currentUser = getCurrentActor();
        if (!canManage(currentUser.getStatus(), target.getStatus())) {
            throw new InsufficientPermissionsException("权限不足，不能修改该用户");
        }
        if(dto.getPassword() != null && !dto.getPassword().isBlank()){
            String password_hash = PasswordUtil.addMi(dto.getPassword());
            dto.setPassword(password_hash);
        }
        dto.setUpdatedAt(LocalDateTime.now());
        try{
            am.updateUserByIdForAdmin(dto);
        }catch (Exception e){
            throw new SqlOperationException("你是个大笨蛋");
        }
        //删除缓存
        userRedisService.clearUsersCache();
        userRedisService.clearUserdata(dto.getOldId());
    }

    @Override
    public User getUserInfoById(Long id) {
        User u =  um.selectUserById(id);
        if(u == null || u.getStatus() == null )
        {
            throw new UserNotFoundException("用户不存在");
        }
        return u;
    }

    /**
     * 清除所有用户查询缓存
     * 使用 pattern 删除匹配的所有 key
     */


    @Override
    public String makeInviteCode() {
        User u =new User();
        u.setPasswordHash(PasswordUtil.addMi(java.util.UUID.randomUUID().toString()));
        u.setStatus(STATUS_ADMIN_BAN);
        // 生成不重复的邀请码，占位用户名使用完整邀请码避免前缀碰撞
        String code;
        int attempts = 0;
        do {
            code = RandomCodeUtil.makeComplexCode(10);
            attempts++;
        } while (am.selectInvitePassword(code) != null && attempts < 10);
        if (am.selectInvitePassword(code) != null) {
            throw new IllegalStateException("邀请码生成失败，请重试");
        }
        u.setInviteCode(code);
        u.setUsername(PENDING_ADMIN_PREFIX + code);
        um.insertUser(u);
        //todoEd: 删除管理端用户查询的分页缓存
        userRedisService.clearUsersCache();
        return u.getInviteCode();
    }
//    @Override
//    public void deleteUserByUserName(String username) {
//        um.deleteUserByUserName(username);

//    }



    /**
     * 当前操作人（从数据库实时读取，避免缓存中的旧状态）
     */
    private User getCurrentActor() {
        String currentUsername = BaseContext.getCurrentUsername();
        if (currentUsername == null) {
            throw new InsufficientPermissionsException("未登录");
        }
        User currentUser = um.getUserByUserName(currentUsername);
        if (currentUser == null) {
            throw new InsufficientPermissionsException("当前用户不存在");
        }
        return currentUser;
    }

    /**
     * 角色层级：普通管理员只能管理普通用户，超级管理员可管理除超级管理员外的所有用户
     */
    private boolean canManage(Integer actorStatus, Integer targetStatus) {
        if (actorStatus == null || targetStatus == null) {
            return false;
        }
        if (targetStatus == STATUS_SUPER_ADMIN) {
            return false;
        }
        if (actorStatus == STATUS_SUPER_ADMIN) {
            return true;
        }
        if (actorStatus == STATUS_ADMIN) {
            return targetStatus == STATUS_USER || targetStatus == STATUS_USER_BAN;
        }
        return false;
    }

}
