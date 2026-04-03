@echo off
setlocal enabledelayedexpansion

set "ROOT=%~dp0"
set "BACKEND_DIR=%ROOT%backend\api"
set "FRONTEND_DIR=%ROOT%web-admin"
set "AI_SERVICE_DIR=%ROOT%ai-service"

echo ========================================
echo Starting Aura Full System
echo ========================================
echo.

:: ============================================
:: [1/6] Check prerequisites
:: ============================================
echo [1/6] Checking prerequisites...

docker info >nul 2>&1
if errorlevel 1 (
    echo   [INFO] Docker is not running. Starting Docker Desktop...
    start "" "C:\Program Files\Docker\Docker\Docker Desktop.exe"
    echo   Waiting for Docker to initialize...
    set /a DOCKER_ATTEMPTS=0
    :wait_docker
    set /a DOCKER_ATTEMPTS+=1
    if !DOCKER_ATTEMPTS! gtr 60 (
        echo   [ERROR] Docker failed to start within 2 minutes. Please start Docker Desktop manually.
        pause
        exit /b 1
    )
    timeout /t 2 /nobreak >nul
    docker info >nul 2>&1
    if errorlevel 1 (
        goto :wait_docker
    )
    echo   Docker: started
)
echo   Docker: OK

python --version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Python not found! Install Python 3.13+
    pause
    exit /b 1
)
echo   Python: OK

node --version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Node.js not found! Install Node.js 18+
    pause
    exit /b 1
)
echo   Node.js: OK

echo.

:: ============================================
:: [2/6] Install frontend deps if needed
:: ============================================
echo [2/6] Checking frontend dependencies...
if not exist "%FRONTEND_DIR%\node_modules" (
    echo   Installing node_modules...
    cd /d "%FRONTEND_DIR%"
    call npm install
    if errorlevel 1 (
        echo [ERROR] npm install failed!
        pause
        exit /b 1
    )
    echo   Dependencies installed.
) else (
    echo   node_modules: OK
)
echo.

:: ============================================
:: [3/6] PostgreSQL setup
:: ============================================
echo [3/6] Setting up PostgreSQL...

:: Check if container already running
docker ps --format "{{.Names}}" | findstr "aura_postgres" >nul
if %errorlevel%==0 (
    echo   PostgreSQL container: already running
) else (
    :: Clean up old containers
    docker rm -f aura_postgres_old 2>nul
    docker rm -f aura_postgres 2>nul

    :: Start PostgreSQL
    echo   Starting PostgreSQL container...
    docker run -d --name aura_postgres ^
        -e POSTGRES_DB=aura ^
        -e POSTGRES_USER=aura_user ^
        -e POSTGRES_PASSWORD=aura_password ^
        -p 5433:5432 ^
        postgres:15-alpine

    :: Wait for PostgreSQL to be ready
    echo   Waiting for PostgreSQL...
    set /a PG_ATTEMPTS=0
    :wait_pg
    set /a PG_ATTEMPTS+=1
    if !PG_ATTEMPTS! gtr 30 (
        echo   [ERROR] PostgreSQL failed to start within 60 seconds!
        pause
        exit /b 1
    )
    docker exec aura_postgres pg_isready -U aura_user -d aura >nul 2>&1
    if errorlevel 1 (
        timeout /t 2 /nobreak >nul
        goto :wait_pg
    )
    echo   PostgreSQL: ready
)

:: Fix table ownership
docker exec aura_postgres psql -U postgres -d aura -c "ALTER TABLE users OWNER TO aura_user;" 2>nul
echo.

:: ============================================
:: [4/6] Start Backend API
:: ============================================
echo [4/6] Starting Backend API on port 3002...

:: Check if port 3002 is already in use
netstat -ano | findstr ":3002 " >nul
if %errorlevel%==0 (
    echo   [WARN] Port 3002 is already in use. Stopping existing process...
    for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":3002 "') do (
        taskkill /F /PID %%a >nul 2>&1
    )
    timeout /t 2 /nobreak >nul
)

start "Aura Backend API" cmd /k "cd /d ""%BACKEND_DIR%"" && python main.py"

:: Wait for backend to be ready
echo   Waiting for backend...
set /a BK_ATTEMPTS=0
:wait_backend
set /a BK_ATTEMPTS+=1
if !BK_ATTEMPTS! gtr 15 (
    echo   [WARN] Backend may not have started. Check the backend window.
    goto :start_frontend
)
curl -s http://localhost:3002/api/health >nul 2>&1
if errorlevel 1 (
    timeout /t 2 /nobreak >nul
    goto :wait_backend
)
echo.

:: ============================================
:: [5/6] Start Frontend
:: ============================================
:start_frontend
echo [5/6] Starting Frontend on port 5173...
start "Aura Frontend" cmd /k "cd /d ""%FRONTEND_DIR%"" && npm run dev"

:: Wait for frontend
echo   Waiting for frontend...
timeout /t 5 /nobreak >nul
echo   Frontend: ready
echo.

:: ============================================
:: [6/6] Summary
:: ============================================
echo ========================================
echo System started successfully!
echo ========================================
echo.
echo URLs:
echo   - Backend API:   http://localhost:3002
echo   - API Docs:      http://localhost:3002/docs
echo   - Frontend:      http://localhost:5173
echo.
echo Demo accounts:
echo   - admin@aura.ru / admin123
echo   - manager@aura.ru / manager123
echo   - cosmetolog@aura.ru / cosmo123
echo.
echo Opening Frontend in browser...
start http://localhost:5173
echo.
echo To stop: close the Backend API and Frontend windows, then:
echo   docker stop aura_postgres
echo.
pause
endlocal
