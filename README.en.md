<img src="frontend/src/assets/img/common/biglogo.png" alt="Hundun Lab" width="320">

# Hundun Lab

The official site for the Hundun software family — project introductions, installer downloads, and a plugin community.

[![JDK](https://img.shields.io/badge/JDK-17-blue?style=flat-square)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?style=flat-square)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3-42B883?style=flat-square)](https://vuejs.org/)
[![License](https://img.shields.io/badge/License-MulanPSL--2.0-red?style=flat-square)](LICENSE)

Nothing about a product is hard-coded: software, features, supported platforms, releases and installers all come from the database.
The design goal is that a new version is just data entered in the admin console, not a code change and redeploy
(the console itself is not built yet).

The site currently features **HundunDesktop**, a retro handheld-style desktop tool built with Electron 28 and Vue 3.

## Status

| Phase | Scope | Status |
| --- | --- | --- |
| 1 | Public site: home, software list & detail, sign-in, login-gated downloads | Done |
| 1 | Admin console: software & release entry, installer upload, download stats | Not started (no software/release management API yet) |
| 2 | Plugin community: listing & detail, manifest validation on upload, review, stable download URL | Not started (placeholder page) |
| 3 | User center, feedback handling, admin permission hardening | Not started |

## Tech stack

| Layer | Choice |
| --- | --- |
| Backend | Spring Boot 3.5, JDK 17, MyBatis + PageHelper, Redis |
| Auth | JWT (jjwt 0.12), BCrypt, email codes, Redis-backed token revocation |
| Storage | Alibaba Cloud OSS private bucket with presigned URLs |
| Frontend | Vue 3, Vite, Pinia, Vue Router (history mode), vue-i18n |
| Database | MySQL 8.0 |

## Project layout

```
chaos-lab/
├── backend/                      # Spring Boot backend
│   └── src/main/java/com/hundunlab/site/
│       ├── common/               # Response envelope, error codes, context, utils, config
│       ├── framework/            # Token parsing, access decisions, rate limiting
│       └── module/
│           ├── auth/             # Sign-in methods and email codes
│           ├── software/         # Software, features, platforms, releases, assets
│           ├── download/         # Download tickets and records
│           ├── user/             # Profile, avatar upload, feedback, generic uploads
│           └── admin/            # User management, admin invitations (to be rebuilt in phase 3)
├── frontend/                     # Public site
│   └── src/
│       ├── views/site/           # Home, list, detail, sign-in, plugins
│       ├── components/           # Header, footer, software card, auth dialog
│       ├── i18n/                 # zh / en messages with persisted locale
│       ├── api/                  # API wrappers and error-code translation
│       └── stores/               # Pinia session state
├── db/                           # Idempotent schema and seed scripts
└── docs/                         # Skeleton review report (Chinese)
```

> [!NOTE]
> `frontend/src/views/{admin,login,user,help}` and `api/{admin,user,feedback,article}.js` are legacy pages carried over
> from the base project. They are not routed and are not referenced by the build; phase 3 will replace or delete them.

## Getting started

### Prerequisites

- JDK 17+, Maven 3.9+
- MySQL 8.0+, Redis 6+
- Node.js 20.19+ or 22.12+, pnpm 9/10 (npm also works)

### 1. Prepare the database

```bash
mysql -uroot -p -e "CREATE DATABASE hundun_lab CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"
mysql -uroot -p hundun_lab < db/01_schema.sql
mysql -uroot -p hundun_lab < db/02_seed.sql
```

Both scripts are idempotent: running them again is safe and will not overwrite content edited in the admin console.

### 2. Run the backend

All secrets are supplied through environment variables (or a local `backend/src/main/resources/application-secret.yml`, already git-ignored).

| Variable | Required | Default | Notes |
| --- | --- | --- | --- |
| `JWT_SECRET` | Yes | — | At least 32 bytes; the app refuses to start otherwise |
| `DB_URL` | No | local `hundun_lab` | JDBC URL |
| `DB_USERNAME` / `DB_PASSWORD` | Yes | `root` / — | No default password on purpose |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` / `REDIS_DATABASE` | No | `localhost` / `6379` / empty / `0` | Token revocation, email codes, cache |
| `OSS_ENDPOINT` / `OSS_BUCKET` / `OSS_DOMAIN` | For downloads | `oss-cn-beijing.aliyuncs.com` / empty | Private bucket used to sign URLs |
| `ALIYUN_ACCESS_KEY_ID` / `ALIYUN_ACCESS_KEY_SECRET` | For downloads | empty | App still starts; downloads return 503 |
| `MAIL_SMTP_HOST` / `MAIL_SMTP_PORT` / `MAIL_FROM` / `MAIL_AUTH_CODE` | No | `smtp.qq.com` / `587` / empty | Sending is skipped when unset |
| `UPLOAD_MAX_FILE_SIZE` / `UPLOAD_MAX_REQUEST_SIZE` | No | `20MB` / `25MB` | Upload quota for plugin `.zip`; installers go straight to OSS |
| `CORS_ALLOWED_ORIGINS` | No | `http://localhost:5577` | Frontend origin when deployed separately; leave empty for same-origin |
| `TRUST_PROXY_HEADERS` | No | `false` | Only enable behind a trusted reverse proxy, otherwise `X-Forwarded-For` is spoofable |
| `SERVER_PORT` | No | `5757` | |
| `SPRING_PROFILES_ACTIVE` | No | `dev` | Only `dev` loads `application-dev.yml` and the local secret file |
| `JWT_EXPIRATION_MILLIS` | No | `86400000` | Token lifetime, 24 hours by default |

Apart from `JWT_SECRET` and the database password, the defaults are enough to browse and sign in with a password;
email-code sign-in needs SMTP, and handing out installers needs OSS.

```bash
cd backend
JWT_SECRET="<32+ random bytes>" DB_PASSWORD="<password>" mvn spring-boot:run
```

### 3. Run the frontend

```bash
cd frontend
pnpm install          # or npm install
pnpm dev              # http://localhost:5577
```

The dev server proxies `/api` to `http://localhost:5757`, so no CORS setup is needed.

### 4. About accounts

`POST /user/register` always creates a normal user (`users.status = 1`). The repository ships no admin account
and no bootstrap logic — to call `/admin/**`, register an account and manually set its `status` to `2` (admin)
or `3` (super admin). Phase 3 replaces this with account management in the admin console.

### Deployment prerequisites

The frontend hard-codes `/api` as its base URL (`baseURL` in `src/api/http.js`). In production either deploy it
same-origin and have Nginx reverse-proxy `/api` to port 5757, or register the frontend origin in
`CORS_ALLOWED_ORIGINS` and point the frontend at the API domain. No code change is required either way.
Nothing is deployed yet.

## Design notes

### Auth: parse first, decide later

`TokenResolveFilter` only parses the bearer token and puts it into the request context; it never makes an access decision.
`AccessControlInterceptor` is the single gate: deny by default, open only where `@PublicApi` is present, and check roles again
for the `/admin` prefix and `@RequireRole`. Anonymous requests get `null` from `getCurrentId()` — never a synthetic user.

### Downloads: ticket after sign-in, bytes straight from OSS

1. Anonymous visitors can browse every public page;
2. Clicking download while signed out opens the sign-in dialog and remembers the intended installer;
3. After sign-in the client calls `POST /download/ticket`;
4. The server verifies the software is published, the release is published, and the asset belongs to that release,
   then writes the download record, increments the counter, and returns a 5-minute presigned OSS URL;
5. The browser navigates straight to OSS — installers never pass through the app server, so large files cannot tie it up.

> [!IMPORTANT]
> The download record and the `download_count` increment share one transaction. IP addresses are stored as SHA-256 digests
> and User-Agent strings are truncated to 255 characters; no plaintext is kept.

### Bilingual: codes in the database, copy in the frontend

- Content fields are paired (`name_zh` / `name_en`); the API returns both, the client picks one, and switching languages needs no refetch;
- All UI copy lives in `frontend/src/i18n` — components never hard-code Chinese;
- The backend returns stable `errorCode` values only and the client translates them, so API language never leaks into the UI;
- The chosen locale is persisted in localStorage and applied to `<html lang>`.

## API overview

Every response uses `{ code, errorCode, message, data }`. HTTP status conveys the category; business failures are identified by `errorCode`.

### Core site flow

| Method | Path | Auth | Purpose |
| --- | --- | --- | --- |
| GET | `/software` | Public | Software list with latest release, platforms, download count |
| GET | `/software/{slug}` | Public | Software detail: features, screenshots, releases, assets |
| POST | `/download/ticket` | Signed in | Exchange for a download URL (5 min, 30 requests/min) |
| GET | `/download/mine` | Signed in | My download history (`limit` defaults to 20, max 100) |

### Accounts

| Method | Path | Auth | Purpose |
| --- | --- | --- | --- |
| POST | `/user/register` | Public | Register, 5 requests/min |
| POST | `/user/register/admin` | Public | Register an admin with an invite code, 5 requests/min |
| POST | `/user/login/username-password` | Public | Username and password |
| POST | `/user/login/email-password` | Public | Email and password |
| POST | `/user/login/phone-password` | Public | Phone and password |
| POST | `/user/login/email-code/send` | Public | Send an email code (rate-limited) |
| POST | `/user/login/email-code` | Public | Email-code sign-in |
| POST | `/user/login/logout` | Signed in | Sign out and revoke the current token |
| GET | `/user/info` | Signed in | Current profile |
| PUT | `/user/nickname`, `/email`, `/phone`, `/password` | Signed in | Update profile; password changes are limited to 5/min |
| PUT | `/user/avatar` | Signed in | Avatar upload (`multipart`, 10MB per file) |
| DELETE | `/user` | Signed in | Normal users delete themselves with their password; admins can delete in bulk |
| POST | `/files` | Signed in | Generic file upload, 10 requests/min |

### Feedback

| Method | Path | Auth | Purpose |
| --- | --- | --- | --- |
| POST | `/feedback` | Signed in | Submit feedback |
| GET | `/feedback` | Admin | Paginated feedback list |
| PUT | `/feedback/status` | Admin | Update handling status |
| DELETE | `/feedback/handled` | Admin | Clear handled feedback |

### Admin (not wired to the UI yet)

`/admin/**` still has 9 endpoints inherited from the base project: create admin (super admin only), generate invite code,
paginated user list and detail, change status, delete user, operation logs, user count. They are protected by `@RequireRole`,
with the `/admin` path prefix acting as a second role check in the interceptor. The public site has no entry points to them;
phase 3 redesigns them before exposing anything.

## Data and content

- `db/01_schema.sql` is DDL only; `db/02_seed.sql` holds the launch content. Both are idempotent;
- No installers, screenshots, or user rows are committed to the repository;
- New software and releases are meant to be entered through the admin console (not built yet) — data only, no code change;
- Schema changes go into numbered `1x_*.sql` scripts without altering existing column semantics. See [db/README.md](db/README.md).

> [!NOTE]
> The seed holds exactly one software (HundunDesktop), 5 features, 2 platforms and 1 release (`26.2.1024`),
> with no assets and no screenshots. A fresh clone therefore shows **no primary download button** and the
> "no downloadable installer for this release" hint — that is missing data, not a broken page.

## Test and build

```bash
cd backend  && mvn test        # 23 cases: 10 access control, 6 token parsing, 7 rate limiting
cd frontend && pnpm build      # outputs dist/
cd frontend && pnpm lint       # oxlint + eslint
```

The backend tests are plain unit tests with no Spring context, so they need neither MySQL, Redis, nor `JWT_SECRET` —
CI can run them as-is.

## Roadmap

- **Phase 1 (in progress)**: public site done; remaining is the admin console — software and release entry, OSS installer upload, download stats.
- **Phase 2**: plugin community — cards and detail pages, `.zip` upload with manifest validation, review queue, one stable URL per plugin.
- **Phase 3**: user center, feedback handling, admin permission hardening.

## Troubleshooting

> [!WARNING]
> Without OSS configured, downloading returns `DOWNLOAD_STORAGE_UNAVAILABLE` (503). That is intentional: a clear
> "storage unavailable" beats handing out a link that 404s.

- **App fails on startup**: `JWT_SECRET` is missing or shorter than 32 bytes — a hard security requirement, no fallback.
- **`/admin/**` returns 403 (`AUTH_FORBIDDEN`)**: the current account's `status` is not 2 or 3; see "About accounts".
- **Download returns 401 (`AUTH_NOT_LOGGED_IN`)**: the ticket endpoint is not public — sign in first.
- **Vite cannot find rollup after `pnpm install`**: the lockfile is v9 format; use pnpm 9/10. pnpm 12 produces incomplete links.
- **Edits seem ignored**: Vite occasionally caches the old SFC; restart the dev server.

## Documentation

Internal documents are written in Chinese and are **not** mirrored in this README.

- [CHANGELOG.md](CHANGELOG.md) — change log (Chinese)
- [docs/01-项目计划书.md](docs/01-项目计划书.md) — the project plan: milestones M0–M3, scope, and decision log (Chinese, **authoritative**)
- [docs/00-骨架评审报告.md](docs/00-骨架评审报告.md) — skeleton review, change list, and design decisions (Chinese, frozen)
- [db/README.md](db/README.md) — script order and hard rules (Chinese)
- [frontend/README.md](frontend/README.md) — frontend project notes (Chinese)

Released under the [Mulan Permissive Software License v2](LICENSE).
