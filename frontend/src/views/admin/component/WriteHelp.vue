<script setup>
import { reactive, ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Document, EditPen, Check, Clock } from '@element-plus/icons-vue'
import { marked } from 'marked'
import { submitHelpArticle } from '@/api/article.js'
import { useUserStore } from '@/stores/user.js'

// 配置 marked 选项
marked.setOptions({
  breaks: true,  // 支持 GitHub 风格的换行
  gfm: true,     // 启用 GitHub Flavored Markdown
})

// 表单数据
const formData = reactive({
  title: '',
  text: ''
})

// 表单验证规则
const rules = {
  title: [
    { required: true, message: '请输入标题', trigger: 'blur' },
    { min: 1, max: 100, message: '长度在 1 到 100 个字符', trigger: 'blur' }
  ],
  text: [
    { required: true, message: '请输入正文', trigger: 'blur' }
  ]
}

// 表单引用
const formRef = ref(null)

// 计算字数统计
const titleCount = computed(() => formData.title.length)
const textCount = computed(() => formData.text.length)

// 渲染 Markdown 内容
const renderedMarkdown = computed(() => {
  if (!formData.text) return ''
  try {
    return marked.parse(formData.text)
  } catch (error) {
    console.error('Markdown 解析错误:', error)
    return formData.text
  }
})

// 模拟自动保存状态
const isSaving = ref(false)
const lastSaved = ref(null)

// 提交表单
const handleSubmit = async () => {
  const valid = await formRef.value.validate()
  if (!valid) {
    return false
  }

  const userStore = useUserStore()
  const creator = userStore.userInfo.nickname || userStore.userInfo.username
  await submitHelpArticle(formData.title,formData.text,creator)

  ElMessage.success('提交成功')

  // 重置表单
  formData.title = ''
  formData.text = ''
  lastSaved.value = null
}
</script>

<template>
  <div class="write-help-container">
    <!-- 主内容区 -->
    <div class="main-content">
      <!-- 左侧表单区 -->
      <div class="form-section">
        <el-form
          :model="formData"
          :rules="rules"
          ref="formRef"
          class="help-form"
        >
          <!-- 标题输入区 -->
          <div class="input-group">
            <div class="group-header">
              <el-icon><EditPen /></el-icon>
              <span class="group-label">标题</span>
              <span class="char-count">{{ titleCount }}/100</span>
            </div>
            <el-form-item prop="title" class="no-margin">
              <el-input
                v-model.trim="formData.title"
                placeholder="输入简洁明了的标题..."
                maxlength="100"
                show-word-limit
              />
            </el-form-item>
          </div>

          <!-- 正文输入区 -->
          <div class="input-group">
            <div class="group-header">
              <el-icon><Document /></el-icon>
              <span class="group-label">正文内容</span>
              <span class="char-count">{{ textCount }} 字</span>
            </div>
            <el-form-item prop="text" class="no-margin">
              <el-input
                v-model="formData.text"
                type="textarea"
                :rows="13"
                placeholder="详细描述帮助内容,可以使用Markdown格式..."
              />
            </el-form-item>
          </div>

          <!-- 操作按钮区 -->
          <div class="action-bar">
            <div class="save-status" v-if="lastSaved">
              <el-icon><Check /></el-icon>
              <span>已保存 {{ lastSaved }}</span>
            </div>
            <el-button type="primary" @click="handleSubmit" class="submit-btn">
              <el-icon style="margin-right: 8px;"><Check /></el-icon>
              发布帮助文档
            </el-button>
          </div>
        </el-form>
      </div>

      <!-- 右侧预览区 -->
      <div class="preview-section">
      <div class="preview-section">
        <div class="preview-header">
          <el-icon><Clock /></el-icon>
          <span>实时预览</span>
        </div>
        <div class="preview-content">
          <div class="preview-title" v-if="formData.title">
            {{ formData.title }}
          </div>
          <div class="preview-placeholder" v-if="!formData.text && !formData.title">
            <el-icon :size="48"><Document /></el-icon>
            <p>开始输入以查看预览</p>
          </div>
          <div class="preview-text markdown-body" v-if="formData.text" v-html="renderedMarkdown">
          </div>
        </div>
      </div>
    </div>
  </div>
  </div>
</template>

