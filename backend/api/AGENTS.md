# BACKEND/API KNOWLEDGE

## OVERVIEW
Operational admin backend: FastAPI + asyncpg in one main module.

## STRUCTURE
```text
backend/api/
├── main.py                 # API routes, auth, DB init, seed logic
├── requirements.txt        # Python deps
├── start.bat               # Local backend start helper
├── Dockerfile
├── test_db*.py             # DB diagnostic scripts
└── {media folders}/        # Uploaded content assets
```

## WHERE TO LOOK
| Task | Location | Notes |
|---|---|---|
| Startup/DB connection failure | `main.py` -> `lifespan()` | Retry loop + env-based DB config |
| Schema/seed issues | `main.py` -> `init_db()` | Table creation + default dictionaries + demo users |
| JWT/login/register/me | `main.py` auth models + `/api/auth/*` routes | `python-jose` + bcrypt/passlib |
| Permission/user resolution | `get_current_user()` | Decodes token, fetches DB user |
| API endpoint changes | Route decorators in `main.py` | Most resources are in same file |

## CONVENTIONS
- DB config comes from `DB_HOST/DB_PORT/DB_NAME/DB_USER/DB_PASSWORD`; in `main.py` defaults are `localhost:5433`, DB `aura`, user `aura_user`.
- Local `start.bat` starts Postgres on `5432`; set `DB_PORT=5432` when running backend via that script path.
- `DATABASE_URL` variable exists but pool creation path uses explicit `DB_*` env vars in `lifespan()`.
- JWT defaults are hardcoded fallback values; production should override via env.
- `init_db()` performs both table creation and seed population; treat as boot-critical path.
- Error payloads for auth are user-facing Russian strings; preserve UX consistency when changing auth flows.

## ANTI-PATTERNS
- Avoid adding schema changes only in ad-hoc SQL scripts; keep boot-time compatibility in `init_db()`.
- Avoid silently swallowing DB exceptions in startup logic without emitting clear logs.
- Avoid splitting routes into new modules without updating startup imports/tests; current architecture assumes single-file centralization.

## COMMANDS
```bash
# Start backend from repo root
run_backend.bat

# Start directly
cd backend/api
python main.py
```

## NOTES
- If backend starts on wrong port, verify `uvicorn.run(..., port=3002)` near file bottom.
- `backend/api/start.bat` still runs uvicorn on 3001 and should be treated as legacy unless intentionally updated.
- If auth works but protected routes fail, check Authorization header path from frontend (`web-admin/src/api/index.js`).
