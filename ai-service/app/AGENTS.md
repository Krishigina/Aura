# AI-SERVICE/APP KNOWLEDGE

## OVERVIEW
Standalone FastAPI AI service for RAG/recommendations/ingredient analysis.

## STRUCTURE
```text
ai-service/app/
├── main.py                 # FastAPI app + router registration
├── api/routes/             # HTTP endpoints by domain
├── core/config.py          # Pydantic settings (host/port/providers)
├── services/               # Business orchestration
├── infrastructure/         # Integrations (vector db, external APIs)
└── models/                 # Schemas
```

## WHERE TO LOOK
| Task | Location | Notes |
|---|---|---|
| Service boot/config issue | `core/config.py`, `main.py` | Env-backed settings; default port 9001 |
| Endpoint wiring | `main.py` includes routers | Prefix `/api/v1` |
| Provider/API key behavior | `core/config.py` + infra/services | `llm_provider`, `openai_api_key` |

## CONVENTIONS
- Settings are centralized via `pydantic-settings` in `core/config.py`.
- CORS is fully open by default.
- Health endpoint is `/health`; root endpoint returns simple service identity payload.

## ANTI-PATTERNS
- Do not change route prefixes in a single router only; keep all `include_router(..., prefix="/api/v1")` aligned.
- Do not embed provider secrets directly in code; use env-backed settings.

## COMMANDS
```bash
cd ai-service
python -m uvicorn app.main:app --host 0.0.0.0 --port 9001 --reload
```

## NOTES
- This module is independent from `backend/api`; do not conflate admin API auth/data routes with AI service routes.
