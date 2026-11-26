# Gmail SMTP setup. Passwords are read privately, passed through process environment,
# tested without sending mail, and saved only after successful authentication.
function Protect-ShopSecretFile {
    param([Parameter(Mandatory=$true)][string]$Path)
    $identity = [Security.Principal.WindowsIdentity]::GetCurrent()
    $acl = [IO.File]::GetAccessControl($Path,[Security.AccessControl.AccessControlSections]::Access)
    $acl.SetAccessRuleProtection($true,$false)
    foreach($rule in @($acl.Access)) { [void]$acl.RemoveAccessRuleSpecific($rule) }
    $acl.AddAccessRule((New-Object Security.AccessControl.FileSystemAccessRule($identity.User,'FullControl','Allow')))
    $systemSid = New-Object Security.Principal.SecurityIdentifier('S-1-5-18')
    $acl.AddAccessRule((New-Object Security.AccessControl.FileSystemAccessRule($systemSid,'FullControl','Allow')))
    [IO.File]::SetAccessControl($Path,$acl)
}

function Write-ShopSmtpEnvironment {
    param([Parameter(Mandatory=$true)][string]$Path,[Parameter(Mandatory=$true)][System.Collections.IDictionary]$Values)
    $resolvedPath = [IO.Path]::GetFullPath($Path)
    $directory = Split-Path -Parent $resolvedPath
    [IO.Directory]::CreateDirectory($directory) | Out-Null
    $lines = if(Test-Path -LiteralPath $resolvedPath){[IO.File]::ReadAllLines($resolvedPath)}else{@()}
    $retained = @($lines | Where-Object {
        if($_ -match '^\s*([A-Za-z_][A-Za-z0-9_]*)\s*='){-not $Values.Contains($Matches[1])}else{$true}
    })
    foreach($key in $Values.Keys){
        if($key -notmatch '^[A-Z_]+$' -or [string]$Values[$key] -match '[\r\n]'){throw 'Invalid environment value.'}
        $retained += "$key=$($Values[$key])"
    }
    $temporary = Join-Path $directory ('.smtp-'+[Guid]::NewGuid().ToString('N')+'.tmp')
    try{
        [IO.File]::WriteAllText($temporary,'',[Text.UTF8Encoding]::new($false))
        Protect-ShopSecretFile -Path $temporary
        [IO.File]::WriteAllLines($temporary,[string[]]$retained,[Text.UTF8Encoding]::new($false))
        if(Test-Path -LiteralPath $resolvedPath){Protect-ShopSecretFile -Path $resolvedPath;[IO.File]::Replace($temporary,$resolvedPath,[NullString]::Value)}else{[IO.File]::Move($temporary,$resolvedPath)}
        Protect-ShopSecretFile -Path $resolvedPath
    }finally{if(Test-Path -LiteralPath $temporary){Remove-Item -LiteralPath $temporary -Force}}
}

function Invoke-ShopGmailSetup {
    param([Parameter(Mandatory=$true)][string]$Java,[Parameter(Mandatory=$true)][string]$Jar,[Parameter(Mandatory=$true)][string]$ConfigPath)
    Write-Host 'Gmail SMTP: smtp.gmail.com:587, STARTTLS. Khong dung mat khau dang nhap Google.'
    Write-Host 'Tao App Password: https://myaccount.google.com/apppasswords (can bat xac minh 2 buoc).'
    $email = (Read-Host 'Dia chi Gmail gui thu').Trim()
    if($email -notmatch '^[^@\s=]+@[^@\s=]+\.[^@\s=]+$'){Write-Host 'Email khong hop le.';return $false}
    $secure = Read-Host 'Mat khau ung dung Gmail 16 ky tu (an khi nhap)' -AsSecureString
    $bstr = [IntPtr]::Zero
    $password = $null
    $previous = @{}
    try{
        $bstr=[Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
        $password=[Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr) -replace '\s',''
        if($password -notmatch '^[A-Za-z0-9]{16}$'){Write-Host 'Can mat khau UNG DUNG 16 ky tu, khong phai mat khau Gmail thong thuong.';return $false}
        $settings=[ordered]@{
            SPRING_MAIL_HOST='smtp.gmail.com';SPRING_MAIL_PORT='587';SPRING_MAIL_USERNAME=$email;SPRING_MAIL_PASSWORD=$password
            SHOP_RESET_FROM=$email;SHOP_SMTP_AUTH='true';SHOP_SMTP_STARTTLS='true';SHOP_SMTP_SSL='false';SHOP_RESET_DELIVERY='smtp'
        }
        if(-not $env:SHOP_RESET_BASE_URL){$settings['SHOP_RESET_BASE_URL']='http://localhost:8080'}
        foreach($key in $settings.Keys){$previous[$key]=[Environment]::GetEnvironmentVariable($key,'Process');[Environment]::SetEnvironmentVariable($key,$settings[$key],'Process')}
        Write-Host 'Dang kiem tra TLS va dang nhap SMTP (khong gui email)...'
        $previousPreference=$ErrorActionPreference
        try{
            $ErrorActionPreference='Continue'
            $output = & $Java -jar $Jar --smtp-check --spring.main.web-application-type=none --spring.main.banner-mode=off 2>&1
            $result=$LASTEXITCODE
        }finally{$ErrorActionPreference=$previousPreference}
        if($result -ne 0){
            $text=$output | Out-String
            if($text.Contains('SMTP_CHECK_AUTH_FAILED')){Write-Host 'Gmail tu choi dang nhap. Kiem tra email va App Password, khong dung mat khau dang nhap Google.'}
            else{Write-Host 'Khong ket noi duoc SMTP. Kiem tra Internet, cong 587 va App Password.'}
            Write-Host 'Chua luu cau hinh SMTP. Cau hinh cu duoc giu nguyen.'
            return $false
        }
        if(-not (($output | Out-String).Contains('SMTP_CHECK_OK'))){Write-Host 'Chua xac nhan duoc SMTP. Hay build ban moi truoc.';return $false}
        Write-ShopSmtpEnvironment -Path $ConfigPath -Values $settings
        Write-Host 'SMTP_CHECK_OK. Da luu cau hinh cuc bo, khong dua mat khau vao Git.'
        return $true
    }finally{
        foreach($key in $previous.Keys){[Environment]::SetEnvironmentVariable($key,$previous[$key],'Process')}
        if($bstr -ne [IntPtr]::Zero){[Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)}
        $secure.Dispose();$password=$null
    }
}
