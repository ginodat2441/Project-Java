@echo off
setlocal

if /i "%~1"=="nopause" set "NO_PAUSE=1"
if not defined NGROK_CMD for /f "delims=" %%N in ('where ngrok.exe 2^>nul') do if not defined NGROK_CMD set "NGROK_CMD=%%N"
if not defined NGROK_CMD if exist "%LOCALAPPDATA%\Microsoft\WinGet\Links\ngrok.exe" set "NGROK_CMD=%LOCALAPPDATA%\Microsoft\WinGet\Links\ngrok.exe"
if not defined NGROK_CMD if exist "%LOCALAPPDATA%\Microsoft\WindowsApps\ngrok.exe" set "NGROK_CMD=%LOCALAPPDATA%\Microsoft\WindowsApps\ngrok.exe"
if not defined NGROK_CMD if exist "%LOCALAPPDATA%\Programs\ngrok\ngrok.exe" set "NGROK_CMD=%LOCALAPPDATA%\Programs\ngrok\ngrok.exe"
if defined NGROK_CMD (
  call "%NGROK_CMD%" version >nul 2>nul
  if errorlevel 1 set "NGROK_CMD="
)
if not defined NGROK_CMD (
  echo ngrok was not found. Opening Microsoft Store search for ngrok...
  start "" "ms-windows-store://search/?query=ngrok"
  echo Install ngrok, then run this setup file again.
  if not defined NO_PAUSE pause
  exit /b 1
)

set "NGROK_EXE=%NGROK_CMD%"
powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference = 'Stop'; $secure = $null; $ptr = [IntPtr]::Zero; $plain = $null; try { $secure = Read-Host 'Enter your NEW ngrok authtoken' -AsSecureString; if ($secure.Length -eq 0) { throw 'The authtoken cannot be empty.' }; $ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure); $plain = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr); $configDir = Join-Path $env:LOCALAPPDATA 'ngrok'; $configPath = Join-Path $configDir 'ngrok.yml'; New-Item -ItemType Directory -Path $configDir -Force | Out-Null; $hadConfig = Test-Path -LiteralPath $configPath; if ($hadConfig) { $backupPath = $configPath + '.' + (Get-Date -Format 'yyyyMMddHHmmssfff') + '.bak'; Copy-Item -LiteralPath $configPath -Destination $backupPath }; $yaml = @('version: 3', 'agent:', ('  authtoken: ' + (ConvertTo-Json -InputObject $plain -Compress)), 'tunnels:', '  app1:', '    proto: http', '    addr: 3000'); [IO.File]::WriteAllLines($configPath, [string[]]$yaml, [Text.Encoding]::ASCII); & $env:NGROK_EXE config check; if ($LASTEXITCODE -ne 0) { if ($hadConfig) { Copy-Item -LiteralPath $backupPath -Destination $configPath -Force } else { Remove-Item -LiteralPath $configPath -Force }; throw 'ngrok rejected the config. The previous config has been restored.' }; Write-Host ('ngrok config saved to: ' + $configPath); if ($hadConfig) { Write-Host ('Previous config backup: ' + $backupPath) }; Write-Host 'Start the tunnel with: ngrok http http://127.0.0.1:3000' } catch { Write-Error $_; exit 1 } finally { if ($ptr -ne [IntPtr]::Zero) { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr) }; if ($null -ne $secure) { $secure.Dispose() }; $plain = $null }"
if errorlevel 1 (
  echo.
  echo ngrok setup failed.
  if not defined NO_PAUSE pause
  exit /b 1
)

echo.
if not defined NO_PAUSE pause
endlocal
