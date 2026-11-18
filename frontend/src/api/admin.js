import http from './http'

/**
 * 管理端接口封装（`/admin/**`，需要 status ≥ 2）。
 *
 * 后端这套是底座遗留的九个接口（docs/04 §3.7），入参字段名以 DTO 为准：
 * GetUsersDTO / UpdateUserDTO。注意两处后端脾气：
 * 1. `/admin/user/page` 查不到结果时抛的是 `USER_NOT_FOUND`(404)，不是空数组，
 *    调用方得把它当「空列表」处理，不能弹成错误；
 * 2. `/admin/user/status` 是「取反」语义，不传目标状态，返回改完之后的 status。
 */

export function fetchUsers({ page = 1, size = 20, name = '', status = null } = {}) {
  return http.post('/admin/user/page', {
    page,
    size,
    name: name || undefined,
    status: status ?? undefined,
  })
}

export function fetchUserCount() {
  return http.get('/admin/users/count')
}

/** 只改传了的字段：后端 SQL 对 null/空串是「不动这一列」 */
export function updateUser(dto) {
  return http.put('/admin/user', dto)
}

export function toggleUserStatus(username) {
  return http.put('/admin/user/status', null, { params: { username } })
}

export function deleteUser(id) {
  return http.delete(`/admin/user/${id}`)
}

/**
 * 邀请管理（仅超管，后端两个接口都 @RequireRole(3)）。
 * 生成走 GET 而非 POST：这是底座遗留语义，它会**插入**一行占位用户，
 * 不是幂等查询，别当只读接口随手重试。
 */
export function makeInviteCode() {
  return http.get('/admin/invite-code')
}

/** 待用邀请码：只返回占位行的 id / code / createdAt（User.inviteCode 是 @JsonIgnore） */
export function fetchInvites() {
  return http.get('/admin/invites')
}

/** 撤销邀请码 = 删掉它对应的占位用户行 */
export function revokeInvite(id) {
  return deleteUser(id)
}
