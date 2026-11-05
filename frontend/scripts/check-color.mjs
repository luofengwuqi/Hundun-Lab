#!/usr/bin/env node
/**
 * check-color —— D057 / D080：色值只能来自 var(--...)，页面与组件的样式里
 * 不许出现十六进制或 rgb()/hsl() 字面量。
 *
 * 扫描范围：src/**\/*.vue 的 <style> 块与 style="..." 内联属性、src/**\/*.css。
 * 豁免：令牌定义文件（*tokens.css / *token.css）——令牌是唯一的合法字面量住处，
 *       由人工评审把关（D080）。
 * 不抓：white/black 这类颜色关键字，它们不会被「主色只允许明度 ±8%」的规则破坏。
 *
 * 用法：
 *   node scripts/check-color.mjs            # 校验（默认，比基线）
 *   node scripts/check-color.mjs --update   # 违规减少后收窄基线
 */
import { readFileSync } from 'node:fs'
import {
  SRC,
  collect,
  stripComments,
  keyOf,
  readBaseline,
  writeBaseline,
  compare,
} from './lib/lint-utils.mjs'

const LITERAL = /#[0-9a-fA-F]{3,8}\b|\b(?:rgba?|hsla?)\(/g
const isTokenFile = (file) => /tokens?\.css$/.test(file)

/** 从 .vue 里取出真正会被渲染的样式片段：<style> 块 + 内联 style 属性 */
function styleSnippets(text) {
  const out = []
  for (const m of text.matchAll(/<style[^>]*>([\s\S]*?)<\/style>/g)) out.push(m[1])
  for (const m of text.matchAll(/\sstyle="([^"]*)"/g)) out.push(m[1])
  return out.join('\n')
}

const files = collect(SRC, ['.vue', '.css']).filter((f) => !isTokenFile(f))
const actual = {}

for (const file of files) {
  const raw = readFileSync(file, 'utf8')
  const body = file.endsWith('.vue') ? styleSnippets(raw) : raw
  const hits = stripComments(body).match(LITERAL)
  if (hits) actual[keyOf(file)] = hits.length
}

const baseline = readBaseline()
if (process.argv.includes('--update')) {
  writeBaseline({ ...baseline, color: actual })
  const total = Object.values(actual).reduce((a, b) => a + b, 0)
  console.log(`✓ check-color: 基线已更新为 ${total} 处 / ${Object.keys(actual).length} 个文件`)
  process.exit(0)
}

process.exit(compare('color', actual, baseline) ? 0 : 1)
