@echo off
echo Starting Aura Admin Panel...

start "Aura Backend" cmd /k "cd /d "%~dp0backend\api" && python -m uvicorn app.main:app --host 0.0.0.0 --port 3001"

start "Aura Frontend" cmd /k "cd /d "%~dp0web-admin" && npm run dev"

echo Backend: http://localhost:3001
echo Frontend: http://localhost:5173
