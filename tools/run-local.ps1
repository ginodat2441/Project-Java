param(
    [ValidateSet('Build','Run')][string]$Mode = 'Run',
    [ValidateSet('Client','Admin','Both')][string]$Service = 'Client'
)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $root
$envFile = Join-Path $root '.vscode/oracle-local.env'
if (-not (Test-Path -LiteralPath $envFile)) { throw 'Missing .vscode/oracle-local.env. Add your local Oracle configuration first.' }
foreach ($line in Get-Content -LiteralPath $envFile -Encoding UTF8) {
    if ($line -match '^\s*([A-Z][A-Z0-9_]*)=(.*)$') {
        [Environment]::SetEnvironmentVariable($Matches[1], $Matches[2].Trim().Trim('"').Trim("'"), 'Process')
    }
}
if (-not $env:SPRING_DATASOURCE_PASSWORD) {
    throw 'Missing SPRING_DATASOURCE_PASSWORD in local configuration.'
}
$javac = Get-Command javac.exe -ErrorAction SilentlyContinue
$candidates = @($env:JAVA_HOME)
if ($javac) { $candidates += Split-Path -Parent (Split-Path -Parent $javac.Source) }
$candidates += 'G:\oracle\sqldeveloper\jdk\jre'
$candidates += @(Get-ChildItem (Join-Path $env:USERPROFILE 'Downloads') -Directory -Filter 'sqldeveloper-*' -ErrorAction SilentlyContinue |
    ForEach-Object { Join-Path $_.FullName 'sqldeveloper/jdk/jre' })
foreach ($candidate in $candidates) {
    if ($candidate -and (Test-Path -LiteralPath ([IO.Path]::Combine($candidate, 'bin/javac.exe')))) { $env:JAVA_HOME=$candidate; break }
}
if (-not $env:JAVA_HOME -or -not (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin/javac.exe'))) { throw 'Set JAVA_HOME to JDK 17 or newer.' }
$env:Path = "$(Join-Path $env:JAVA_HOME 'bin');$env:Path"
$java = Join-Path $env:JAVA_HOME 'bin/java.exe'
$services = if ($Service -eq 'Both') { @('Client', 'Admin') } else { @($Service) }
$modules = @{ Client = 'client_backend'; Admin = 'admin/admin_code' }
$ports = @{ Client = 8081; Admin = 8888 }
if ($Mode -eq 'Build') {
    $mvn = if (Get-Command mvn -ErrorAction SilentlyContinue) { 'mvn' } else { Join-Path $root '.local/apache-maven-3.9.9/bin/mvn.cmd' }
    foreach ($name in $services) {
        & $mvn "-Dmaven.repo.local=$(Join-Path $env:USERPROFILE '.m2/repository')" -f (Join-Path $root "$($modules[$name])/pom.xml") clean package
        if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    }
    exit 0
}

$clientFiles = [Uri]((Join-Path $root 'client') + [IO.Path]::DirectorySeparatorChar)
$launches = foreach ($name in $services) {
    $directory = Join-Path $root $modules[$name]
    $jar = Join-Path $directory 'target/clothing-shop-0.0.1-SNAPSHOT.jar'
    $port = $ports[$name]
    $path = if ($name -eq 'Admin') { '/login' } else { '/' }
    $url = "http://localhost:$port$path"
    $listeners = @(Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue)
    if ($listeners.Count -gt 0) {
        $owners = @($listeners.OwningProcess | Sort-Object -Unique)
        $owner = Get-CimInstance Win32_Process -Filter "ProcessId=$($owners[0])"
        $jarPattern = [regex]::Escape($jar.Replace('/', '\'))
        if ($owners.Count -eq 1 -and $owner.Name -eq 'java.exe' -and
            $owner.CommandLine -match ('(?:^|\s)-jar\s+(?:"' + $jarPattern + '"|' + $jarPattern + ')(?:\s|$)')) {
            Write-Host "$name is already running at $url"
            continue
        }
        throw "$name port $port is in use by another process (PID $($owners -join ', '))."
    }
    if (-not (Test-Path -LiteralPath $jar)) { throw "Build first: .\shop.cmd -Mode Build -Service $Service" }
    $javaArgs = @('-jar', ('"' + $jar + '"'), "--server.port=$port", '--server.address=127.0.0.1')
    if ($name -eq 'Client') {
        $javaArgs += ('--spring.web.resources.static-locations=classpath:/static/,' + $clientFiles.AbsoluteUri)
    }
    Write-Host "$name $url"
    @{ Arguments = $javaArgs; Directory = $directory; Name = $name }
}

if (-not $launches) { exit 0 }

if ($Service -ne 'Both') {
    Set-Location -LiteralPath $launches.Directory
    $javaArgs = $launches.Arguments
    $javaArgs[1] = $javaArgs[1].Trim('"')
    & $java @javaArgs
    exit $LASTEXITCODE
}

$processes = @()
try {
    foreach ($launch in $launches) {
        $processes += Start-Process -FilePath $java -ArgumentList $launch.Arguments -WorkingDirectory $launch.Directory -NoNewWindow -PassThru
    }
    while ($true) {
        foreach ($process in $processes) {
            if ($process.WaitForExit(250)) {
                if ($process.ExitCode -ne 0) { exit $process.ExitCode }
                exit 0
            }
        }
    }
} finally {
    foreach ($process in $processes) {
        if (-not $process.HasExited) { Stop-Process -Id $process.Id -ErrorAction SilentlyContinue }
    }
}
