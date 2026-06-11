@echo off
setlocal EnableExtensions EnableDelayedExpansion

set "ROOT=%~dp0"
set "FRONTEND_DIR=%ROOT%web-admin"
set "AI_SERVICE_DIR=%ROOT%ai-service"

set "DB_NAME=aura"
set "DB_USER=aura_user"
set "DB_PASSWORD=aura_password"
set "DB_PORT=5433"
set "WEAVIATE_CONTAINER=aura_weaviate"
set "T2V_CONTAINER=aura_t2v_transformers"

echo ========================================
echo Starting Aura Full System
echo ========================================
echo.

:: ============================================
:: [1/7] Check prerequisites
:: ============================================
echo [1/7] Checking prerequisites...

set "DB_ALREADY_AVAILABLE=false"
set "DOCKER_READY=false"

powershell -NoProfile -ExecutionPolicy Bypass -Command "if (Test-NetConnection -ComputerName 127.0.0.1 -Port 5433 -InformationLevel Quiet) { exit 0 } else { exit 1 }" >nul 2>&1
if not errorlevel 1 (
    set "DB_ALREADY_AVAILABLE=true"
    echo   PostgreSQL on localhost:5433 is already reachable
)
docker info >nul 2>&1
if errorlevel 1 (
    echo   [INFO] Docker is not running. Starting Docker Desktop...
    start "" "C:\Program Files\Docker\Docker\Docker Desktop.exe"
    echo   Waiting for Docker Desktop...
    set /a DOCKER_ATTEMPTS=0
    :wait_docker
    set /a DOCKER_ATTEMPTS+=1
    if !DOCKER_ATTEMPTS! gtr 90 (
        echo   [WARN] Docker did not become ready within 3 minutes.
        echo   Continuing without Docker-dependent steps.
        goto :docker_done
    )
    call :sleep 2
    docker info >nul 2>&1
    if errorlevel 1 goto :wait_docker
)
set "DOCKER_READY=true"
echo   Docker: OK
:docker_done

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
:: [2/7] Install frontend deps if needed
:: ============================================
echo [2/7] Checking frontend dependencies...
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
:: [3/7] PostgreSQL setup
:: ============================================
echo [3/7] Setting up PostgreSQL...

if /I "%DB_ALREADY_AVAILABLE%"=="true" (
    echo   PostgreSQL: using existing database on localhost:5433
) else (
    if /I not "%DOCKER_READY%"=="true" (
        echo   [ERROR] PostgreSQL is not reachable on localhost:5433 and Docker is unavailable.
        echo   Start Docker Desktop or PostgreSQL manually, then run start.bat again.
        pause
        exit /b 1
    )

    docker inspect aura_postgres >nul 2>&1
    if not errorlevel 1 (
        docker inspect -f "{{.State.Running}}" aura_postgres 2>nul | findstr /I "true" >nul
        if errorlevel 1 (
            echo   Starting existing PostgreSQL container...
            docker start aura_postgres >nul
        ) else (
            echo   PostgreSQL container: already running
        )
    ) else (
        echo   Creating PostgreSQL container...
        docker run -d --name aura_postgres ^
            -e POSTGRES_DB=%DB_NAME% ^
            -e POSTGRES_USER=%DB_USER% ^
            -e POSTGRES_PASSWORD=%DB_PASSWORD% ^
            -p %DB_PORT%:5432 ^
            -v aura_postgres_data:/var/lib/postgresql/data ^
            postgres:15-alpine >nul 2>&1
        if errorlevel 1 (
            echo   [ERROR] Could not create PostgreSQL container.
            pause
            exit /b 1
        )
    )

    echo   Waiting for PostgreSQL...
    set /a PG_ATTEMPTS=0
    :wait_pg
    set /a PG_ATTEMPTS+=1
    if !PG_ATTEMPTS! gtr 30 (
        echo   [ERROR] PostgreSQL failed to start within 60 seconds.
        pause
        exit /b 1
    )
    docker exec aura_postgres pg_isready -U %DB_USER% -d %DB_NAME% >nul 2>&1
    if errorlevel 1 (
        call :sleep 2
        goto :wait_pg
    )
    echo   PostgreSQL: ready
)
echo.

:: ============================================
:: [4/7] Vector DB setup
:: ============================================
echo [4/7] Setting up Weaviate vector DB...
if /I not "%DOCKER_READY%"=="true" (
    echo   [ERROR] Docker is unavailable, so Weaviate cannot be started.
    echo   Start Docker Desktop and run start.bat again.
    pause
    exit /b 1
) else (
    echo   Starting Docker service: t2v-transformers
    docker compose up -d t2v-transformers >nul 2>&1
    if errorlevel 1 (
        echo   [ERROR] Could not start t2v-transformers via docker compose.
        pause
        exit /b 1
    )

    echo   Waiting for transformers inference...
    call :wait_http_ready "Transformers inference" "http://localhost:8081/.well-known/ready" 60 2 "%T2V_CONTAINER%"
    if errorlevel 1 (
        echo   [ERROR] Transformers inference did not become ready.
        call :print_container_logs "%T2V_CONTAINER%"
        pause
        exit /b 1
    )
    echo   Transformers inference: ready

    echo   Starting Docker service: weaviate
    docker compose up -d weaviate >nul 2>&1
    if errorlevel 1 (
        echo   [ERROR] Could not start Weaviate via docker compose.
        pause
        exit /b 1
    )

    echo   Waiting for Weaviate...
    call :wait_http_ready "Weaviate" "http://localhost:8080/v1/.well-known/ready" 450 2 "%WEAVIATE_CONTAINER%"
    if errorlevel 1 (
        echo   [ERROR] Weaviate did not become ready.
        call :print_container_logs "%WEAVIATE_CONTAINER%"
        pause
        exit /b 1
    ) else (
        echo   Weaviate: ready
    )
)
echo.

