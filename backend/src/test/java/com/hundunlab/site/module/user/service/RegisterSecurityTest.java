package com.hundunlab.site.module.user.service;

import com.hundunlab.site.common.exception.MyException.BizException;
import com.hundunlab.site.common.exception.MyException.UserExistedException;
import com.hundunlab.site.common.response.ErrorCode;
import com.hundunlab.site.common.util.AliOSSUtil;
import com.hundunlab.site.common.util.FileUploadValidator;
import com.hundunlab.site.module.admin.pojo.DTO.AddAdminDTO;
import com.hundunlab.site.module.admin.service.impl.AdminServiceImpl;
import com.hundunlab.site.module.auth.pojo.dto.RegisterDTO;
import com.hundunlab.site.module.user.cache.UserRedisService;
import com.hundunlab.site.module.user.mapper.AdminMapper;
import com.hundunlab.site.module.user.mapper.UserMapper;
import com.hundunlab.site.module.user.pojo.Entity.User;
import com.hundunlab.site.module.user.pojo.VO.RegisterVO;
import com.hundunlab.site.module.user.service.serviceimpl.UserServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 注册入参 → 落库这一段的安全契约。
 * <p>
 * 这些约束都不是「风格问题」，每一条都对应一种实际可触发的失败或越权：
 * 空串占唯一索引、客户端自带头像、依赖数据库默认角色、并发注册看到 500。
 * 改注册逻辑先跑这个测试。
 */
