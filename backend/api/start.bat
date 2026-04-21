@echo off
setlocal EnableExtensions

set "ROOT=%~dp0..\.."

if /I "%~1"=="--backend-only" (
    cd /d "%~dp0"
    python -m uvicorn main:app --host 0.0.0.0 --port 3001 --reload
    pause
    exit /b %errorlevel%
)

if exist "%ROOT%\start.bat" (
    echo [INFO] Redirecting to root start.bat (backend + frontend + postgres)...
    call "%ROOT%\start.bat"
    exit /b %errorlevel%
)

echo [ERROR] Root start.bat not found at "%ROOT%\start.bat"
pause
exit /b 1
