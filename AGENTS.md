# AGENTS.md

Hundun Lab 官网仓库：`backend/`（Spring Boot 3.5 + JDK 17 + MyBatis）、`frontend/`（Vue 3 + Vite）。
产品范围与验收见 `docs/prd/03-PRD-DRAFT.md`；当前状态与挂账见 `docs/prd/00-CONTEXT.md`；
决策与替代链见 `docs/prd/02-DECISIONS-AND-QUESTIONS.md`。本文件只管工程操作与提交推送，
不复述需求、字段与接口契约。

## 包管理与命令

- 前端用 **pnpm**（存在 `frontend/pnpm-lock.yaml`），不要用 npm / yarn 安装依赖。
- 后端用 **Maven**（`backend/pom.xml`）。

| 任务 | 命令 |
| --- | --- |
| 前端依赖 | `cd frontend && pnpm install` |
| 前端开发 | `cd frontend && pnpm dev` |
| 前端全量检查 | `cd frontend && pnpm lint` |
| 前端构建 | `cd frontend && pnpm build` |
| 后端测试 | `cd backend && mvn test` |
| 后端打包 | `cd backend && mvn package -DskipTests` |

改动前端代码或文案后必须跑 `pnpm lint`（oxlint → prettier → check-i18n → check-color，后两项是严格模式，基线为 0）。

## 提交与双推

- 提交前缀固定：`feat` / `fix` / `chore` / `docs` / `style` / `refactor` / `data`；涉及库结构的提交必须带增量脚本（D069）。
- **完成一个阶段性进展后，直接提交并推送到两个远端，不必逐次征询**：

```bash
git push origin master && git push gitee master
```

- 实际远端以 `git remote -v` 为准：`origin` = GitHub `Hundun-Lab`，`gitee` = Gitee `chaos-lab`。
  注意这与 D068 里「`origin` 是 gitee」的旧表述相反，**不要照抄旧命令**（`git push github …` 会报找不到 remote）。
- 推送后核对两端 `git log` 指向同一提交；一端失败要修好重推，**不得只推一端就宣布完成**（D022、D068）。
- 「阶段性进展」指：一个功能跑通 / 一批文档成稿 / 一个缺陷闭环，而不是每存一次文件。
- 仍然禁止未经确认的操作：`push --force`、改写历史、删除远端分支、`reset --hard` 丢弃工作区。

## 改动边界

- 色值只允许写在 `frontend/src/styles/tokens.css`；组件与页面禁 `#hex` / `rgba()` 字面量（D057、D080）。
- 界面文案走 vue-i18n，中英键必须齐全（D058）。
- 管理接口放在各业务模块的 `AdminXxxController`，不要塞进 `admin` 模块（D047）。
- 不要顺手重构、全仓格式化或升级依赖；只改当前任务需要的文件。
- 规划中的能力不得当作已实现：`/admin` 目前只有概览与用户管理两页，F12–F15 内容运营模块尚未接入；
  真实进度以 `docs/prd/00-CONTEXT.md` 为准。

## 文档指引

| 需要 | 文件 |
| --- | --- |
| 当前状态 / 挂账 | `docs/prd/00-CONTEXT.md` |
| 决策与未决问题 | `docs/prd/02-DECISIONS-AND-QUESTIONS.md` |
| 需求与验收 | `docs/prd/03-PRD-DRAFT.md` |
| 术语与业务不变量 | `docs/02-术语与业务规则.md` |
| 字段 / 接口契约 | `docs/03-数据字典.md`、`docs/04-接口文档.md` |
