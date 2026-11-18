@echo off
setlocal EnableExtensions
set "ROOT=%~dp0"

if /i "%~1"=="frontend" goto :frontend
if /i "%~1"=="proxy" goto :proxy
if /i "%~1"=="client-backend" goto :clientBackend
if /i "%~1"=="admin-backend" goto :adminBackend
if /i "%~1"=="ngrok" goto :ngrok

call :findPython
if errorlevel 1 goto :failed

call :findMaven
if errorlevel 1 goto :failed

call :findNgrok
if errorlevel 1 (
  echo ngrok was not found. The local services will start without a public tunnel.
) else (
  call "%NGROK_CMD%" config check
  if errorlevel 1 (
    echo ngrok configuration is invalid. Run proxy_setting\setup_ngrok.bat first.
    set "NGROK_CMD="
  )
)

echo Starting client frontend on port 5500...
start "Client Frontend - 5500" "%ComSpec%" /d /k call "%~f0" frontend

echo Starting client backend on port 8080...
start "Client Backend - 8080" "%ComSpec%" /d /k call "%~f0" client-backend

echo Starting admin backend and UI on port 8888...
start "Admin Backend - 8888" "%ComSpec%" /d /k call "%~f0" admin-backend

echo Starting reverse proxy on port 3000...
start "Shop Proxy - 3000" "%ComSpec%" /d /k call "%~f0" proxy

if defined NGROK_CMD (
  echo Starting ngrok tunnel to the local proxy...
  start "Shop ngrok tunnel" "%ComSpec%" /d /k call "%~f0" ngrok
)

echo.
echo Client UI:       http://localhost:5500
echo Client via proxy: http://localhost:3000
echo Public URL:      shown in the ngrok window
echo Client API:      http://localhost:8080
echo Admin UI:        http://localhost:8888/admin
echo Backend startup runs mvn spring-boot:run. Close each window or press Ctrl+C to stop.
exit /b 0

:clientBackend
call :findMaven
if errorlevel 1 exit /b 1
pushd "%ROOT%client_backend" || exit /b 1
call "%MAVEN_CMD%" spring-boot:run
set "SERVICE_EXIT=%ERRORLEVEL%"
popd
exit /b %SERVICE_EXIT%

:adminBackend
call :findMaven
if errorlevel 1 exit /b 1
pushd "%ROOT%admin\admin_code" || exit /b 1
call "%MAVEN_CMD%" spring-boot:run
set "SERVICE_EXIT=%ERRORLEVEL%"
popd
exit /b %SERVICE_EXIT%

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

:ngrok
call :findNgrok
if errorlevel 1 exit /b 1
call "%NGROK_CMD%" start app1
exit /b %ERRORLEVEL%

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

:findMaven
set "MAVEN_CMD="
for /f "delims=" %%M in ('where mvn.cmd 2^>nul') do if not defined MAVEN_CMD set "MAVEN_CMD=%%M"
if defined MAVEN_CMD (
  exit /b 0
)
if exist "%ROOT%.local\apache-maven-3.9.9\bin\mvn.cmd" (
  set "MAVEN_CMD=%ROOT%.local\apache-maven-3.9.9\bin\mvn.cmd"
  exit /b 0
)
echo Maven was not found. Add Maven to PATH or install it under .local\apache-maven-3.9.9.
exit /b 1

:findNgrok
set "NGROK_CMD="
for /f "delims=" %%N in ('where ngrok.exe 2^>nul') do if not defined NGROK_CMD set "NGROK_CMD=%%N"
if defined NGROK_CMD exit /b 0
echo ngrok was not found in PATH.
exit /b 1

:failed
echo.
echo Startup was stopped. Fix the reported issue and run this file again.
pause
exit /b 1
