@echo off
setlocal

set "ROOT=%~dp0"
set "BACKEND_DIR=%ROOT%backend\api"
set "FRONTEND_DIR=%ROOT%web-admin"
set "MOBILE_DIR=%ROOT%mobile"

if /i "%~1"=="--check" goto :check

echo Starting Aura system...

if exist "%BACKEND_DIR%\main.py" (
  start "Aura Backend" cmd /k "cd /d ""%BACKEND_DIR%"" && python -m uvicorn main:app --host 0.0.0.0 --port 3001 --reload"
) else (
  echo [WARN] Backend not found: %BACKEND_DIR%\main.py
)

if exist "%FRONTEND_DIR%\package.json" (
  start "Aura Frontend" cmd /k "cd /d ""%FRONTEND_DIR%"" && npm run dev"
) else (
  echo [WARN] Frontend not found: %FRONTEND_DIR%\package.json
)

if exist "%MOBILE_DIR%\gradlew.bat" (
  start "Aura Mobile" cmd /k "cd /d ""%MOBILE_DIR%"" && gradlew.bat installDebug"
) else (
  where bash >nul 2>nul
  if not errorlevel 1 (
    if exist "%MOBILE_DIR%\gradlew" (
      start "Aura Mobile" cmd /k "cd /d ""%MOBILE_DIR%"" && bash ./gradlew installDebug"
    ) else (
      echo [WARN] Mobile launcher not found: gradlew(.bat)
    )
  ) else (
    echo [WARN] mobile\gradlew.bat is missing and bash is not available.
    echo [INFO] Install Git Bash or add mobile\gradlew.bat, then rerun start.bat
  )
)

echo.
echo Backend:  http://localhost:3001
echo Frontend: http://localhost:5173
echo Mobile:   installDebug started in separate window
goto :eof

:check
echo ROOT=%ROOT%
echo BACKEND_DIR=%BACKEND_DIR%
echo FRONTEND_DIR=%FRONTEND_DIR%
echo MOBILE_DIR=%MOBILE_DIR%
if exist "%BACKEND_DIR%\main.py" (echo [OK] backend main.py found) else (echo [ERR] backend main.py missing)
if exist "%FRONTEND_DIR%\package.json" (echo [OK] frontend package.json found) else (echo [ERR] frontend package.json missing)
if exist "%MOBILE_DIR%\gradlew.bat" (
  echo [OK] mobile gradlew.bat found
) else (
  if exist "%MOBILE_DIR%\gradlew" (echo [OK] mobile gradlew found ^(bash required^)) else (echo [ERR] mobile gradlew missing)
)
endlocal
