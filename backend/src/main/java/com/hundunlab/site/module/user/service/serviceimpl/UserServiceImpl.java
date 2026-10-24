package com.hundunlab.site.module.user.service.serviceimpl;

import com.hundunlab.site.common.context.BaseContext;
import com.hundunlab.site.common.exception.MyException.*;
import com.hundunlab.site.common.util.AliOSSUtil;
import com.hundunlab.site.common.util.FileUploadValidator;
import com.hundunlab.site.common.util.PasswordUtil;
import com.hundunlab.site.module.user.cache.UserRedisService;
import com.hundunlab.site.module.user.mapper.UserMapper;
import com.hundunlab.site.module.admin.pojo.DTO.DeleteUsersDTO;
import com.hundunlab.site.module.user.pojo.DTO.PutDTO;
import com.hundunlab.site.module.auth.pojo.dto.RegisterDTO;
import com.hundunlab.site.module.user.pojo.DTO.UpdatePasswordDTO;
import com.hundunlab.site.module.user.pojo.Entity.User;
import com.hundunlab.site.module.user.pojo.VO.RegisterVO;
import com.hundunlab.site.module.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

import static com.hundunlab.site.common.constant.redis.RedisConstant.EMAIL_CODE_PATH;
import static com.hundunlab.site.common.constant.oss.UserOssConstant.USER_AVATAR_FOLDER;
import static com.hundunlab.site.common.constant.status.UserStatusConstant.*;

@Slf4j
@Service
public class UserServiceImpl implements UserService {
    //26/3/26 芜柒:看见之前的代码想把自己打一顿怎么办
    //26/4/1  芜柒:腿打断
    @Autowired
    private UserMapper um;
    @Autowired
    private UserRedisService userRedisService;
    @Autowired
    private AliOSSUtil aliOssUtil;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private FileUploadValidator fileUploadValidator;

    @Transactional(rollbackFor = Exception.class)
    public RegisterVO register(RegisterDTO dto) {
        //1检验用户是否已经存在
        checkUserNotExists(dto.getUsername(),dto.getPhone(),dto.getEmail());
        //2 不存在 插入
        User u = new User();
        BeanUtils.copyProperties(dto,u);
        u.setPasswordHash(PasswordUtil.addMi(dto.getPassword()));
        um.insertUser(u);
        //3 删除管理端用户查询的分页缓存
        userRedisService.clearUsersCache();
        //4 返回
        return new RegisterVO(u.getUsername());
    }

    @Transactional
    @Override
    public void deleteUsers(DeleteUsersDTO dto) throws IllegalAccessException {

        //拿到执行人对象
        String operateUsername = BaseContext.getCurrentUsername();
        User operateUser = getValidUserByUserName(operateUsername);
        //检验执行者身份
        log.info("当前操作用户{}状态为{}", operateUser.getUsername(), operateUser.getStatus());
        if (operateUser.getStatus() == STATUS_USER) {
            //普通用户 走根据密码删除
            //1 校验密码
            if (!PasswordUtil.check(dto.getPassword(), operateUser.getPasswordHash())) {
                throw new PasswordException("密码不正确!");
            }
            //2 校验传参合法性
            if (dto.getUsernames().size() > 1 || !dto.getUsernames().get(0).equals(operateUsername)) {//删除自己之外的用户则报错
                throw new InsufficientPermissionsException("当前用户权限不足!");
            }


            //3删除自己
            um.deleteUserByUserNameList(dto.getUsernames());

            //4 删除后立即吊销自己的token与数据缓存
            userRedisService.clearUserToken(operateUser.getId());
            userRedisService.clearUserdata(operateUser.getId());
            userRedisService.clearUsersCache();

        } else if (operateUser.getStatus() == STATUS_ADMIN || operateUser.getStatus() == STATUS_SUPER_ADMIN) {
            //管理员 无需密码删除
            //1 传参存在性判断
            List<User> validUserListByUserNameList = getValidUserListByUserNameList(dto.getUsernames());
            if (validUserListByUserNameList.isEmpty()) {
                return;
            }
            //2 禁止越级删除：普通管理员不能删除管理员，任何人不能删除超级管理员
            for (User target : validUserListByUserNameList) {
                if (!canManage(operateUser.getStatus(), target.getStatus())) {
                    throw new InsufficientPermissionsException("当前用户权限不足，不能删除该用户!");
                }
            }
            //3 删除这些合法参数
            um.deleteUserByUserNameList(dto.getUsernames());

            //4 删除后逐一吊销token与数据缓存，防止已删除用户持旧token继续访问
            for (User target : validUserListByUserNameList) {
                userRedisService.clearUserToken(target.getId());
                userRedisService.clearUserdata(target.getId());
            }
            userRedisService.clearUsersCache();
        } else {
            throw new IllegalAccessException("用户已被禁用!");
        }
    }

