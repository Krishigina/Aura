# PROJECT KNOWLEDGE BASE

**Generated:** 2026-04-03 13:00 (Europe/Moscow)  
**Commit:** 8989274  
**Branch:** feature/web-admin-panel

## OVERVIEW
Aura is a multi-app workspace: FastAPI admin backend, React admin frontend, AI FastAPI service, Kotlin mobile app, plus an embedded `oh-my-openagent` codebase.

## STRUCTURE
```text
./
├── backend/api/        # Main operational Admin API (FastAPI + asyncpg)
├── web-admin/src/      # React admin UI and API client wiring
├── ai-service/app/     # Separate AI microservice (RAG/recommendations)
├── mobile/             # Kotlin Multiplatform app + shared module
├── oh-my-openagent/    # Vendor-like external codebase with own AGENTS hierarchy
├── start.bat           # Canonical local startup orchestrator
└── docker-compose.yml  # Infra containers (Postgres/Redis/Weaviate)
```

## WHERE TO LOOK
| Task | Location | Notes |
|---|---|---|
| Admin API auth/data behavior | `backend/api/main.py` | Single large file; JWT + DB init + CRUD |
| Admin UI login/session bugs | `web-admin/src/context/AuthContext.jsx` | Session persistence + `/api/auth/me` validation |
| Frontend request failures | `web-admin/src/api/index.js` | Default API URL + auth header injection |
| Full local startup issues | `start.bat` | Starts Docker Postgres + backend + frontend |
| AI endpoints / model behavior | `ai-service/app/api/**`, `ai-service/app/main.py` | Separate service, port 9001 |
| Mobile UI/navigation/data flow | `mobile/shared/src/commonMain/kotlin/**` | Most Kotlin code is in `shared` |

## CONVENTIONS (PROJECT-SPECIFIC)
- Prefer running local admin workflow via `start.bat` in repo root (Docker Postgres + backend + frontend).
- Active admin backend defaults: `localhost:3002`; frontend defaults: `localhost:5173`.
- Frontend API base in `web-admin` is env-driven (`VITE_API_URL`) with fallback to `http://localhost:3002/api`.
- `oh-my-openagent/` already contains deep AGENTS docs; do not duplicate them at root.
- Repository has many ad-hoc debug/test scripts at root (`check_*.py`, `test_*.py`); treat them as diagnostics utilities, not production modules.

## ANTI-PATTERNS (THIS PROJECT)
- Do not assume README service ports are current; verify against `start.bat` and active module configs.
- Do not mix Docker DB port mappings with backend defaults blindly; confirm `DB_PORT` and compose/run mapping first.
- Do not add AGENTS files under directories already covered by strong existing hierarchy (`oh-my-openagent/**`) unless clearly missing.

## UNIQUE STYLES
- Heavy practical behavior is centralized in `backend/api/main.py` instead of split routers/services.
- Mixed architecture state: historical microservice layout in docs vs operational single FastAPI admin backend for current workflow.

## COMMANDS
```bash
# Local admin workflow (recommended)
start.bat

# Backend only
run_backend.bat

# Frontend only
run_frontend.bat

# Infra only
docker-compose up -d
```

## AUTO-START RULE
- If user says "запусти", "запусти проект", "start", "run" — **immediately run `start.bat`** without asking.
- `start.bat` handles everything: Docker Postgres, backend, frontend, dependency checks.

## NOTES
- If login shows “Ошибка соединения с сервером”, first verify backend is listening on 3002 and DB container is healthy.
- `backend/api/main.py` boot may fail during init if DB schema/tables are incomplete; check startup traceback before frontend debugging.
