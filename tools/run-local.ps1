param(
    [ValidateSet('Build','Run')][string]$Mode = 'Run',
    [ValidateSet('Client','Admin')][string]$Service = 'Client'
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
foreach ($key in 'SPRING_DATASOURCE_URL','SPRING_DATASOURCE_USERNAME','SPRING_DATASOURCE_PASSWORD') {
    if (-not [Environment]::GetEnvironmentVariable($key, 'Process')) { throw "Missing $key in local configuration." }
}
$candidates = @($env:JAVA_HOME, 'G:\oracle\sqldeveloper\jdk\jre')
foreach ($candidate in $candidates) {
    if ($candidate -and (Test-Path -LiteralPath (Join-Path $candidate 'bin/javac.exe'))) { $env:JAVA_HOME=$candidate; break }
}
if (-not $env:JAVA_HOME -or -not (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin/javac.exe'))) { throw 'Set JAVA_HOME to JDK 17 or newer.' }
$env:Path = "$(Join-Path $env:JAVA_HOME 'bin');$env:Path"
$java = Join-Path $env:JAVA_HOME 'bin/java.exe'
$module = if ($Service -eq 'Client') { 'client_backend' } else { 'admin/admin_code' }
if ($Mode -eq 'Build') {
    $mvn = if (Get-Command mvn -ErrorAction SilentlyContinue) { 'mvn' } else { Join-Path $root '.local/apache-maven-3.9.9/bin/mvn.cmd' }
    & $mvn "-Dmaven.repo.local=$(Join-Path $env:USERPROFILE '.m2/repository')" -f (Join-Path $root "$module/pom.xml") clean package
    exit $LASTEXITCODE
}
$jar = Join-Path $root "$module/target/clothing-shop-0.0.1-SNAPSHOT.jar"
if (-not (Test-Path -LiteralPath $jar)) { throw "Build first: .\shop.cmd -Mode Build -Service $Service" }
Set-Location -LiteralPath (Join-Path $root $module)
& $java -jar $jar
exit $LASTEXITCODE
