<img src="frontend/src/assets/img/common/biglogo.png" alt="Hundun Lab" width="320">

# Hundun Lab

The official site for the Hundun software family — project introductions, installer downloads, and a plugin community.

[![JDK](https://img.shields.io/badge/JDK-17-blue?style=flat-square)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?style=flat-square)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3-42B883?style=flat-square)](https://vuejs.org/)
[![License](https://img.shields.io/badge/License-MulanPSL--2.0-red?style=flat-square)](LICENSE)

Nothing about a product is hard-coded: software, features, supported platforms, releases and installers all come from the database.
Shipping a new version means entering data in the admin console, not editing code and redeploying.

The site currently features **HundunDesktop**, a desktop card app built with Electron 28 and Vue 3.

## Status

| Phase | Scope | Status |
| --- | --- | --- |
| 1 | Public site: home, software list & detail, sign-in, login-gated downloads | Done |
| 1 | Admin console: software & release entry, installer upload, download stats | In progress |
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
│           ├── user/             # Profile and feedback
│           └── admin/            # User management, admin invitations
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
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | No | `localhost` / `6379` / empty | Token revocation, email codes, cache |
| `OSS_ENDPOINT` / `OSS_BUCKET` / `OSS_DOMAIN` | For downloads | empty | Private bucket used to sign URLs |
| `ALIYUN_ACCESS_KEY_ID` / `ALIYUN_ACCESS_KEY_SECRET` | For downloads | empty | App still starts; downloads return 503 |
| `MAIL_SMTP_HOST` / `MAIL_FROM` / `MAIL_AUTH_CODE` | No | `smtp.qq.com` / empty | Sending is skipped when unset |
| `CORS_ALLOWED_ORIGINS` | No | `http://localhost:5577` | Leave empty for same-origin deployment |
| `SERVER_PORT` | No | `5757` | |

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

| Method | Path | Auth | Purpose |
| --- | --- | --- | --- |
| GET | `/software` | Public | Software list with latest release, platforms, download count |
| GET | `/software/{slug}` | Public | Software detail: features, screenshots, releases, assets |
| POST | `/download/ticket` | Signed in | Exchange for an installer download URL |
| GET | `/download/mine` | Signed in | My download history |
| POST | `/user/register` | Public | Register |
| POST | `/user/login/username-password` | Public | Password sign-in |
| POST | `/user/login/email-code` / `/email-code/send` | Public | Email-code sign-in / send code |
| POST | `/user/login/logout` | Signed in | Sign out and revoke the token |
| GET | `/user/info` | Signed in | Current profile |
| POST | `/feedback` | Signed in | Submit feedback |
| GET | `/feedback`, `PUT /feedback/status` | Admin | List and handle feedback |

Every response uses `{ code, errorCode, message, data }`. HTTP status conveys the category; business failures are identified by `errorCode`.

## Data and content

- `db/01_schema.sql` is DDL only; `db/02_seed.sql` holds the launch content. Both are idempotent;
- No installers, screenshots, or user rows are committed to the repository;
- New software and releases are entered through the admin console (in progress) — no code change needed;
- Schema changes go into numbered `1x_*.sql` scripts without altering existing column semantics. See [db/README.md](db/README.md).

## Test and build

```bash
cd backend  && mvn test        # 23 cases covering access control, token parsing, rate limiting
cd frontend && pnpm build      # outputs dist/
```

## Roadmap

- **Phase 1 (in progress)**: public site done; admin console for software/release entry, OSS installer upload, download stats.
- **Phase 2**: plugin community — cards and detail pages, `.zip` upload with manifest validation, review queue, one stable URL per plugin.
- **Phase 3**: user center, feedback handling, admin permission hardening.

## Troubleshooting

> [!WARNING]
> Without OSS configured, downloading returns `DOWNLOAD_STORAGE_UNAVAILABLE` (503). That is intentional: a clear
> "storage unavailable" beats handing out a link that 404s.

- **App fails on startup**: `JWT_SECRET` is missing or shorter than 32 bytes — a hard security requirement, no fallback.
- **Vite cannot find rollup after `pnpm install`**: the lockfile is v9 format; use pnpm 9/10. pnpm 12 produces incomplete links.
- **Edits seem ignored**: Vite occasionally caches the old SFC; restart the dev server.

## Documentation

- [docs/00-骨架评审报告.md](docs/00-骨架评审报告.md) — skeleton review, change list, and design decisions (Chinese)
- [db/README.md](db/README.md) — script order and hard rules (Chinese)
- [frontend/README.md](frontend/README.md) — frontend project notes

Released under the [Mulan Permissive Software License v2](LICENSE).
