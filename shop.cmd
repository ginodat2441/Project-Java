@echo off
setlocal
:find_service
if "%~1"=="" goto backend
if /i "%~1"=="-Service" goto split
shift
goto find_service
:split
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0tools\run-local.ps1" %*
exit /b %errorlevel%
:backend
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0tools\shop.ps1" %*
exit /b %errorlevel%
