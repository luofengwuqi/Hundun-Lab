package com.hundunlab.site.module.admin.service.impl;

import com.hundunlab.site.module.user.pojo.DTO.UpdateUserDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static com.hundunlab.site.common.constant.status.UserStatusConstant.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 后台层级铁律：「不能越权修改高位权限人的信息」。
 * <p>
 * 纯函数测试，不依赖 Spring / Redis / MySQL——{@code canManage} 不碰任何字段，
 * 直接 new 一个实例就能验。改这条规则先跑这个测试，它比手工点界面可靠。
 */
class AdminPermissionTest {

    private final AdminServiceImpl service = new AdminServiceImpl();

    @Test
    @DisplayName("超管不可被任何人修改（含另一个超管）")
    void superAdminCannotBeManagedByAnyone() {
        List<Integer> actors = Arrays.asList(
                STATUS_SUPER_ADMIN, STATUS_ADMIN, STATUS_USER, STATUS_USER_BAN, STATUS_ADMIN_BAN);
        for (Integer actor : actors) {
            assertFalse(service.canManage(actor, STATUS_SUPER_ADMIN),
                    "status=" + actor + " 不应能操作超管");
        }
    }

    @Test
    @DisplayName("普通管理员只能动普通用户及其禁用态")
    void adminOnlyManagesNormalUsers() {
        assertTrue(service.canManage(STATUS_ADMIN, STATUS_USER));
        assertTrue(service.canManage(STATUS_ADMIN, STATUS_USER_BAN));

        assertFalse(service.canManage(STATUS_ADMIN, STATUS_ADMIN), "不得动同级管理员");
        assertFalse(service.canManage(STATUS_ADMIN, STATUS_ADMIN_BAN), "不得动被禁用的管理员");
        assertFalse(service.canManage(STATUS_ADMIN, STATUS_SUPER_ADMIN));
    }

    @Test
    @DisplayName("超管可管所有低于自己的角色，但不含自己")
    void superAdminManagesEveryoneBelow() {
        assertTrue(service.canManage(STATUS_SUPER_ADMIN, STATUS_ADMIN));
        assertTrue(service.canManage(STATUS_SUPER_ADMIN, STATUS_ADMIN_BAN));
        assertTrue(service.canManage(STATUS_SUPER_ADMIN, STATUS_USER));
        assertTrue(service.canManage(STATUS_SUPER_ADMIN, STATUS_USER_BAN));

        assertFalse(service.canManage(STATUS_SUPER_ADMIN, STATUS_SUPER_ADMIN), "不得动自己");
    }

    @Test
    @DisplayName("普通用户 / 禁用账号没有任何管理权")
    void normalAndBannedUsersManageNothing() {
        for (Integer actor : List.of(STATUS_USER, STATUS_USER_BAN, STATUS_ADMIN_BAN)) {
            for (Integer target : List.of(
                    STATUS_USER, STATUS_USER_BAN, STATUS_ADMIN, STATUS_ADMIN_BAN, STATUS_SUPER_ADMIN)) {
                assertFalse(service.canManage(actor, target),
                        "actor=" + actor + " target=" + target + " 不应放行");
            }
        }
    }

    @Test
    @DisplayName("状态缺失一律拒绝（默认拒绝，不 NPE）")
    void nullStatusIsDenied() {
        assertFalse(service.canManage(null, STATUS_USER));
        assertFalse(service.canManage(STATUS_SUPER_ADMIN, null));
        assertFalse(service.canManage(null, null));
    }

    @Test
    @DisplayName("UpdateUserDTO 不得有 status 字段——否则改资料就成了提权通道")
    void updateUserDtoHasNoStatusField() {
        List<String> fields = Arrays.stream(UpdateUserDTO.class.getDeclaredFields())
                .map(Field::getName)
                .toList();
        assertFalse(fields.contains("status"),
                "UpdateUserDTO 出现 status 字段会让 PUT /admin/user 绕过 changeStatus 的层级校验");
    }
}
