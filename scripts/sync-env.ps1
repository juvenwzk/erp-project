# Sync OSS / DeepSeek credentials from Windows environment variables into .env for Docker
$envPath = Join-Path $PSScriptRoot "..\.env"
if (-not (Test-Path $envPath)) {
    Copy-Item (Join-Path $PSScriptRoot "..\.env.example") $envPath
}

$lines = Get-Content $envPath -Encoding UTF8
$map = @{}
foreach ($line in $lines) {
    if ($line -match '^\s*([^#=]+)=(.*)$') {
        $map[$matches[1].Trim()] = $matches[2].Trim()
    }
}

if ($env:DEEPSEEK_API_KEY) { $map["DEEPSEEK_API_KEY"] = $env:DEEPSEEK_API_KEY.Trim() }
if ($env:OSS_ACCESS_KEY_ID) { $map["OSS_ACCESS_KEY_ID"] = $env:OSS_ACCESS_KEY_ID.Trim() }
if ($env:OSS_ACCESS_KEY_SECRET) { $map["OSS_ACCESS_KEY_SECRET"] = $env:OSS_ACCESS_KEY_SECRET.Trim() }
if ($env:ALIYUN_OSS_ENDPOINT) { $map["ALIYUN_OSS_ENDPOINT"] = $env:ALIYUN_OSS_ENDPOINT.Trim() }
if ($env:ALIYUN_OSS_BUCKET) { $map["ALIYUN_OSS_BUCKET"] = $env:ALIYUN_OSS_BUCKET.Trim() }
if ($env:ALIYUN_OSS_REGION) { $map["ALIYUN_OSS_REGION"] = $env:ALIYUN_OSS_REGION.Trim() }

$order = @(
    "MYSQL_ROOT_PASSWORD", "MYSQL_DATABASE", "DEEPSEEK_API_KEY",
    "OSS_ACCESS_KEY_ID", "OSS_ACCESS_KEY_SECRET",
    "ALIYUN_OSS_ENDPOINT", "ALIYUN_OSS_BUCKET", "ALIYUN_OSS_REGION"
)
$out = @("# Docker env - do not commit")
foreach ($key in $order) {
    if ($map.ContainsKey($key)) { $out += ($key + "=" + $map[$key]) }
}
Set-Content -Path $envPath -Value $out -Encoding UTF8

$ok = $true
$dsKey = $map["DEEPSEEK_API_KEY"]
if ([string]::IsNullOrWhiteSpace($dsKey) -or $dsKey -eq "your-deepseek-api-key") {
    Write-Host "WARN: DEEPSEEK_API_KEY missing or placeholder. AI Agent will not work in Docker."
    $ok = $false
} else {
    Write-Host "OK: DEEPSEEK_API_KEY synced to .env (length=$($dsKey.Length))"
}

$id = $map["OSS_ACCESS_KEY_ID"]
$secret = $map["OSS_ACCESS_KEY_SECRET"]
if ((-not [string]::IsNullOrWhiteSpace($id)) -and (-not [string]::IsNullOrWhiteSpace($secret))) {
    Write-Host "OK: OSS credentials synced to .env"
} else {
    Write-Host "WARN: OSS keys missing. Image upload may fail in Docker."
    $ok = $false
}

if (-not $ok) { exit 1 }
