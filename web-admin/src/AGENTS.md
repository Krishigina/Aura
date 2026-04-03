# WEB-ADMIN/SRC KNOWLEDGE

## OVERVIEW
React (Vite) admin panel with context-based auth/session and centralized API client.

## STRUCTURE
```text
web-admin/src/
├── App.jsx                 # Routing + ProtectedRoute
├── main.jsx                # React bootstrap
├── api/index.js            # API wrapper + module APIs
├── context/
│   ├── AuthContext.jsx     # Login/register/session validation
│   ├── ThemeContext.jsx
│   └── ToastContext.jsx
├── pages/                  # Dashboard, Products, Procedures, Content, Users...
└── components/             # Shared UI blocks/layout
```

## WHERE TO LOOK
| Task | Location | Notes |
|---|---|---|
| Login flow bug | `context/AuthContext.jsx` | `login()`, token persistence, `/api/auth/me` check |
| “Server connection” errors | `context/AuthContext.jsx` + backend health | Catch path returns user-facing Russian message |
| Missing auth on requests | `api/index.js` | `getToken()` + Authorization injection in `request()` |
| Wrong backend URL/port | `api/index.js`, `AuthContext.jsx`, env `VITE_API_URL` | Keep base URL and route prefix consistent |
| Route guard issues | `App.jsx` `ProtectedRoute` | `user/loading` behavior |

## CONVENTIONS
- API fallback defaults assume backend on `http://localhost:3002`.
- Token storage keys are fixed: `aura_token`, `aura_user`.
- Most data access should go through `request()` wrapper; direct `fetch` is used in several endpoints (uploads plus some media/read helpers) and may skip auth header injection.
- Role permissions map supports both RU and EN role keys.

## ANTI-PATTERNS
- Do not hardcode `/api` paths inconsistently across context and api wrapper.
- Do not bypass token validation on app boot unless explicit offline behavior is intended.
- Do not add or keep protected direct `fetch` calls without explicit `Authorization` header.

## COMMANDS
```bash
cd web-admin
npm run dev
npm run build
npm run lint
```

## NOTES
- `AuthContext` catches network errors and keeps stale local session in one path; review this behavior before tightening security semantics.
