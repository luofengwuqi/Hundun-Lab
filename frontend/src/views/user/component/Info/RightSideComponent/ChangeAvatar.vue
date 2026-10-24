<script setup>
import { ref, computed, onUnmounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import defaultAvatar from '@/assets/img/common/user.jpg'
import { updateAvatarAPI } from '@/api/user'

const userStore = useUserStore()
const previewUrl = ref('')
const selectedFile = ref(null)
const uploading = ref(false)

// OSS 域名配置
const OSS_DOMAIN = 'https://fifteen-hundun.oss-cn-beijing.aliyuncs.com'

// 获取当前用户头像 - 使用 computed 保证响应式
const currentAvatar = computed(() => {
  const avatarUrl = userStore.userInfo?.avatarUrl
  if (!avatarUrl) return defaultAvatar
  // 如果已经是完整 URL，直接返回；否则拼接 OSS 域名
  return avatarUrl.startsWith('http') ? avatarUrl : `${OSS_DOMAIN}/${avatarUrl}`
})

// 压缩图片到指定大小（默认 1MB）
const compressImage = (file, maxSize = 1024 * 1024) => {
  return new Promise((resolve, reject) => {
    // 如果原图已经小于 maxSize，直接返回原图
    if (file.size <= maxSize) {
      resolve(file)
      return
    }

    const reader = new FileReader()
    reader.readAsDataURL(file)
    reader.onload = (e) => {
      const img = new Image()
      img.src = e.target.result
      img.onload = () => {
        const canvas = document.createElement('canvas')
        let width = img.width
        let height = img.height

        // 计算压缩比例，目标是将文件大小降到 maxSize 以内
        // 先估算需要的尺寸缩小比例
        const sizeRatio = file.size / maxSize
        const dimensionRatio = Math.sqrt(sizeRatio)

        // 如果尺寸过大，先缩小尺寸
        const maxWidth = 1920
        const maxHeight = 1920
        if (width > maxWidth || height > maxHeight) {
          const ratio = Math.min(maxWidth / width, maxHeight / height)
          width = Math.floor(width * ratio)
          height = Math.floor(height * ratio)
        }

        // 再根据文件大小进一步缩小
        if (dimensionRatio > 1) {
          width = Math.floor(width / dimensionRatio)
          height = Math.floor(height / dimensionRatio)
        }

        canvas.width = width
        canvas.height = height
        const ctx = canvas.getContext('2d')
        ctx.drawImage(img, 0, 0, width, height)

        // PNG 转 JPEG 以获得更好的压缩效果
        const outputType = 'image/jpeg'
        let quality = 0.8
        let compressedDataUrl = canvas.toDataURL(outputType, quality)

        // 如果仍然超过大小限制，逐步降低质量
        while (compressedDataUrl.length * 0.75 > maxSize && quality > 0.3) {
          quality -= 0.1
          compressedDataUrl = canvas.toDataURL(outputType, quality)
        }

        // 将 DataURL 转换为 Blob
        fetch(compressedDataUrl)
          .then(res => res.blob())
          .then(blob => {
            // 创建新的 File 对象，改为 jpg 格式
            const fileName = file.name.replace(/\.\w+$/, '.jpg')
            const compressedFile = new File([blob], fileName, {
              type: outputType,
              lastModified: Date.now()
            })
            resolve(compressedFile)
          })
          .catch(reject)
      }
      img.onerror = reject
    }
    reader.onerror = reject
  })
}

// 处理文件选择
const handleFileChange = async (event) => {
  const file = event.target.files[0]
  if (!file) return

  // 验证文件类型
  const validTypes = ['image/jpeg', 'image/png', 'image/webp']
  if (!validTypes.includes(file.type)) {
    ElMessage.error('只支持 JPG、PNG、WEBP 格式的图片')
    return
  }

  // 验证文件大小（限制 5MB）
  const maxSize = 5 * 1024 * 1024
  if (file.size > maxSize) {
    ElMessage.error('图片大小不能超过 5MB')
    return
  }

  // 压缩图片到 500KB
  ElMessage.info('正在压缩图片...')
  try {
    const compressedFile = await compressImage(file, 500 * 1024)

    // 创建预览 URL
    previewUrl.value = URL.createObjectURL(compressedFile)
    selectedFile.value = compressedFile

    ElMessage.success(`图片已压缩：${(file.size / 1024).toFixed(2)}KB → ${(compressedFile.size / 1024).toFixed(2)}KB`)
  } catch (error) {
    ElMessage.error('图片压缩失败，请重试')
  }
}

// 确认上传
const handleConfirm = async () => {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择图片')
    return
  }

  uploading.value = true

  try {
    // 创建 FormData 并上传
    const formData = new FormData()
    formData.append('file', selectedFile.value)
    await updateAvatarAPI(formData)

    // 从后端重新获取用户信息
    await userStore.initUserInfo()

    ElMessage.success('头像修改成功')

    // 清空选择
    selectedFile.value = null
    previewUrl.value = ''
  } catch (error) {
    ElMessage.error('头像修改失败，请重试')
  } finally {
    uploading.value = false
  }
}

