param(
    [ValidateSet('Run','Build','Test','CreateAdmin','Migrate','CleanImages','ConfigureSmtp','TestSmtp')][string]$Mode = 'Run'
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $projectRoot
`$javaHomeCandidates = @(`$env:JAVA_HOME)

if (-not $env:JAVA_HOME) {
    foreach ($candidate in $javaHomeCandidates) {
        if ($candidate -and (Test-Path -LiteralPath (Join-Path $candidate 'bin/java.exe'))) { $env:JAVA_HOME=$candidate; break }
    }
}
$javaCmd = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin/java.exe' } else { 'java' }
$javaVersionOutput = (& $javaCmd -version 2>&1 | Out-String)
if ($LASTEXITCODE -ne 0 -or $javaVersionOutput -notmatch 'version "25(?:\.|")') { throw 'JDK 25 LTS is required. Set JAVA_HOME to a JDK 25 installation.' }
$mavenCmd = if (Get-Command mvn -ErrorAction SilentlyContinue) { 'mvn' } else { Join-Path $projectRoot '.local/apache-maven-3.9.9/bin/mvn.cmd' }
$localEnv = Join-Path $projectRoot '.vscode/oracle-local.env'
function Import-ShopLocalEnv {
foreach($localEnv in @((Join-Path $projectRoot '.vscode/oracle-local.env'),(Join-Path $projectRoot '.vscode/viettel-local.env'))){
if (Test-Path -LiteralPath $localEnv) {
    foreach ($line in Get-Content -LiteralPath $localEnv) {
        if ($line -match '^\s*(SPRING_DATASOURCE_PASSWORD|SPRING_DATASOURCE_URL|SPRING_DATASOURCE_USERNAME|SHOP_UPLOAD_DIR|SHOP_PRIVATE_DIR|SHOP_RESET_DELIVERY|SHOP_RESET_BASE_URL|SHOP_RESET_FROM|SHOP_SMTP_AUTH|SHOP_SMTP_STARTTLS|SHOP_SMTP_SSL|SPRING_MAIL_HOST|SPRING_MAIL_PORT|SPRING_MAIL_USERNAME|SPRING_MAIL_PASSWORD|SHOP_SHIPPING_[A-Z_]+|SHOP_VIETTEL_[A-Z_]+)=(.*)$') {
            [Environment]::SetEnvironmentVariable($Matches[1],$Matches[2].Trim().Trim('"').Trim("'"),'Process')
        }
    }
}
}
}
Import-ShopLocalEnv
if ($Mode -eq 'Migrate') {
    Write-Host 'V001 through V008 have already been applied on this machine. Review SHOP_MIGRATION first; do not rerun schema.sql.'
    Write-Host 'For a different existing database: use SQL*Plus with V001 through V007 in order once, after backup and duplicate preflight.'
    exit
}
if ($Mode -eq 'Build' -or $Mode -eq 'Test') {
    if ($Mode -eq 'Test') {
        $stageEnvPath=Join-Path $projectRoot '.local/stage.env'
        if (-not (Test-Path -LiteralPath $stageEnvPath)) { throw 'Create the isolated CLOTHING_SHOP_STAGE schema first; tests never mutate the active shop.' }
        foreach($stageLine in Get-Content -LiteralPath $stageEnvPath){
            if($stageLine -match '^([^=]+)=(.*)$'){
                [Environment]::SetEnvironmentVariable($Matches[1],$Matches[2],'Process')
            }
        }
        $env:RUN_ORACLE_TESTS='false'
        $env:RUN_V8_TESTS='true'
        $env:SHOP_UPLOAD_DIR=Join-Path $projectRoot '.local/test-uploads'
        $env:SHOP_PRIVATE_DIR=Join-Path $projectRoot '.local/test-private'
        $env:SHOP_RESET_DELIVERY='local'
    }
    & $mavenCmd -f backend_java/pom.xml $(if($Mode -eq 'Build'){'package'}else{'test'}) '-DargLine=-Dnet.bytebuddy.experimental=true'
    exit $LASTEXITCODE
}
$jar = Join-Path $projectRoot 'backend_java/target/clothing-shop-0.0.1-SNAPSHOT.jar'
if (-not (Test-Path -LiteralPath $jar)) { throw 'Build first: .\tools\shop.ps1 -Mode Build' }
Set-Location -LiteralPath (Join-Path $projectRoot 'backend_java')
if ($Mode -eq 'ConfigureSmtp') {
    . (Join-Path $PSScriptRoot 'configure-smtp.ps1')
    if (-not (Invoke-ShopGmailSetup -Java $javaCmd -Jar $jar -ConfigPath $localEnv)) { exit 1 }
    Import-ShopLocalEnv
    $listener = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($listener) {
        $currentBackend = Get-CimInstance Win32_Process -Filter "ProcessId=$($listener.OwningProcess)"
        if ($currentBackend.CommandLine -and $currentBackend.CommandLine.Contains($jar)) { Stop-Process -Id $listener.OwningProcess }
        else { Write-Host 'SMTP da luu. Backend dang chay tu IDE/ung dung khac; hay khoi dong lai backend de nap cau hinh moi.'; exit 0 }
    }
    $logs = Join-Path $projectRoot '.local'
    [IO.Directory]::CreateDirectory($logs) | Out-Null
    $backendProcess = Start-Process -FilePath $javaCmd -ArgumentList @('-jar',('"'+$jar+'"')) -WorkingDirectory (Join-Path $projectRoot 'backend_java') -WindowStyle Hidden -RedirectStandardOutput (Join-Path $logs 'server.log') -RedirectStandardError (Join-Path $logs 'server-error.log') -PassThru
    $backendProcess.Id | Set-Content -LiteralPath (Join-Path $logs 'server.pid')
    Write-Host 'Da khoi dong lai backend voi SMTP. Mo http://localhost:8080/forgot-password de thu gui thu.'
    exit 0
} elseif ($Mode -eq 'TestSmtp') {
    & $javaCmd -jar $jar --smtp-check --spring.main.web-application-type=none --spring.main.banner-mode=off
} elseif ($Mode -eq 'CreateAdmin') {
    & $javaCmd -jar $jar --create-admin --spring.main.web-application-type=none
} elseif ($Mode -eq 'CleanImages') {
    & $javaCmd -jar $jar --cleanup-images --spring.main.web-application-type=none
} else {
    & $javaCmd -jar $jar
}
exit $LASTEXITCODE