    @Override
    public void updateAvatar(MultipartFile file) {
        //头像只允许图片，并限制大小与内容类型
        fileUploadValidator.validate(file, true);
        //构建文件路径
        Long currentId = BaseContext.getCurrentId();
        String currentUsername = BaseContext.getCurrentUsername();
        String fileName = aliOssUtil.makeUUIDFileName(file);
        String objectName = currentUsername + USER_AVATAR_FOLDER  + fileName;
        //拿到用户对象
        User u;
        User userInRedis = userRedisService.getUserInRedis(currentId);
        if(userInRedis!=null){
            u = userInRedis;
        }else{
            u = um.selectUserById(currentId);
            userRedisService.saveUserInRedis(u);
        }
        if(u==null){
            throw new UserNotFoundException("用户不存在");
        }
        //检查用户是否已有头像 如果有 删除
        if(u.getAvatarUrl() != null){
            aliOssUtil.delete(u.getAvatarUrl());
        }
        //在oss中添加
        try {
            aliOssUtil.upload(file, objectName);
        } catch (Exception e) {
            throw new OSSException("上传头像失败了...是oss的问题...");
        }
        //在数据库中改写头像字段

        try {
            um.updateUserAvatarPathById(currentId, objectName);
        }catch (Exception e){
            aliOssUtil.delete(objectName);
            throw new SqlOperationException("更新输入库失败...");
        }
        //删除用户缓存
        userRedisService.clearUserdata(BaseContext.getCurrentId());

    }

    //辅助性方法 注册时候验证username  email phone 的唯一性
    public void checkUserNotExists(String username,String phone,String email)
    {
        Integer id = um.checkUserNotExists(username,phone,email);
        //无事发生
        if(id == null || id == 0) {
            return;
        }
        //统一报错文案，不指明具体哪个字段被占用，避免注册接口被用于枚举已注册账号
        throw new UserExistedException("注册信息不可用，请更换后重试");
    }


    //辅助性方法 用户是否存在
    //1 单个用户
    public User getValidUserByIdInMysql(Long id){
        User u =  um.selectUserById(id);
        if(u==null){
            throw new UserNotFoundException("用户不存在");
        }
        userRedisService.saveUserInRedis(u);
        return u;
    }
    public User getValidUserByUserName(String username)
        {
            log.info("单个用户校验存在性开始");
            User u = um.getUserByUserName(username);
            if(u==null)
            {
                throw new UserNotFoundException("用户不存在");
            }
            log.info("已经查询到当前使用而存在的用户信息:{}",u);
            return u;
        }
    //2 用户名集合
    public List<User> getValidUserListByUserNameList(List<String> userNameList)
    {
        if(userNameList==null)
        {
            throw new IllegalArgumentException("删除用户传参错误") ;
        }
        log.info("多个用户校验存在性开始");
        List<User> userList = um.selectByUserNameList(userNameList);
        //num代表测试有没有不存在的用户名
        int num =  userNameList.size()-userList.size();
        if(num!=0)
        {
            log.warn("有"+num+"个不存在的用户 服务器已经忽视");
        }
//        log.error("通过");

        return userList;
    }

