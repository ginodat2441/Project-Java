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

set "MAVEN_CMD="
for /f "delims=" %%M in ('where mvn.cmd 2^>nul') do if not defined MAVEN_CMD set "MAVEN_CMD=%%M"
if not defined MAVEN_CMD if exist "%ROOT%.local\apache-maven-3.9.9\bin\mvn.cmd" set "MAVEN_CMD=%ROOT%.local\apache-maven-3.9.9\bin\mvn.cmd"
if not defined MAVEN_CMD call :installMaven
if errorlevel 1 goto :failed

call :findNgrok
if errorlevel 1 (
  echo ngrok was not found. The local services will start without a public tunnel.
) else (
  call :ensureNgrokConfig
  if errorlevel 1 (
    echo ngrok is not configured. Local services will start without a public tunnel.
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
  call :waitForProxy
  if errorlevel 1 (
    echo The local proxy did not listen on port 3000. ngrok was not started.
  ) else (
    echo Starting ngrok tunnel to the local proxy...
    start "Shop ngrok tunnel" "%ComSpec%" /d /k call "%~f0" ngrok
  )
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
if not defined MAVEN_CMD (
  echo Maven path was not inherited. Run run-all.bat without arguments first.
  exit /b 1
)
pushd "%ROOT%client_backend" || exit /b 1
call "%MAVEN_CMD%" spring-boot:run
set "SERVICE_EXIT=%ERRORLEVEL%"
popd
exit /b %SERVICE_EXIT%

:adminBackend
if not defined MAVEN_CMD (
  echo Maven path was not inherited. Run run-all.bat without arguments first.
  exit /b 1
)
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
call "%NGROK_CMD%" http http://127.0.0.1:3000
exit /b %ERRORLEVEL%

:findPython
set "PYTHON_CMD="
where py >nul 2>nul
if not errorlevel 1 (
  set "PYTHON_CMD=py -3"
  call :validatePython
  if not errorlevel 1 exit /b 0
  set "PYTHON_CMD="
)
for /f "delims=" %%P in ('where python.exe 2^>nul') do if not defined PYTHON_CMD if /i not "%%~dpP"=="%LOCALAPPDATA%\Microsoft\WindowsApps\" call :validatePythonPath "%%P"
if defined PYTHON_CMD exit /b 0
for /d %%P in ("%LOCALAPPDATA%\Programs\Python\Python*") do if not defined PYTHON_CMD if exist "%%~fP\python.exe" call :validatePythonPath "%%~fP\python.exe"
if defined PYTHON_CMD exit /b 0
call :installPython
exit /b %ERRORLEVEL%

:validatePython
if /i "%PYTHON_CMD%"=="py -3" goto :validatePythonLauncher
%PYTHON_CMD% -c "import sys, http.server; assert sys.version_info >= (3, 10)" >nul 2>nul
exit /b %ERRORLEVEL%

:validatePythonLauncher
py -3 -c "import sys, http.server; assert sys.version_info >= (3, 10)" >nul 2>nul
exit /b %ERRORLEVEL%

:validatePythonPath
"%~1" -c "import sys, http.server; assert sys.version_info >= (3, 10)" >nul 2>nul
if errorlevel 1 exit /b 1
set "PYTHON_CMD="%~1""
exit /b 0

:findNgrok
if not defined NGROK_CMD for /f "delims=" %%N in ('where ngrok.exe 2^>nul') do if not defined NGROK_CMD set "NGROK_CMD=%%N"
if not defined NGROK_CMD for /f "delims=" %%N in ('where ngrok 2^>nul') do if not defined NGROK_CMD set "NGROK_CMD=%%N"
if not defined NGROK_CMD if exist "%LOCALAPPDATA%\Microsoft\WinGet\Links\ngrok.exe" set "NGROK_CMD=%LOCALAPPDATA%\Microsoft\WinGet\Links\ngrok.exe"
if not defined NGROK_CMD if exist "%LOCALAPPDATA%\Microsoft\WindowsApps\ngrok.exe" set "NGROK_CMD=%LOCALAPPDATA%\Microsoft\WindowsApps\ngrok.exe"
if not defined NGROK_CMD if exist "%LOCALAPPDATA%\Programs\ngrok\ngrok.exe" set "NGROK_CMD=%LOCALAPPDATA%\Programs\ngrok\ngrok.exe"
if not defined NGROK_CMD if exist "%ProgramFiles%\ngrok\ngrok.exe" set "NGROK_CMD=%ProgramFiles%\ngrok\ngrok.exe"
if not defined NGROK_CMD goto :missingNgrok
call "%NGROK_CMD%" version >nul 2>nul
if not errorlevel 1 exit /b 0
set "NGROK_CMD="
:missingNgrok
echo ngrok was not found. Opening Microsoft Store search for ngrok...
start "" "ms-windows-store://search/?query=ngrok"
exit /b 1

:ensureNgrokConfig
set "NGROK_EXE=%NGROK_CMD%"
powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "$p=Join-Path $env:LOCALAPPDATA 'ngrok\ngrok.yml'; if (!(Test-Path -LiteralPath $p)) { exit 1 }; $token=Select-String -Path $p -Pattern '^\s*authtoken:\s*\S+'; if (!$token) { exit 1 }" >nul 2>nul
if errorlevel 1 goto :setupNgrokConfig
call "%NGROK_CMD%" config check >nul
if errorlevel 1 goto :setupNgrokConfig
exit /b 0

:setupNgrokConfig
echo ngrok needs a valid account token on this computer.
call "%ROOT%proxy_setting\setup_ngrok.bat" nopause
if errorlevel 1 exit /b 1
call "%NGROK_CMD%" config check
exit /b %ERRORLEVEL%

:waitForProxy
echo Waiting for the proxy to listen on port 3000...
powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "$deadline=(Get-Date).AddSeconds(30); do { if (Test-NetConnection -ComputerName 127.0.0.1 -Port 3000 -InformationLevel Quiet) { exit 0 }; Start-Sleep -Milliseconds 500 } while ((Get-Date) -lt $deadline); exit 1"
exit /b %ERRORLEVEL%

:installPython
where winget >nul 2>nul
if errorlevel 1 (
  echo Python is missing and winget is unavailable. Install Python 3.13 manually from https://www.python.org/downloads/windows/
  exit /b 1
)
echo Python is missing. Installing Python 3.13 for the current user...
winget install --id Python.Python.3.13 --exact --scope user --silent --accept-package-agreements --accept-source-agreements
if errorlevel 1 (
  echo Python installation failed. Install Python 3.13 manually and run this file again.
  exit /b 1
)
if exist "%LOCALAPPDATA%\Programs\Python\Python313\python.exe" call :validatePythonPath "%LOCALAPPDATA%\Programs\Python\Python313\python.exe"
for /d %%P in ("%LOCALAPPDATA%\Programs\Python\Python*") do if not defined PYTHON_CMD if exist "%%~fP\python.exe" call :validatePythonPath "%%~fP\python.exe"
if defined PYTHON_CMD exit /b 0
echo Python installation completed, but a working Python 3.10+ runtime was not found.
echo Install Python 3.13 manually from https://www.python.org/downloads/windows/ and run this file again.
exit /b 1

:installMaven
where choco >nul 2>nul
if not errorlevel 1 (
  echo Maven is missing. Trying to install it with Chocolatey...
  choco install maven --yes
  if not errorlevel 1 (
    for /f "delims=" %%M in ('where mvn.cmd 2^>nul') do if not defined MAVEN_CMD set "MAVEN_CMD=%%M"
    if defined MAVEN_CMD exit /b 0
  )
)
echo Maven could not be installed automatically.
echo Install Apache Maven 3.9.9 or newer, add its bin folder to PATH, then run this file again.
echo Download: https://maven.apache.org/download.cgi
exit /b 1

:failed
echo.
echo Startup was stopped. Fix the reported issue and run this file again.
pause
exit /b 1
