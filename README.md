# REELVAULT

REELVAULT is a local, account-based media archive for films, TV series, anime films and anime series. It keeps the existing OOP model (`Media` → `Movie` / `Series`, with `Series` owning `Episode` records) and adds a Next.js interface, Spring Security sessions, and live TMDB/AniList catalogue adapters.

## Requirements

- Java 17 or newer
- Node.js 20 or newer and npm
- Internet access for the first Maven dependency download and live catalogue requests
- A TMDB Read Access Token for live films and TV search. Anime catalogues use AniList without an API key.

## Configure and run on Windows

1. `backend/run-backend.ps1` reads the allow-listed values from `backend/.env` and can also pick up a TMDB token saved in your Windows user environment. If neither is configured, add your TMDB Read Access Token to `backend/.env` for film and TV catalogues. The token is read only by the Java backend; it is never a `NEXT_PUBLIC_*` value, and the launcher does not print it.
2. Open one PowerShell window:

   ```powershell
   Set-Location D:\reelvault\backend
   .\run-backend.ps1
   ```

   The API runs at `http://localhost:8081`. AniList anime search works without a token. When TMDB is not configured, film/TV requests show a clear configuration error rather than demo titles.
3. Open a second PowerShell window:

   ```powershell
   Set-Location D:\reelvault\frontend
   Copy-Item .env.example .env.local
   npm install
   npm run dev
   ```

4. Visit `http://localhost:3000`, create an account, sign in, browse a category, and add titles to your private library.

For a production frontend check, run `npm run lint` (TypeScript check) and `npm run build` from `frontend`. `npm run typecheck` is an explicit alias for the TypeScript check.

## Data and schema safety

The local SQLite file is `backend/data/reelvault.db` and is ignored by Git. Existing rows are retained: the account/catalogue schema changes are additive (`owner_id`, provider metadata and the new accounts table). The existing single-user library is claimed by the first account registered after this update; later accounts cannot see it. Demo seed data has been removed. Do not replace the database file to troubleshoot.

The current local schema uses Hibernate's non-destructive `update` mode. Review and snapshot the DB before any future schema migration. For a future PostgreSQL deployment, configure `REELVAULT_DATABASE_URL`, `REELVAULT_DATABASE_DRIVER=org.postgresql.Driver`, and `REELVAULT_HIBERNATE_DIALECT=org.hibernate.dialect.PostgreSQLDialect`; PostgreSQL's JDBC driver is included. A persistent managed database is required in production; do not rely on an ephemeral SQLite filesystem.

## Tests

```powershell
Set-Location D:\reelvault\backend
.\mvnw.cmd test
```

The integration tests cover persistence, filtering, status/rating/review, series episode progress and cross-account privacy. Tests use isolated in-memory SQLite and never write the local library database.

## API overview

- Public provider catalogue: `GET /api/catalogue/{MOVIE|TV_SERIES|ANIME_MOVIE|ANIME_SERIES}?search=&page=`
- Public details: `GET /api/catalogue/{category}/{provider-id}`
- Accounts: `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth/me`, `POST /api/auth/logout`
- Private library: `GET /api/dashboard`, `/api/media`, and scoped create/import/update/delete routes
- Series progress: `POST /api/series/{id}/episodes`, `PATCH /api/episodes/{id}/watched`

All unsafe browser requests use the session's CSRF token. CORS origins, cookie security, database URL/driver/dialect and TMDB credentials are environment-configurable. Account passwords are BCrypt-hashed; browser-supplied user IDs are never used for authorization.

## Attribution

This product uses the TMDB API but is not endorsed or certified by TMDB. TMDB data and images are attributed in the app's Credits area; anime data is provided by AniList. Follow [TMDB's API FAQ and branding requirements](https://developer.themoviedb.org/docs/faq) if you change the attribution UI.

## Preparing a private repository

The root `.gitignore` excludes `.env*` except `.env.example`, build outputs, databases, logs and backups. This project directory is not initialized as a Git repository by this task. `.gitignore` cannot remove secrets from earlier commits if a repository is created later; review history and files before publishing.