// 取消操作
const handleCancel = () => {
  selectedFile.value = null
  previewUrl.value = ''
}

// 组件卸载时清理资源
onUnmounted(() => {
  if (previewUrl.value) {
    URL.revokeObjectURL(previewUrl.value)
  }
})
</script>

<template>
  <div class="change-avatar">
    <div class="avatar-container">
      <!-- 头像对比区域 -->
      <div class="avatars-row">
        <!-- 当前头像展示 -->
        <div class="avatar-item">
          <div class="avatar-label">当前头像</div>
          <el-avatar :size="120" :src="currentAvatar" />
        </div>

        <!-- 箭头指示 -->
        <div class="arrow-icon">
          <el-icon :size="50"><Right /></el-icon>
        </div>

        <!-- 新头像预览 -->
        <div class="avatar-item">
          <div class="avatar-label">新头像预览</div>
          <div class="preview-box">
            <el-avatar v-if="previewUrl" :size="120" :src="previewUrl" />
            <div v-else class="preview-placeholder">
              <el-icon :size="40"><Picture /></el-icon>
              <span>暂无预览</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 文件选择 -->
      <div class="file-input-wrapper">
        <input
          type="file"
          accept="image/jpeg,image/png,image/webp"
          @change="handleFileChange"
          class="file-input"
          id="avatar-upload"
        />
        <label for="avatar-upload" class="select-btn">
          <el-icon><Upload /></el-icon>
          选择图片
        </label>
      </div>

      <!-- 提示文字 -->
      <div class="tips">
        <p style="color: #C3272B">强烈建议您将头像裁剪为正方形后再上传!ಠ_ಠ</p>
        <p>支持 JPG、PNG、WEBP 格式</p>
        <p>文件大小不超过 5MB（自动压缩至 500KB 以内）</p>
      </div>

      <!-- 操作按钮 -->
      <div class="action-buttons">
        <el-button @click="handleCancel" :disabled="uploading">取消</el-button>
        <el-button type="danger" @click="handleConfirm" :loading="uploading" :disabled="!selectedFile" class="confirm-btn">
          确认修改
        </el-button>
      </div>
    </div>
  </div>
</template>

<style scoped lang="less">
.change-avatar {
  padding: 40px;
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: calc(100vh - 160px);
}

.avatar-container {
  width: 100%;
  max-width: 700px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 30px;
}

.avatars-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 40px;
  width: 100%;
}

.avatar-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 15px;
  flex: 1;
}

.arrow-icon {
  color: #C3272B;
  flex-shrink: 0;
}

.avatar-label {
  font-size: 16px;
  font-weight: 500;
  color: #303133;
}

.preview-box {
  width: 120px;
  height: 120px;
  border: 2px dashed #dcdfe6;
  border-radius: 50%;
  display: flex;
  justify-content: center;
  align-items: center;
  overflow: hidden;
  background-color: #f5f7fa;
}

.preview-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: #909399;

  span {
    font-size: 12px;
  }
}

.file-input-wrapper {
  position: relative;
}

.file-input {
  display: none;
}

.select-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  background-color: #C3272B;
  color: white;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s;

  &:hover {
    background-color: #d63a3e;
  }
}

.tips {
  text-align: center;
  color: #909399;
  font-size: 13px;
  line-height: 1.8;

  p {
    margin: 0;
  }
}

.action-buttons {
  display: flex;
  gap: 20px;
  margin-top: 10px;

  .el-button {
    min-width: 100px;
  }

  .confirm-btn {
    background-color: #C3272B;
    border-color: #C3272B;

    &:hover {
      background-color: #d63a3e;
      border-color: #d63a3e;
    }

    &:active {
      background-color: #a81f23;
      border-color: #a81f23;
    }
  }
}
</style>
