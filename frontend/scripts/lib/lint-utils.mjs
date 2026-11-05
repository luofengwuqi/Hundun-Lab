/**
 * 两个 check 脚本的公共件：文件遍历、注释剥离、基线读写。
 *
 * 基线（lint-baseline.json）存在的原因：
 * M0c 的「删」之前，旧层里已有 508 处色值字面量、463 行中文。直接开严格模式
 * 会让 lint 从第一天就红着，红着的 lint 等于没有 lint。所以改为棘轮：
 * 基线锁住当前每一处违规，**只许减不许增**；谁加了新违规谁就被拦下。
 * 删完旧层、换完肤以后基线自然归零，`--update` 一次就变成严格模式。
 */
import { readFileSync, writeFileSync, existsSync, readdirSync, statSync } from 'node:fs'
import { join, relative, dirname, sep } from 'node:path'
import { fileURLToPath } from 'node:url'

export const FRONTEND = join(dirname(fileURLToPath(import.meta.url)), '..', '..')
export const SRC = join(FRONTEND, 'src')
export const BASELINE_PATH = join(FRONTEND, 'scripts', 'lint-baseline.json')

/** 递归收集指定后缀的文件，跳过 node_modules 与 dot 目录 */
export function collect(dir, exts, out = []) {
  if (!existsSync(dir)) return out
  for (const name of readdirSync(dir)) {
    if (name === 'node_modules' || name.startsWith('.')) continue
    const full = join(dir, name)
    if (statSync(full).isDirectory()) collect(full, exts, out)
    else if (exts.includes(name.slice(name.lastIndexOf('.')))) out.push(full)
  }
  return out
}

/**
 * 剥掉注释，避免「注释里写了个色值」被误判。
 * 只处理 /* *\/ 、<!-- --> 与行首 //；字符串里的 // （如 https://）不剥，
 * 否则 url 会被截断。
 */
export function stripComments(text) {
  return text
    .replace(/\/\*[\s\S]*?\*\//g, (m) => m.replace(/[^\n]/g, ' '))
    .replace(/<!--[\s\S]*?-->/g, (m) => m.replace(/[^\n]/g, ' '))
    .replace(/(^|\s)\/\/[^\n]*/g, (m) => ' '.repeat(m.length))
}

/** 相对 frontend/ 的稳定路径，用作基线 key（不随机器变化） */
export function keyOf(file) {
  return relative(FRONTEND, file).split(sep).join('/')
}

export function readBaseline() {
  if (!existsSync(BASELINE_PATH)) return {}
  return JSON.parse(readFileSync(BASELINE_PATH, 'utf8'))
}

export function writeBaseline(data) {
  const body = {
    _readme: '自动生成，勿手改。新增违规请修代码，不要改这里；减少违规后运行 --update 收窄基线。',
    ...data,
  }
  writeFileSync(BASELINE_PATH, JSON.stringify(body, null, 2) + '\n', 'utf8')
}

/**
 * 比对实际与基线，打印结论。
 * 返回 true 表示通过。
 */
export function compare(key, actual, baseline) {
  const label = `check-${key}`
  const baselineMap = baseline[key] || {}
  const errors = []
  const shrunk = []

  for (const [file, count] of Object.entries(actual)) {
    const allowed = baselineMap[file] ?? 0
    if (count > allowed) errors.push({ file, count, allowed })
  }
  for (const [file, allowed] of Object.entries(baselineMap)) {
    const count = actual[file] ?? 0
    if (count < allowed) shrunk.push({ file, count, allowed })
  }

  const total = Object.values(actual).reduce((a, b) => a + b, 0)
  const allowedTotal = Object.values(baselineMap).reduce((a, b) => a + b, 0)

  if (errors.length) {
    console.error(`\n✗ ${label}: ${errors.length} 个文件超出基线`)
    for (const e of errors.sort((a, b) => b.count - a.count)) {
      console.error(`    ${e.file}  ${e.count} 处（基线允许 ${e.allowed}）`)
    }
    console.error(`  → 修掉这些，不要改 lint-baseline.json\n`)
    return false
  }

  const tail = allowedTotal > total ? `，较基线减少 ${allowedTotal - total} 处（可 --update 收窄）` : ''
  console.log(`✓ ${label}: ${total} 处 / 基线 ${allowedTotal}${tail}`)
  if (shrunk.length && !process.argv.includes('--quiet')) {
    console.log(`  已改善 ${shrunk.length} 个文件：${shrunk.slice(0, 5).map((s) => s.file).join('、')}${shrunk.length > 5 ? ' …' : ''}`)
  }
  return true
}
