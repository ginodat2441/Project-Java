$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'configure-smtp.ps1')
$projectRoot=Split-Path -Parent $PSScriptRoot
$testDirectory=Join-Path $projectRoot ('.local/smtp-config-test-'+[Guid]::NewGuid().ToString('N'))
$testFile=Join-Path $testDirectory 'oracle-local.env'
try{
    [IO.Directory]::CreateDirectory($testDirectory) | Out-Null
    [IO.File]::WriteAllText($testFile,"# preserved comment`r`nSPRING_DATASOURCE_PASSWORD=dummy-oracle-value`r`nSPRING_MAIL_HOST=old.example.test`r`n",[Text.UTF8Encoding]::new($false))
    $values=[ordered]@{SPRING_MAIL_HOST='smtp.gmail.com';SPRING_MAIL_PASSWORD=('X'*16);SHOP_RESET_DELIVERY='smtp'}
    Write-ShopSmtpEnvironment -Path $testFile -Values $values
    $text=[IO.File]::ReadAllText($testFile)
    if(-not $text.Contains('SPRING_DATASOURCE_PASSWORD=dummy-oracle-value')){throw 'Original Oracle config changed.'}
    if(-not $text.Contains('# preserved comment')){throw 'Comment changed.'}
    if(-not $text.Contains('SPRING_MAIL_HOST=smtp.gmail.com')){throw 'SMTP config missing.'}
    if(-not (Get-Acl -LiteralPath $testFile).AreAccessRulesProtected){throw 'Secret file ACL is not protected.'}
    Write-ShopSmtpEnvironment -Path $testFile -Values $values
    if(@([IO.File]::ReadAllLines($testFile) | Where-Object {$_ -match '^SPRING_MAIL_HOST='}).Count -ne 1){throw 'Duplicate SMTP keys.'}
    $before=[IO.File]::ReadAllText($testFile)
    try{Write-ShopSmtpEnvironment -Path $testFile -Values ([ordered]@{SPRING_MAIL_HOST="bad`nINJECTED=value"});throw 'Unsafe multiline value accepted.'}
    catch{if($_.Exception.Message -eq 'Unsafe multiline value accepted.'){throw}}
    if([IO.File]::ReadAllText($testFile) -ne $before){throw 'Invalid input modified the original file.'}
    Write-Output 'SMTP config tests passed: Oracle preserved, atomic updates, no duplicate keys, private ACL, multiline injection rejected.'
}finally{
    if(Test-Path -LiteralPath $testFile){Remove-Item -LiteralPath $testFile -Force}
    if(Test-Path -LiteralPath $testDirectory){Remove-Item -LiteralPath $testDirectory -Force}
}
