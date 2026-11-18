param(
    [ValidateSet('Build','Run')][string]$Mode = 'Run',
    [ValidateSet('Client','Admin')][string]$Service = 'Client'
)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $root
$envFile = Join-Path $root '.vscode/oracle-local.env'
if (Test-Path -LiteralPath $envFile) {
    foreach ($line in Get-Content -LiteralPath $envFile -Encoding UTF8) {
        if ($line -match '^\s*([A-Z][A-Z0-9_]*)=(.*)$') {
            [Environment]::SetEnvironmentVariable($Matches[1], $Matches[2].Trim().Trim('"').Trim("'"), 'Process')
        }
    }
}
$databaseKeys = @('SPRING_DATASOURCE_URL','SPRING_DATASOURCE_USERNAME','SPRING_DATASOURCE_PASSWORD')
$missingDatabaseKeys = @($databaseKeys | Where-Object {
    -not [Environment]::GetEnvironmentVariable($_, 'Process')
})
if ($missingDatabaseKeys.Count -gt 0 -and $missingDatabaseKeys.Count -lt $databaseKeys.Count) {
    throw "Incomplete Oracle configuration. Set all of: $($databaseKeys -join ', '), or leave them unset to use application.properties."
}
if ($missingDatabaseKeys.Count -eq $databaseKeys.Count) {
    Write-Host 'No Oracle environment override found; using the datasource settings from application.properties.'
}
$candidates = @($env:JAVA_HOME, 'G:\oracle\sqldeveloper\jdk\jre')
foreach ($candidate in $candidates) {
    if ($candidate -and (Test-Path -LiteralPath "$candidate\bin\javac.exe")) { $env:JAVA_HOME=$candidate; break }
}
if (-not $env:JAVA_HOME -or -not (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin/javac.exe'))) { throw 'Set JAVA_HOME to JDK 17 or newer.' }
$env:Path = "$(Join-Path $env:JAVA_HOME 'bin');$env:Path"
$java = Join-Path $env:JAVA_HOME 'bin/java.exe'
$module = if ($Service -eq 'Client') { 'client_backend' } else { 'admin/admin_code' }
if ($Mode -eq 'Build') {
    $mvn = if (Get-Command mvn -ErrorAction SilentlyContinue) { 'mvn' } else { Join-Path $root '.local/apache-maven-3.9.9/bin/mvn.cmd' }
    & $mvn "-Dmaven.repo.local=$(Join-Path $env:USERPROFILE '.m2/repository')" '-DargLine=-Dnet.bytebuddy.experimental=true' -f (Join-Path $root "$module/pom.xml") clean package
    exit $LASTEXITCODE
}
$jar = Join-Path $root "$module/target/clothing-shop-0.0.1-SNAPSHOT.jar"
if (-not (Test-Path -LiteralPath $jar)) { throw "Build first: .\shop.cmd -Mode Build -Service $Service" }
Set-Location -LiteralPath (Join-Path $root $module)
& $java -jar $jar
exit $LASTEXITCODE
