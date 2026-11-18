package com.hundunlab.site.module.admin.pojo.VO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 待用邀请码（邀请管理页专用视图）。
 * <p>
 * 为什么不直接返回 {@code User}：{@code User.inviteCode} 标了 {@code @JsonIgnore}——
 * 那是故意的，用户管理页不该把码带给管理员以外的人。邀请管理页是超管专属，
 * 用这个 VO 单开一条出口，比把 @JsonIgnore 拿掉安全得多。
 * <p>
 * {@code id} 是占位用户行的主键，撤销邀请码就是 {@code DELETE /admin/user/{id}}。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class InviteCodeVO {

    private Long id;

    private String code;

    /** 生成时间（占位行的 created_at） */
    private LocalDateTime createdAt;
}
