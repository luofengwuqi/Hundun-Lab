/**
 * 临时验收构建（沙盒里 vite 因 4GB 虚存上限起不来）。
 * esbuild + @vue/compiler-sfc：@/ 别名、省略扩展名、目录 index、import.meta.env 替换、
 * SFC compileScript/Template/Style 拼装 + scoped 注入。
 * 产物：/tmp/hdlab-dist/{bundle.js,bundle.css,index.html,...public}
 */
import * as esbuild from 'esbuild'
import { parse, compileScript, compileTemplate, compileStyle } from '@vue/compiler-sfc'
import { readFileSync, writeFileSync, mkdirSync, cpSync, existsSync } from 'node:fs'
import { createHash } from 'node:crypto'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const root = path.dirname(fileURLToPath(import.meta.url))
const srcDir = path.join(root, 'src')
const outDir = '/tmp/hdlab-dist'

mkdirSync(outDir, { recursive: true })

function sfcId(file) {
  return createHash('sha256').update(file).digest('hex').slice(0, 8)
}

const sfcPlugin = {
  name: 'sfc',
  setup(build) {
    // 统一解析：@/ 别名与相对路径都要做「省略扩展名 + 目录 index」兜底
    const resolveLocal = (requestPath, importer) => {
      const base = requestPath.startsWith('@/')
        ? path.join(srcDir, requestPath.slice(2))
        : path.resolve(path.dirname(importer), requestPath)
      const candidates = []
      if (path.extname(base)) candidates.push(base)
      else {
        candidates.push(`${base}.js`, `${base}.vue`, `${base}.css`, path.join(base, 'index.js'))
      }
      for (const c of candidates) {
        if (existsSync(c)) return c
      }
      return null
    }

    build.onResolve({ filter: /^@\// }, (args) => {
      const resolved = resolveLocal(args.path, args.importer)
      if (resolved) return { path: resolved }
      return { errors: [{ text: `cannot resolve ${args.path}` }] }
    })

    build.onResolve({ filter: /^\.\.?\// }, (args) => {
      const resolved = resolveLocal(args.path, args.importer)
      if (resolved) return { path: resolved }
      return { errors: [{ text: `cannot resolve ${args.path} from ${args.importer}` }] }
    })

    build.onLoad({ filter: /\.vue$/ }, async (args) => {
      const source = readFileSync(args.path, 'utf8')
      const filename = path.relative(root, args.path)
      const id = sfcId(filename)
      const { descriptor, errors } = parse(source, { filename: args.path })
      if (errors.length) {
        return { errors: errors.map((e) => ({ text: `${e.message} (${filename})` })) }
      }

      const script = compileScript(descriptor, { id, genDefaultAs: '_sfc_main' })
      let code = script.content

      const tpl = compileTemplate({
        source: descriptor.template?.content || '<template><div/></template>',
        filename: args.path,
        id,
        compilerOptions: { mode: 'module', bindingMetadata: script.bindings },
      })
      if (tpl.errors.length) {
        return { errors: tpl.errors.map((e) => ({ text: String(e) })) }
      }
      code += `\n${tpl.code}\n_sfc_main.render = render;\nexport default _sfc_main;\n`

      let css = ''
      let hasScoped = false
      for (const style of descriptor.styles) {
        if (style.scoped) hasScoped = true
        const result = compileStyle({
          source: style.content,
          filename: args.path,
          id: `data-v-${id}`,
          scoped: style.scoped,
        })
        css += result.css
      }
      if (hasScoped) code += `\n_sfc_main.__scopeId = 'data-v-${id}';\n`
      if (css) {
        code += `;(function(){var s=document.createElement('style');s.textContent=${JSON.stringify(
          css,
        )};document.head.appendChild(s)})();\n`
      }
      return { contents: code, loader: 'js' }
    })

    // esbuild 不认 import.meta.env；统一替换成生产值
    build.onLoad({ filter: /\.(js|mjs)$/ }, async (args) => {
      let contents = readFileSync(args.path, 'utf8')
      if (contents.includes('import.meta.env')) {
        contents = contents
          .replace(/import\.meta\.env\.BASE_URL/g, JSON.stringify('/'))
          .replace(/import\.meta\.env\.MODE/g, JSON.stringify('production'))
          .replace(/import\.meta\.env\.DEV/g, 'false')
          .replace(/import\.meta\.env\.PROD/g, 'true')
      }
      return { contents, loader: 'js' }
    })
  },
}

await esbuild.build({
  entryPoints: [path.join(srcDir, 'main.js')],
  bundle: true,
  format: 'esm',
  target: 'es2020',
  platform: 'browser',
  mainFields: ['browser', 'module', 'main'],
  outfile: path.join(outDir, 'bundle.js'),
  minify: false,
  plugins: [sfcPlugin],
  alias: { axios: '/tmp/axios-stub.mjs' },
  define: {
    __VUE_OPTIONS_API__: 'true',
    __VUE_PROD_DEVTOOLS__: 'false',
    __VUE_PROD_HYDRATION_MISMATCH_DETAILS__: 'false',
  },
  logLevel: 'warning',
})

console.log('build ok')

// dist/index.html：复用源 index.html 的内联主题脚本，替换入口引用
const html = readFileSync(path.join(root, 'index.html'), 'utf8').replace('/src/main.js', '/bundle.js')
writeFileSync(path.join(outDir, 'index.html'), html)

if (existsSync(path.join(root, 'public'))) {
  cpSync(path.join(root, 'public'), outDir, { recursive: true })
}
console.log('dist ready:', outDir)