:: ============================================
:: [5/7] Start services in one terminal
:: ============================================
echo [5/7] Starting services in this terminal...

if not exist "%AI_SERVICE_DIR%\app\main.py" (
    echo   [WARN] AI service not found at %AI_SERVICE_DIR%. Starting backend and frontend only.
    set "START_AI_SERVICE=false"
) else (
    set "START_AI_SERVICE=true"
)

call :stop_port 9002
call :stop_port 3002
call :stop_port 5173

if /I "%START_AI_SERVICE%"=="true" (
    start "Aura AI Service" /b cmd /c "cd /d ""%AI_SERVICE_DIR%"" && python -m uvicorn app.main:app --host 127.0.0.1 --port 9002"
)

start "Aura Backend API" /b cmd /c "cd /d ""%ROOT%"" && set AI_SERVICE_URL=http://localhost:9002 && set DB_PORT=5433 && python -m backend.main"
start "Aura Frontend" /b cmd /c "cd /d ""%FRONTEND_DIR%"" && npm run dev -- --host 127.0.0.1 --port 5173"

echo   Services are starting in this terminal. Logs may be mixed together.
call :sleep 5
echo   Configuring Android localhost bridge via adb reverse...
powershell -NoProfile -ExecutionPolicy Bypass -File "%ROOT%mobile\scripts\setup-localhost-reverse.ps1" -Ports 3002 -AllowNoDevice
if errorlevel 1 (
    echo   [WARN] Could not configure adb reverse for mobile localhost access.
)
echo.

:: ============================================
:: [6/7] Quick status
:: ============================================
echo [6/7] Quick status...
call :print_http_status "Weaviate" "http://localhost:8080/v1/.well-known/ready"
call :print_http_status "AI Service" "http://localhost:9002/health"
call :print_http_status "Backend API" "http://localhost:3002/api/health"
call :print_http_status "Frontend" "http://localhost:5173"
echo.

:: ============================================
:: [7/7] Summary
:: ============================================
echo ========================================
echo Aura startup sequence finished
echo ========================================
echo.
echo URLs:
echo   - Weaviate:      http://localhost:8080
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
echo   - close this terminal window or stop ports 9002 / 3002 / 5173
echo   - docker stop aura_postgres
echo   - docker compose stop weaviate t2v-transformers
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

:stop_port
set "TARGET_PORT=%~1"
for /f "tokens=5" %%a in ('netstat -ano ^| findstr /R /C:":%TARGET_PORT% .*LISTENING"') do (
    taskkill /F /PID %%a /T >nul 2>&1
)
exit /b 0

:wait_http_ready
set "WAIT_LABEL=%~1"
set "WAIT_URL=%~2"
set "WAIT_ATTEMPTS=%~3"
set "WAIT_SLEEP=%~4"
set "WAIT_CONTAINER=%~5"
set /a __wait_try=0
:wait_http_ready_loop
set /a __wait_try+=1
if !__wait_try! gtr %WAIT_ATTEMPTS% exit /b 1
if not "%WAIT_CONTAINER%"=="" (
    docker inspect -f "{{.State.Running}}" "%WAIT_CONTAINER%" 2>nul | findstr /I "true" >nul
    if errorlevel 1 exit /b 1
)
set "HTTP_STATUS=000"
for /f %%a in ('curl.exe -s -o NUL -w "%%{http_code}" "!WAIT_URL!" 2^>nul') do set "HTTP_STATUS=%%a"
if "!HTTP_STATUS!"=="200" exit /b 0
if "!HTTP_STATUS!"=="204" exit /b 0
call :sleep %WAIT_SLEEP%
goto :wait_http_ready_loop

:print_container_logs
set "LOG_CONTAINER=%~1"
if "%LOG_CONTAINER%"=="" exit /b 0
echo   Last logs from %LOG_CONTAINER%:
docker logs --tail 80 "%LOG_CONTAINER%"
exit /b 0

:print_http_status
set "STATUS_LABEL=%~1"
set "STATUS_URL=%~2"
set "HTTP_STATUS=000"
for /f %%a in ('curl.exe -s -o NUL -w "%%{http_code}" %STATUS_URL% 2^>nul') do set "HTTP_STATUS=%%a"
if "!HTTP_STATUS!"=="200" (
    echo   !STATUS_LABEL!: OK
) else (
    echo   !STATUS_LABEL!: starting or unavailable (HTTP !HTTP_STATUS!)
)
exit /b 0
