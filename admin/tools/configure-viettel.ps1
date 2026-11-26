param()
$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent $PSScriptRoot
Write-Host 'Viettel Post: enter real API credentials and pickup warehouse. No account is created by this script.'
$environment=Read-Host 'Environment: production or development'
if($environment -notin @('production','development')){throw 'Invalid environment'}
$origin=Read-Host 'Full pickup address: street, ward, province'
if([string]::IsNullOrWhiteSpace($origin) -or $origin.Length -gt 500 -or $origin.Contains("`n") -or $origin.Contains("`r")){throw 'Enter a valid warehouse address'}
$secret=Read-Host 'Viettel Post API token (hidden)' -AsSecureString
$pointer=[Runtime.InteropServices.Marshal]::SecureStringToBSTR($secret)
try{$token=[Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)}finally{[Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)}
if([string]::IsNullOrWhiteSpace($token) -or $token -notmatch '^[A-Za-z0-9._~+/-]+=*$'){throw 'Invalid token format'}
$values=@{}
foreach($item in @(@('GRAMS_PER_ITEM','Conservative packed grams per product (confirm by weighing)',1,10000),@('PACKING_GRAMS','Additional packaging grams',0,5000),@('LENGTH_CM','Parcel length cm',1,200),@('WIDTH_CM','Parcel width cm',1,200),@('HEIGHT_CM','Parcel height cm',1,200))){
 $parsed=0;$inputValue=Read-Host $item[1]
 if(-not [int]::TryParse($inputValue,[ref]$parsed) -or $parsed -lt $item[2] -or $parsed -gt $item[3]){throw ('Invalid parcel value: '+$item[0])}
 $values[$item[0]]=$parsed
}
$settings=[ordered]@{
 SHOP_SHIPPING_ENABLED='true'; SHOP_VIETTEL_ENVIRONMENT=$environment
 SHOP_VIETTEL_TOKEN=$token; SHOP_SHIPPING_ORIGIN=$origin
}
foreach($entry in $values.GetEnumerator()){$settings['SHOP_SHIPPING_'+$entry.Key]=$entry.Value}
$configPath=Join-Path $projectRoot '.vscode/viettel-local.env'
. (Join-Path $PSScriptRoot 'configure-smtp.ps1')
Write-ShopSmtpEnvironment -Path $configPath -Values $settings
$token=$null;$lines=$null;$secret.Dispose()
Write-Host 'Saved private Viettel Post configuration. Restart backend with .\shop.cmd -Mode Run.'
Write-Host 'This saves configuration only. Verify an actual quote with your Viettel account before accepting orders.'
