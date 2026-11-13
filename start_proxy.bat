@echo off
setlocal
cd /d "%~dp0"

where py >nul 2>nul
if not errorlevel 1 set "PYTHON_CMD=py -3"
if not defined PYTHON_CMD (
  where python >nul 2>nul
  if not errorlevel 1 set "PYTHON_CMD=python"
)
if not defined PYTHON_CMD (
  echo Python 3 was not found. Install Python 3 and try again.
  pause
  exit /b 1
)

where ngrok >nul 2>nul
if errorlevel 1 (
  echo ngrok was not found. Start it separately with: ngrok http 3000
) else (
  echo Starting ngrok tunnel to localhost:3000...
  start "ngrok - localhost:3000" "%ComSpec%" /k ngrok http 3000
)

echo Starting frontend and API proxy...
echo Make sure the frontend is running on port 5500 and backend on port 8080.
%PYTHON_CMD% proxy.py
if errorlevel 1 (
  echo.
  echo Proxy stopped with an error.
  pause
)

endlocal