    public User selectUserById(Long id) {
        // 先从redis里面那
        User userInRedis = userRedisService.getUserInRedis(id);
        if( userInRedis != null ){
            return userInRedis;
        }
        User u = um.selectUserById(id);
        userRedisService.saveUserInRedis(u);
        return u;
    }

    @Override
    public void updateNickname(PutDTO dto) {
        //获取当前用户
        Long id = BaseContext.getCurrentId();
        User user = getValidUserByIdInMysql(id);
        //校验密码
        if(!PasswordUtil.check(dto.getPassword(),user.getPasswordHash())){
            throw new PasswordException("密码输错了哦...");
        }
        //更新用户
        String nickname = dto.getValue();
        user.setNickname(nickname);
        um.updateNickNameById(nickname,id);
        userRedisService.saveUserInRedis(user);
    }

    @Override
    public void updateEmail(PutDTO dto) {
        //格式校验：防止把邮箱改成非法值后账号找回通道被静默切断
        String email = dto.getValue();
        if (email == null || !email.matches("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$")) {
            throw new IllegalArgumentException("邮箱格式错误");
        }
        //获取当前用户
        Long id = BaseContext.getCurrentId();
        User user = getValidUserByIdInMysql(id);
        //校验密码
        if(!PasswordUtil.check(dto.getPassword(),user.getPasswordHash())){
            throw new PasswordException("密码输错了哦...");
        }
        //更新用户
        user.setEmail(email);
        try{
            um.updateEmailById(email,id);
        }catch (Exception e){
            throw new SqlOperationException("这个邮箱已被使用...");
        }
        userRedisService.saveUserInRedis(user);
    }

    @Override
    public void updatePhone(PutDTO dto) {
        //格式校验：防止把手机号改成非法值
        String phone = dto.getValue();
        if (phone == null || !phone.matches("^1[3-9]\\d{9}$")) {
            throw new IllegalArgumentException("手机号格式错误");
        }
        //获取当前用户
        Long id = BaseContext.getCurrentId();
        User user = getValidUserByIdInMysql(id);
        //校验密码
        if(!PasswordUtil.check(dto.getPassword(),user.getPasswordHash())){
            throw new PasswordException("密码输错了哦...");
        }
        //更新用户
        user.setPhone(phone);
        try{
            um.updatePhoneById(phone,id);
        }catch (Exception e){
            throw new SqlOperationException("这个手机号已被使用...");
        }
        userRedisService.saveUserInRedis(user);
    }

    @Override
    public void updatePassword(UpdatePasswordDTO dto) {
        //1 在redis中读验证码
        User user = userRedisService.getUserInRedis(BaseContext.getCurrentId());
        if(user == null){
            user = getValidUserByIdInMysql(BaseContext.getCurrentId());
        }
        String key = EMAIL_CODE_PATH + user.getEmail();
        String codeInRedis = stringRedisTemplate.opsForValue().get(key);
        //2 验证（恒定时间比较，防时序侧信道穷举验证码）
        if (codeInRedis == null){
            throw new UserNotFoundException("验证码已过期");
        }
        if(!constantTimeEquals(dto.getCode(), codeInRedis)){
            throw new PasswordException("验证码错误");
        }
        //3 修改
        um.updatePasswordById(PasswordUtil.addMi(dto.getPassword()),BaseContext.getCurrentId());
        //4 验证码一次性使用，修改成功后立即删除，防止 TTL 内被重复利用
        stringRedisTemplate.delete(key);
        //5 删除缓存
        userRedisService.clearUserdata(BaseContext.getCurrentId());

    }


    /**
     * 恒定时间字符串比较，避免逐字符短路比较造成的时序泄露
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return java.security.MessageDigest.isEqual(
                a.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                b.getBytes(java.nio.charset.StandardCharsets.UTF_8));
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


