@echo off
setlocal EnableExtensions EnableDelayedExpansion

set "ROOT=%~dp0"
set "FRONTEND_DIR=%ROOT%web-admin"
set "AI_SERVICE_DIR=%ROOT%ai-service"

set "DB_NAME=aura"
set "DB_USER=aura_user"
set "DB_PASSWORD=aura_password"
set "DB_PORT=5433"

echo ========================================
echo Starting Aura Full System
echo ========================================
echo.

:: ============================================
:: [1/8] Check prerequisites
:: ============================================
echo [1/8] Checking prerequisites...

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
:: [2/8] Install frontend deps if needed
:: ============================================
echo [2/8] Checking frontend dependencies...
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
:: [3/8] PostgreSQL setup
:: ============================================
echo [3/8] Setting up PostgreSQL...

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
:: [4/8] Vector DB setup
:: ============================================
echo [4/8] Setting up Weaviate vector DB...
echo   Starting Docker services: weaviate, t2v-transformers
docker compose up -d weaviate t2v-transformers >nul 2>&1
if errorlevel 1 (
    echo   [WARN] Could not start Weaviate via docker compose. RAG chat may be unavailable.
) else (
    echo   Waiting for Weaviate...
    set /a WV_ATTEMPTS=0
    :wait_weaviate
    set /a WV_ATTEMPTS+=1
    if !WV_ATTEMPTS! gtr 45 (
        echo   [WARN] Weaviate did not report ready state yet. Continuing startup.
        goto :weaviate_done
    )
    powershell -NoProfile -ExecutionPolicy Bypass -Command "try { $r = Invoke-WebRequest -UseBasicParsing -Uri 'http://localhost:8080/v1/.well-known/ready' -TimeoutSec 2; if($r.StatusCode -eq 200){exit 0}else{exit 1} } catch { exit 1 }" >nul 2>&1
    if errorlevel 1 (
        call :sleep 2
        goto :wait_weaviate
    )
    echo   Weaviate: ready
)
:weaviate_done
echo.

:: ============================================
:: [5/8] Start AI Service
:: ============================================
echo [5/8] Starting AI Service on port 9002...

if not exist "%AI_SERVICE_DIR%\app\main.py" (
    echo   [WARN] AI service not found at %AI_SERVICE_DIR%. Skipping AI service startup.
    goto :start_backend
)

taskkill /F /FI "WINDOWTITLE eq Aura AI Service*" /T >nul 2>&1
for /f "tokens=5" %%a in ('netstat -ano ^| findstr /R /C:":9002 .*LISTENING"') do (
    taskkill /F /PID %%a >nul 2>&1
)

start "Aura AI Service" cmd /k "cd /d ""%AI_SERVICE_DIR%"" && python -m uvicorn app.main:app --host 127.0.0.1 --port 9002"

echo   Waiting for AI service...
set /a AI_ATTEMPTS=0
:wait_ai
set /a AI_ATTEMPTS+=1
if !AI_ATTEMPTS! gtr 45 (
    echo   [WARN] AI service may not have started. Check the AI Service window.
    goto :start_backend
)
powershell -NoProfile -ExecutionPolicy Bypass -Command "try { $r = Invoke-WebRequest -UseBasicParsing -Uri 'http://localhost:9002/health' -TimeoutSec 2; if($r.StatusCode -eq 200){exit 0}else{exit 1} } catch { exit 1 }" >nul 2>&1
if errorlevel 1 (
    call :sleep 2
    goto :wait_ai
)
echo   AI service: ready
echo.

:: ============================================
:: [6/8] Start Backend API
:: ============================================
:start_backend
echo [6/8] Starting Backend API on port 3002...

taskkill /F /FI "WINDOWTITLE eq Aura Backend API*" /T >nul 2>&1
for /f "tokens=5" %%a in ('netstat -ano ^| findstr /R /C:":3002 .*LISTENING"') do (
    taskkill /F /PID %%a >nul 2>&1
)

start "Aura Backend API" cmd /k "set AI_SERVICE_URL=http://localhost:9002 && call ""%ROOT%run_backend.bat"""

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
:: [7/8] Start Frontend
:: ============================================
echo [7/8] Starting Frontend on port 5173...

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
:: [8/8] Summary
:: ============================================
echo ========================================
echo System started successfully!
echo ========================================
echo.
echo URLs:
echo   - AI Service:    http://localhost:9002
echo   - AI Health:     http://localhost:9002/health
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
echo   - close "Aura AI Service", "Aura Backend API" and "Aura Frontend" windows
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
