@echo off
cd /d "%~dp0"
python -m uvicorn main:app --host 0.0.0.0 --port 3001 --reload
pause