@ExtendWith(MockitoExtension.class)
class RegisterSecurityTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private UserRedisService userRedisService;
    @Mock
    private AliOSSUtil aliOssUtil;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private FileUploadValidator fileUploadValidator;

    @InjectMocks
    private UserServiceImpl userService;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    private static RegisterDTO dto(String username, String password) {
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername(username);
        dto.setPassword(password);
        return dto;
    }

    @Test
    @DisplayName("空串邮箱/手机号归一化为 NULL，不占唯一索引坑位")
    void blankOptionalFieldsBecomeNull() {
        RegisterDTO request = dto("wuqi", "secret123");
        // 前端「没填」被序列化成空串是最常见的情况：@Email 放行 ""，@Pattern 之外的字段也放行
        request.setEmail("");
        request.setPhone("   ");

        RegisterVO vo = userService.register(request);

        verify(userMapper).insertUser(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertNull(saved.getEmail(), "空串邮箱必须落成 NULL，否则第二个空串注册会撞 unique_email");
        assertNull(saved.getPhone(), "空白手机号必须落成 NULL");
        assertEquals("wuqi", vo.getUsername());
    }

    @Test
    @DisplayName("注册产物由服务端决定：角色固定普通用户，客户端头像不采纳")
    void registerForcesServerSideFields() {
        RegisterDTO request = dto("wuqi", "secret123");
        request.setAvatarUrl("https://evil.example/tracker.png");

        userService.register(request);

        verify(userMapper).insertUser(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertEquals(1, saved.getStatus(), "注册一律产出 status = 1，不依赖数据库默认值");
        assertNull(saved.getAvatarUrl(), "头像只能走 PUT /user/avatar 上传，注册时不得写入任意 URL");
        assertEquals("wuqi", saved.getNickname(), "昵称缺省与用户名一致");
    }

    @Test
    @DisplayName("用户名/邮箱/手机号先 trim 再查重与落库")
    void inputIsTrimmedBeforeCheckAndInsert() {
        RegisterDTO request = dto("  wuqi  ", "secret123");
        request.setEmail(" wuqi@example.com ");
        request.setPhone(" 13800138000 ");

        userService.register(request);

        // 不 trim 的话 MySQL 会把 "wuqi " 当成另一个用户，而前端登录时会 trim，人自己就登不进来
        verify(userMapper).checkUserNotExists("wuqi", "13800138000", "wuqi@example.com");
        verify(userMapper).insertUser(userCaptor.capture());
        assertEquals("wuqi", userCaptor.getValue().getUsername());
        assertEquals("wuqi@example.com", userCaptor.getValue().getEmail());
        assertEquals("13800138000", userCaptor.getValue().getPhone());
    }

    @Test
    @DisplayName("密码只落哈希，不落明文")
    void passwordIsHashed() {
        userService.register(dto("wuqi", "secret123"));

        verify(userMapper).insertUser(userCaptor.capture());
        String hash = userCaptor.getValue().getPasswordHash();
        assertNotEquals("secret123", hash);
        assertTrue(hash.startsWith("$2"), "应为 BCrypt 哈希：" + hash);
    }

    @Test
    @DisplayName("查重命中：抛 USER_ALREADY_EXISTS，且不进入 INSERT")
    void existingAccountIsRejectedBeforeInsert() {
        when(userMapper.checkUserNotExists(eq("wuqi"), any(), any())).thenReturn(1);

        assertThrows(UserExistedException.class, () -> userService.register(dto("wuqi", "secret123")));
        verify(userMapper, never()).insertUser(any());
    }

    @Test
    @DisplayName("并发注册撞唯一索引：收敛成 409，不把 DuplicateKeyException 漏成 500")
    void duplicateKeyIsTranslatedToConflict() {
        when(userMapper.checkUserNotExists(eq("wuqi"), any(), any())).thenReturn(0);
        doThrow(new DuplicateKeyException("unique_username"))
                .when(userMapper).insertUser(any());

        assertThrows(UserExistedException.class, () -> userService.register(dto("wuqi", "secret123")));
        verify(userRedisService, never()).clearUsersCache();
    }

    @Test
    @DisplayName("管理员邀请注册：同样不采纳客户端头像，空串同样归一化为 NULL")
    void adminRegisterAppliesSameRules() {
        AdminMapper adminMapper = mock(AdminMapper.class);
        AdminServiceImpl adminService = adminService(adminMapper);

        when(adminMapper.selectInvitePassword("invite-code")).thenReturn("invite-code");
        when(adminMapper.deleteInvitePlaceholder("invite-code")).thenReturn(1);

        AddAdminDTO request = new AddAdminDTO();
        request.setUsername("newadmin");
        request.setPassword("secret123");
        request.setImportantPassword("invite-code");
        request.setAvatarUrl("https://evil.example/tracker.png");
        request.setEmail("");

        adminService.addAdmin(request);

        verify(userMapper).insertUser(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertEquals(2, saved.getStatus(), "邀请注册产出管理员，而不是普通用户");
        assertNull(saved.getAvatarUrl());
        assertNull(saved.getEmail());
    }

    @Test
    @DisplayName("邀请码不存在或已被用掉：返回 INVITE_CODE_INVALID，不落库")
    void invalidInviteCodeIsRejected() {
        AdminMapper adminMapper = mock(AdminMapper.class);
        AdminServiceImpl adminService = adminService(adminMapper);

        // select 找不到 → 码不存在（码是被 DELETE 消费的，用掉的码在库里也不存在）
        when(adminMapper.selectInvitePassword("bad-code")).thenReturn(null);

        AddAdminDTO request = new AddAdminDTO();
        request.setUsername("newadmin");
        request.setPassword("secret123");
        request.setImportantPassword("bad-code");

        BizException error = assertThrows(BizException.class, () -> adminService.addAdmin(request));
        assertEquals(ErrorCode.INVITE_CODE_INVALID, error.getErrorCode());
        verify(userMapper, never()).insertUser(any());
    }

    private AdminServiceImpl adminService(AdminMapper adminMapper) {
        AdminServiceImpl service = new AdminServiceImpl();
        inject(service, "am", adminMapper);
        inject(service, "um", userMapper);
        inject(service, "redisTemplate", mock(RedisTemplate.class));
        inject(service, "objectMapper", new ObjectMapper());
        inject(service, "userRedisService", userRedisService);
        return service;
    }

    private static void inject(Object target, String field, Object value) {
        try {
            var declared = target.getClass().getDeclaredField(field);
            declared.setAccessible(true);
            declared.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("注入字段失败：" + field, e);
        }
    }
}
