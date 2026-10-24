// src/utils/500.js
import { ElNotification } from 'element-plus'

/**
 * 显示 500 服务器错误弹窗
 * @param {string} message - 错误信息，可选，默认显示通用提示
 */
export const show500Error = (message = '出现这个错误的话那么大概率是芜师傅的后端又炸了... (ó﹏ò｡)  如果可以的话请在反馈页面反馈该错误的详细截图 非常感谢･ﾟ･(｡>ω<｡)･ﾟ･') => {
  ElNotification({
    title: '啊哦...',
    message: message,
    type: 'error',
    duration: 10000, // 10s关闭
    // position: 'top-right', // 弹窗位置
    showClose: true, // 显示关闭按钮
    dangerouslyUseHTMLString: true, // 允许使用 HTML
  })
}

/**
 * 显示自定义错误弹窗
 * @param {Object} options - 配置选项
 * @param {string} options.title - 标题
 * @param {string} options.message - 错误信息
 * @param {number} options.duration - 显示时间（毫秒），0 表示不自动关闭
 * @param {string} options.position - 位置：top-right / top-left / bottom-right / bottom-left
 */
// export const showErrorNotification = (options = {}) => {
//   const {
//     title = '错误',
//     message = '发生未知错误',
//     duration = 0,
//     position = 'top-right',
//   } = options
//
//   ElNotification({
//     title,
//     message,
//     type: 'error',
//     duration,
//     position,
//     showClose: true,
//   })
// }
