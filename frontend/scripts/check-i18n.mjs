#!/usr/bin/env node
/**
 * check-i18n —— D058：界面文案一律走 vue-i18n，组件的模板与脚本里
 * 不许出现中文/日文/韩文字面量，否则切语言时会漏掉半句。
 *
 * 扫描范围：src/**\/*.vue。
 * 豁免：src/i18n/（语言包本来就该是中文）、注释（注释不是给用户看的）。
 * 判据：一行里出现 CJK 统一表意文字或 CJK 标点，即记一处。
 *       `t('...')` 的键是 ASCII，因此正常写法不会被误伤。
 *
 * 用法：
 *   node scripts/check-i18n.mjs            # 校验（默认，比基线）
 *   node scripts/check-i18n.mjs --update   # 违规减少后收窄基线
 */
import { readFileSync } from 'node:fs'
import { relative, sep } from 'node:path'
import {
  FRONTEND,
  SRC,
  collect,
  stripComments,
  keyOf,
  readBaseline,
  writeBaseline,
  compare,
} from './lib/lint-utils.mjs'

// CJK 统一表意文字 + 中日韩标点（，。「」·等）
const CJK = /[\u4e00-\u9fff\u3000-\u303f\uff01-\uff60]/

const files = collect(SRC, ['.vue']).filter((f) => {
  const rel = relative(FRONTEND, f).split(sep).join('/')
  return !rel.startsWith('src/i18n/')
})

const actual = {}

for (const file of files) {
  const lines = stripComments(readFileSync(file, 'utf8')).split('\n')
  const count = lines.filter((line) => CJK.test(line)).length
  if (count) actual[keyOf(file)] = count
}

const baseline = readBaseline()
if (process.argv.includes('--update')) {
  writeBaseline({ ...baseline, i18n: actual })
  const total = Object.values(actual).reduce((a, b) => a + b, 0)
  console.log(`✓ check-i18n: 基线已更新为 ${total} 行 / ${Object.keys(actual).length} 个文件`)
  process.exit(0)
}

process.exit(compare('i18n', actual, baseline) ? 0 : 1)
