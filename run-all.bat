@echo off
setlocal EnableExtensions
set "ROOT=%~dp0"

if /i "%~1"=="frontend" goto :frontend
if /i "%~1"=="proxy" goto :proxy

call :findPython
if errorlevel 1 goto :failed

if not exist "%ROOT%.vscode\oracle-local.env" (
  echo Missing .vscode\oracle-local.env. Configure Oracle credentials first.
  goto :failed
)

echo Building client backend...
call "%ROOT%shop.cmd" -Mode Build -Service Client
if errorlevel 1 goto :failed

echo Building admin backend...
call "%ROOT%shop.cmd" -Mode Build -Service Admin
if errorlevel 1 goto :failed

echo Starting client frontend on port 5500...
start "Client Frontend - 5500" "%ComSpec%" /d /k call "%~f0" frontend

echo Starting client backend on port 8080...
start "Client Backend - 8080" "%ComSpec%" /d /k call "%ROOT%shop.cmd" -Mode Run -Service Client

echo Starting admin backend and UI on port 8888...
start "Admin Backend - 8888" "%ComSpec%" /d /k call "%ROOT%shop.cmd" -Mode Run -Service Admin

echo Starting reverse proxy on port 3000...
start "Shop Proxy - 3000" "%ComSpec%" /d /k call "%~f0" proxy

echo.
echo Client UI:       http://localhost:5500
echo Client via proxy: http://localhost:3000
echo Client API:      http://localhost:8080
echo Admin UI:        http://localhost:8888/admin
echo Close each service window or press Ctrl+C in it to stop that service.
exit /b 0

:frontend
call :findPython
if errorlevel 1 exit /b 1
pushd "%ROOT%client" || exit /b 1
%PYTHON_CMD% -m http.server 5500 --bind 127.0.0.1
set "SERVICE_EXIT=%ERRORLEVEL%"
popd
exit /b %SERVICE_EXIT%

:proxy
call :findPython
if errorlevel 1 exit /b 1
pushd "%ROOT%proxy_setting" || exit /b 1
set "PROXY_HOST=127.0.0.1"
set "PROXY_PORT=3000"
set "FRONTEND_HOST=127.0.0.1"
set "FRONTEND_PORT=5500"
set "BACKEND_HOST=127.0.0.1"
set "BACKEND_PORT=8080"
%PYTHON_CMD% proxy.py
set "SERVICE_EXIT=%ERRORLEVEL%"
popd
exit /b %SERVICE_EXIT%

:findPython
set "PYTHON_CMD="
where py >nul 2>nul
if not errorlevel 1 (
  set "PYTHON_CMD=py -3"
  exit /b 0
)
where python >nul 2>nul
if not errorlevel 1 (
  set "PYTHON_CMD=python"
  exit /b 0
)
echo Python 3 was not found. Install Python 3 and try again.
exit /b 1

:failed
echo.
echo Startup was stopped. Fix the reported issue and run this file again.
pause
exit /b 1
