@echo off
setlocal

where ngrok >nul 2>nul
if errorlevel 1 (
  echo ngrok was not found. Install ngrok and make sure it is available in PATH.
  pause
  exit /b 1
)

powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference = 'Stop'; $secure = $null; $ptr = [IntPtr]::Zero; $plain = $null; try { $secure = Read-Host 'Enter your NEW ngrok authtoken' -AsSecureString; if ($secure.Length -eq 0) { throw 'The authtoken cannot be empty.' }; $ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure); $plain = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr); $configDir = Join-Path $env:LOCALAPPDATA 'ngrok'; $configPath = Join-Path $configDir 'ngrok.yml'; New-Item -ItemType Directory -Path $configDir -Force | Out-Null; $hadConfig = Test-Path -LiteralPath $configPath; if ($hadConfig) { $backupPath = $configPath + '.' + (Get-Date -Format 'yyyyMMddHHmmssfff') + '.bak'; Copy-Item -LiteralPath $configPath -Destination $backupPath }; $yaml = @('version: 3', 'agent:', ('  authtoken: ' + (ConvertTo-Json -InputObject $plain -Compress)), 'tunnels:', '  app1:', '    proto: http', '    addr: 3000'); [IO.File]::WriteAllLines($configPath, [string[]]$yaml, [Text.Encoding]::ASCII); & ngrok config check; if ($LASTEXITCODE -ne 0) { if ($hadConfig) { Copy-Item -LiteralPath $backupPath -Destination $configPath -Force } else { Remove-Item -LiteralPath $configPath -Force }; throw 'ngrok rejected the config. The previous config has been restored.' }; Write-Host ('ngrok config saved to: ' + $configPath); if ($hadConfig) { Write-Host ('Previous config backup: ' + $backupPath) }; Write-Host 'Start the tunnel with: ngrok start app1' } catch { Write-Error $_; exit 1 } finally { if ($ptr -ne [IntPtr]::Zero) { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr) }; if ($null -ne $secure) { $secure.Dispose() }; $plain = $null }"
if errorlevel 1 (
  echo.
  echo ngrok setup failed.
  pause
  exit /b 1
)

echo.
pause
endlocal
