import { ref } from 'vue'

/**
 * 深浅主题（D081 / D087）。
 *
 * 首帧的主题不在这里决定——index.html 的内联脚本在样式表之前就把
 * data-theme 写到了 <html> 上，否则深色用户每次刷新都会被闪一下。
 * 这里只负责「读当前值」和「用户点了之后改值 + 存盘」。
 *
 * 值只认 light / dark（D081），不认桌面端的 hun-tun-* 命名。
 */
const THEME_STORAGE_KEY = 'hundunlab.theme'

const root = document.documentElement

/** 当前主题。初值直接取内联脚本已经落好的那个属性，不重新猜。 */
const theme = ref(root.dataset.theme === 'dark' ? 'dark' : 'light')

function setTheme(next) {
  theme.value = next
  root.dataset.theme = next
  // 让原生控件（滚动条、输入框、表单内建 UI）跟着换，不然深色页里会冒出白条
  root.style.colorScheme = next
  try {
    localStorage.setItem(THEME_STORAGE_KEY, next)
  } catch {
    // 隐私模式下写不进去就不写，本次会话照样生效
  }
}

export function useTheme() {
  return {
    theme,
    setTheme,
    toggleTheme: () => setTheme(theme.value === 'dark' ? 'light' : 'dark'),
  }
}
