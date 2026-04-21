@echo off
setlocal EnableExtensions EnableDelayedExpansion

set "ROOT=%~dp0"
set "BACKEND_DIR=%ROOT%backend\api"
set "FRONTEND_DIR=%ROOT%web-admin"

set "DB_HOST=localhost"
set "DB_PORT=5433"
set "DB_NAME=aura"
set "DB_USER=aura_user"
set "DB_PASSWORD=aura_password"

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
    if !DOCKER_ATTEMPTS! gtr 90 (
        echo   [ERROR] Docker failed to start within 3 minutes. Start Docker Desktop manually.
        pause
        exit /b 1
    )
    call :sleep 2
    docker info >nul 2>&1
    if errorlevel 1 goto :wait_docker
    echo   Docker: started
)
echo   Docker: OK

python --version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Python not found! Install Python 3.11+
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
    pushd "%FRONTEND_DIR%"
    call npm install
    if errorlevel 1 (
        popd
        echo [ERROR] npm install failed!
        pause
        exit /b 1
    )
    popd
    echo   Dependencies installed.
) else (
    echo   node_modules: OK
)
echo.

:: ============================================
:: [3/6] PostgreSQL setup
:: ============================================
echo [3/6] Setting up PostgreSQL...

docker inspect aura_postgres >nul 2>&1
if errorlevel 1 (
    echo   Creating PostgreSQL container...
    docker run -d --name aura_postgres ^
        -e POSTGRES_DB=%DB_NAME% ^
        -e POSTGRES_USER=%DB_USER% ^
        -e POSTGRES_PASSWORD=%DB_PASSWORD% ^
        -p %DB_PORT%:5432 ^
        postgres:15-alpine >nul
    if errorlevel 1 (
        echo   [ERROR] Failed to create PostgreSQL container.
        pause
        exit /b 1
    )
) else (
    docker inspect -f "{{.State.Running}}" aura_postgres 2>nul | findstr /I "true" >nul
    if errorlevel 1 (
        echo   Starting existing PostgreSQL container...
        docker start aura_postgres >nul
    ) else (
        echo   PostgreSQL container: already running
    )
)

echo   Waiting for PostgreSQL...
set /a PG_ATTEMPTS=0
:wait_pg
set /a PG_ATTEMPTS+=1
if !PG_ATTEMPTS! gtr 45 (
    echo   [ERROR] PostgreSQL failed to become ready.
    docker logs --tail 50 aura_postgres
    pause
    exit /b 1
)
docker exec aura_postgres pg_isready -U %DB_USER% -d %DB_NAME% >nul 2>&1
if errorlevel 1 (
    call :sleep 2
    goto :wait_pg
)
echo   PostgreSQL: ready
echo.

:: ============================================
:: [4/6] Start Backend API
:: ============================================
echo [4/6] Starting Backend API on port 3002...

echo   Restarting backend process...
taskkill /F /FI "WINDOWTITLE eq Aura Backend API*" /T >nul 2>&1

for /f "tokens=5" %%a in ('netstat -ano ^| findstr /R /C:":3002 .*LISTENING"') do (
    taskkill /F /PID %%a >nul 2>&1
)

start "Aura Backend API" cmd /k ""%ROOT%run_backend.bat""

echo   Waiting for backend...
set /a BK_ATTEMPTS=0
:wait_backend
set /a BK_ATTEMPTS+=1
if !BK_ATTEMPTS! gtr 60 (
    echo   [ERROR] Backend did not respond on http://localhost:3002/api/health
    echo   Check the "Aura Backend API" window for traceback.
    pause
    exit /b 1
)
powershell -NoProfile -ExecutionPolicy Bypass -Command "try { $r = Invoke-WebRequest -UseBasicParsing -Uri 'http://localhost:3002/api/health' -TimeoutSec 2; if($r.StatusCode -eq 200){exit 0}else{exit 1} } catch { exit 1 }" >nul 2>&1
if errorlevel 1 (
    call :sleep 2
    goto :wait_backend
)
echo   Backend: ready
echo.

:: ============================================
:: [5/6] Start Frontend
:: ============================================
echo [5/6] Starting Frontend on port 5173...

echo   Restarting frontend process...
taskkill /F /FI "WINDOWTITLE eq Aura Frontend*" /T >nul 2>&1

for /f "tokens=5" %%a in ('netstat -ano ^| findstr /R /C:":5173 .*LISTENING"') do (
    taskkill /F /PID %%a >nul 2>&1
)

start "Aura Frontend" cmd /k ""%ROOT%run_frontend.bat""

echo   Waiting for frontend...
powershell -NoProfile -ExecutionPolicy Bypass -Command "$maxAttempts=60; for($i=0; $i -lt $maxAttempts; $i++){ try { $r=Invoke-WebRequest -UseBasicParsing -Uri 'http://localhost:5173' -TimeoutSec 2; if($r.StatusCode -ge 200){ exit 0 } } catch {}; Start-Sleep -Seconds 2 }; exit 1" >nul 2>&1
if errorlevel 1 (
    echo   [ERROR] Frontend did not respond on http://localhost:5173
    echo   Check the "Aura Frontend" window for errors.
    pause
    exit /b 1
)
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
echo To stop:
echo   - close "Aura Backend API" and "Aura Frontend" windows
echo   - docker stop aura_postgres
echo.
pause
endlocal
exit /b 0

:sleep
set "SLEEP_SECONDS=%~1"
if "%SLEEP_SECONDS%"=="" set "SLEEP_SECONDS=1"
set /a __sleep_n=%SLEEP_SECONDS%+1
ping 127.0.0.1 -n !__sleep_n! >nul
exit /b 0