<style scoped lang="less">
.write-help-container {
  width: 100%;
  min-height: calc(100vh - 120px);
  background-color: #FFF8E8;
  display: flex;
  flex-direction: column;

  // 主内容区 - 左右布局
  .main-content {
    flex: 1;
    display: grid;
    grid-template-columns: 1fr 400px;
    gap: 30px;
    padding: 30px 40px;

    // 左侧表单区
    .form-section {
      height: calc(100vh - 170px);
      overflow-y: auto;

      .help-form {
        background: #FFFFFF;
        border-radius: 16px;
        padding: 40px;
        box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
        min-height: 100%;
        display: flex;
        flex-direction: column;

        .input-group {
          margin-bottom: 28px;

          .group-header {
            display: flex;
            align-items: center;
            gap: 10px;
            margin-bottom: 12px;
            padding-bottom: 10px;
            border-bottom: 2px solid #FFEFD5;

            .el-icon {
              color: #C3272B;
              font-size: 20px;
            }

            .group-label {
              font-size: 16px;
              font-weight: 600;
              color: #5A4A42;
              flex: 1;
            }

            .char-count {
              font-size: 13px;
              color: #AAA;
              font-weight: 500;
            }
          }

          .no-margin {
            margin-bottom: 0;
          }
        }

        // 输入框样式优化
        :deep(.el-input) {
          .el-input__wrapper {
            border-radius: 10px;
            border: 2px solid #FFEFD5;
            background-color: #FFFBF8;
            box-shadow: none;
            padding: 2px 14px;
            transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);

            &:hover {
              border-color: #C3272B;
              background-color: #FFFFFF;
              transform: translateY(-1px);
              box-shadow: 0 4px 12px rgba(195, 39, 43, 0.1);
            }

            &.is-focus {
              border-color: #C3272B;
              background-color: #FFFFFF;
              box-shadow: 0 0 0 3px rgba(195, 39, 43, 0.1);
            }
          }

          .el-input__inner {
            font-size: 15px;
            color: #303133;
            font-weight: 500;

            &::placeholder {
              color: #C0C4CC;
              font-weight: 400;
            }
          }

          .el-input__count {
            background: transparent;
            color: #C3272B;
            font-size: 12px;
            font-weight: 600;
          }
        }

        // 文本域样式优化
        :deep(.el-textarea) {
          .el-textarea__inner {
            border-radius: 10px;
            border: 2px solid #FFEFD5;
            background-color: #FFFBF8;
            font-size: 15px;
            line-height: 1.8;
            color: #303133;
            padding: 16px;
            transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
            resize: vertical;
            box-shadow: none;
            font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;

            &::placeholder {
              color: #C0C4CC;
            }

            &:hover {
              border-color: #C3272B;
              background-color: #FFFFFF;
              transform: translateY(-1px);
              box-shadow: 0 4px 12px rgba(195, 39, 43, 0.1);
            }

            &:focus {
              border-color: #C3272B;
              background-color: #FFFFFF;
              box-shadow: 0 0 0 3px rgba(195, 39, 43, 0.1);
            }
          }
        }

        // 移除错误边框效果
        :deep(.el-form-item.is-error) {
          .el-input,
          .el-textarea {
            .el-input__wrapper,
            .el-textarea__inner {
              box-shadow: none !important;
              border-color: #F56C6C !important;
            }
          }
        }

        // 底部操作栏
        .action-bar {
          margin-top: auto;
          padding-top: 20px;
          display: flex;
          align-items: center;
          justify-content: flex-end;
          gap: 16px;

          .save-status {
            display: flex;
            align-items: center;
            gap: 6px;
            font-size: 13px;
            color: #67C23A;
            font-weight: 500;

            .el-icon {
              font-size: 16px;
            }
          }

          .submit-btn {
            flex: 1;
            max-width: 280px;
            height: 48px;
            border-radius: 10px;
            background: linear-gradient(135deg, #C3272B 0%, #D64A4F 100%);
            border: none;
            font-size: 16px;
            font-weight: 600;
            letter-spacing: 1px;
            color: #FFFFFF;
            transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
            box-shadow: 0 4px 12px rgba(195, 39, 43, 0.25);

            &:hover {
              transform: translateY(-2px);
              box-shadow: 0 6px 20px rgba(195, 39, 43, 0.35);
              background: linear-gradient(135deg, #D64A4F 0%, #E85A5F 100%);
            }

            &:active {
              transform: translateY(0);
              box-shadow: 0 2px 8px rgba(195, 39, 43, 0.2);
            }

            &:disabled {
              background: #E8E8E8;
              box-shadow: none;
              cursor: not-allowed;
              transform: none;
            }
          }
        }
      }
    }

    // 右侧预览区
    .preview-section {
      background: #FFFFFF;
      border-radius: 16px;
      box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
      overflow: hidden;
      display: flex;
      flex-direction: column;
      height: calc(100vh - 170px);
      position: sticky;
      top: 20px;

      .preview-header {
        background: linear-gradient(135deg, #FFF8E8 0%, #FFEFD5 100%);
        padding: 16px 24px;
        display: flex;
        align-items: center;
        gap: 10px;
        border-bottom: 2px solid #FFEFD5;

        .el-icon {
          color: #C3272B;
          font-size: 18px;
        }

        span {
          font-size: 15px;
          font-weight: 600;
          color: #5A4A42;
        }
      }

      .preview-content {
        padding: 24px;
        flex: 1;
        overflow-y: auto;

        .preview-title {
          font-size: 22px;
          font-weight: 700;
          color: #303133;
          margin-bottom: 20px;
          padding-bottom: 16px;
          border-bottom: 2px solid #FFEFD5;
          line-height: 1.4;
        }

        .preview-placeholder {
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: center;
          min-height: 300px;
          color: #C0C4CC;

          .el-icon {
            margin-bottom: 16px;
            opacity: 0.5;
          }

          p {
            font-size: 14px;
            margin: 0;
          }
        }

        .preview-text {
          font-size: 15px;
          line-height: 1.8;
          color: #5A4A42;
          word-break: break-word;

          // Markdown 样式
          &.markdown-body {
            :deep(h1) {
              font-size: 24px;
              font-weight: 700;
              color: #303133;
              margin: 24px 0 16px 0;
              padding-bottom: 8px;
              border-bottom: 2px solid #FFEFD5;
            }

            :deep(h2) {
              font-size: 20px;
              font-weight: 600;
              color: #303133;
              margin: 20px 0 12px 0;
              padding-bottom: 6px;
              border-bottom: 1px solid #FFEFD5;
            }

            :deep(h3) {
              font-size: 18px;
              font-weight: 600;
              color: #303133;
              margin: 16px 0 10px 0;
            }

            :deep(h4), :deep(h5), :deep(h6) {
              font-size: 16px;
              font-weight: 600;
              color: #303133;
              margin: 14px 0 8px 0;
            }

            :deep(p) {
              margin: 12px 0;
              line-height: 1.8;
            }

            :deep(strong) {
              font-weight: 600;
              color: #C3272B;
            }

            :deep(em) {
              font-style: italic;
              color: #5A4A42;
            }

            :deep(ul), :deep(ol) {
              margin: 12px 0;
              padding-left: 24px;

              li {
                margin: 6px 0;
                line-height: 1.8;
              }
            }

            :deep(blockquote) {
              margin: 16px 0;
              padding: 12px 16px;
              background-color: #FFF8E8;
              border-left: 4px solid #C3272B;
              border-radius: 4px;
              color: #5A4A42;

              p {
                margin: 0;
              }
            }

            :deep(code) {
              padding: 2px 6px;
              background-color: #FFF8E8;
              border-radius: 4px;
              font-family: 'Consolas', 'Monaco', monospace;
              font-size: 14px;
              color: #C3272B;
            }

            :deep(pre) {
              margin: 16px 0;
              padding: 16px;
              background-color: #FFF8E8;
              border-radius: 8px;
              overflow-x: auto;
              border: 1px solid #FFEFD5;

              code {
                background: none;
                padding: 0;
                color: #5A4A42;
              }
            }

            :deep(a) {
              color: #C3272B;
              text-decoration: none;
              border-bottom: 1px solid transparent;
              transition: all 0.2s;

              &:hover {
                border-bottom-color: #C3272B;
              }
            }

            :deep(img) {
              max-width: 100%;
              height: auto;
              border-radius: 8px;
              margin: 12px 0;
            }

            :deep(hr) {
              margin: 24px 0;
              border: none;
              border-top: 2px solid #FFEFD5;
            }

            :deep(table) {
              width: 100%;
              margin: 16px 0;
              border-collapse: collapse;

              th, td {
                padding: 10px 12px;
                border: 1px solid #FFEFD5;
                text-align: left;
              }

              th {
                background-color: #FFF8E8;
                font-weight: 600;
                color: #303133;
              }

              tr:nth-child(even) {
                background-color: #FFFBF8;
              }
            }
          }
        }
      }
    }
  }
}

// 动画定义
@keyframes float {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-10px);
  }
}

@keyframes pulse {
  0%, 100% {
    opacity: 0.3;
    transform: scale(1);
  }
  50% {
    opacity: 0.5;
    transform: scale(1.1);
  }
}

// 响应式适配
@media (max-width: 1200px) {
  .write-help-container {
    .main-content {
      grid-template-columns: 1fr;

      .preview-section {
        position: static;
      }
    }
  }
}

@media (max-width: 768px) {
  .write-help-container {
    .header-decoration {
      padding: 30px 24px;

      .decoration-content {
        .icon-wrapper {
          width: 60px;
          height: 60px;
        }

        .title-section {
          .page-title {
            font-size: 24px;
          }

          .page-subtitle {
            font-size: 14px;
          }
        }
      }
    }

    .main-content {
      padding: 20px;

      .form-section {
        .help-form {
          padding: 24px;
        }
      }
    }
  }
}
</style>
