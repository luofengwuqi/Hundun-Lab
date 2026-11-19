import http from './http'

/**
 * 后台软件管理（F12 / F13，D101–D103）。
 *
 * 两条路径刻意分开，别合并：
 * - 资料与内容块走 JSON，`/admin/software/**`；
 * - 安装包走 `/admin/software/{id}/releases/{rid}/assets` 的 multipart 专用口，
 *   上限 2GiB、只进私有桶、边传边算 sha256。**不要**改用 `/files`（那是 10MiB 的通用口）。
 *
 * 子块（特性 / 截图 / 平台 / 仓库）是「整块替换」语义：提交什么就是什么，
 * 不在数组里的旧行会被删掉，顺序按数组下标。所以这里不做 per-row 的增删接口。
 */

export function fetchSoftwareList({ keyword = '', status = null } = {}) {
  return http.get('/admin/software', {
    params: { keyword: keyword || undefined, status: status ?? undefined },
  })
}

export function fetchSoftwareDetail(id) {
  return http.get(`/admin/software/${id}`)
}

export function createSoftware(dto) {
  return http.post('/admin/software', dto)
}

export function updateSoftware(id, dto) {
  return http.put(`/admin/software/${id}`, dto)
}

export function changeSoftwareStatus(id, status) {
  return http.put(`/admin/software/${id}/status`, { status })
}

export function deleteSoftware(id) {
  return http.delete(`/admin/software/${id}`)
}

// ==================== 版本 ====================

export function createRelease(softwareId, dto) {
  return http.post(`/admin/software/${softwareId}/releases`, dto)
}

export function updateRelease(softwareId, releaseId, dto) {
  return http.put(`/admin/software/${softwareId}/releases/${releaseId}`, dto)
}

export function deleteRelease(softwareId, releaseId) {
  return http.delete(`/admin/software/${softwareId}/releases/${releaseId}`)
}

// ==================== 安装包 ====================

/**
 * 上传安装包。
 *
 * `timeout: 0` 是有意的：全局 12 秒对 2GiB 的包毫无意义，上传时间取决于上行带宽。
 * 进度回调给页面画进度条——没有它，大文件上传期间用户只会看到按钮不动。
 */
export function uploadReleaseAsset(
  softwareId,
  releaseId,
  { platform, arch, packageType, file, onProgress },
) {
  const data = new FormData()
  data.append('platform', platform)
  if (arch) data.append('arch', arch)
  data.append('packageType', packageType)
  data.append('file', file)
  return http.post(`/admin/software/${softwareId}/releases/${releaseId}/assets`, data, {
    timeout: 0,
    onUploadProgress: (event) => {
      if (!event.total) return
      onProgress?.(Math.round((event.loaded / event.total) * 100))
    },
  })
}

export function deleteReleaseAsset(softwareId, releaseId, assetId) {
  return http.delete(`/admin/software/${softwareId}/releases/${releaseId}/assets/${assetId}`)
}

// ==================== 图片 ====================

/**
 * 封面 / 截图上传（`POST /files`，需 status ≥ 2，10MiB，禁 zip）。
 * 返回可直接放进 `<img src>` 的 URL。
 */
export function uploadImage(file) {
  const data = new FormData()
  data.append('file', file)
  return http.post('/files', data, { timeout: 120000 })
}
