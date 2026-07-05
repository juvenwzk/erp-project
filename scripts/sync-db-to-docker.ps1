# Sync dev database into Docker MySQL (erp-mysql)
# Default source: erp-server/src/main/resources/application-local.yaml
# Usage: .\scripts\sync-db-to-docker.ps1
#        .\scripts\sync-db-to-docker.ps1 -SourceHost 127.0.0.1 -SourceUser root -SourcePassword 123456

param(
    [string]$SourceHost,
    [int]$SourcePort = 3306,
    [string]$SourceUser,
    [string]$SourcePassword,
    [string]$SourceDatabase = "erp",
    [string]$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
)

$ErrorActionPreference = "Stop"
Set-Location $ProjectRoot

function Read-LocalYamlDatasource {
    param([string]$YamlPath)
    if (-not (Test-Path $YamlPath)) { return $null }
    $text = Get-Content $YamlPath -Raw -Encoding UTF8
    $urlMatch = [regex]::Match($text, 'jdbc:mysql://([^:/]+):(\d+)/([^?]+)')
    $userMatch = [regex]::Match($text, '(?m)^\s*username:\s*(.+)\s*$')
    $passMatch = [regex]::Match($text, '(?m)^\s*password:\s*(.+)\s*$')
    if (-not $urlMatch.Success) { return $null }
    return @{
        Host = $urlMatch.Groups[1].Value
        Port = [int]$urlMatch.Groups[2].Value
        Database = $urlMatch.Groups[3].Value
        User = if ($userMatch.Success) { $userMatch.Groups[1].Value.Trim() } else { "" }
        Password = if ($passMatch.Success) { $passMatch.Groups[1].Value.Trim() } else { "" }
    }
}

function Read-DotEnv {
    param([string]$Path)
    $map = @{}
    if (-not (Test-Path $Path)) { return $map }
    foreach ($line in Get-Content $Path -Encoding UTF8) {
        if ($line -match '^\s*([^#=]+)=(.*)$') {
            $map[$matches[1].Trim()] = $matches[2].Trim()
        }
    }
    return $map
}

$localYaml = Join-Path $ProjectRoot "erp-server\src\main\resources\application-local.yaml"
$ds = Read-LocalYamlDatasource $localYaml
if (-not $SourceHost -and $ds) {
    $SourceHost = $ds.Host
    $SourcePort = $ds.Port
    $SourceDatabase = $ds.Database
}
if (-not $SourceUser -and $ds) { $SourceUser = $ds.User }
if (-not $SourcePassword -and $ds) { $SourcePassword = $ds.Password }

if (-not $SourceHost -or -not $SourceUser) {
    throw "Cannot resolve source database. Pass params or configure application-local.yaml"
}

$envFile = Join-Path $ProjectRoot ".env"
$envMap = Read-DotEnv $envFile
$dockerRootPassword = if ($envMap["MYSQL_ROOT_PASSWORD"]) { $envMap["MYSQL_ROOT_PASSWORD"] } else { "123456" }
$dockerDatabase = if ($envMap["MYSQL_DATABASE"]) { $envMap["MYSQL_DATABASE"] } else { "erp" }
$dockerUser = if ($envMap["MYSQL_USER"]) { $envMap["MYSQL_USER"] } else { "ERP_USER" }
$dockerPassword = if ($envMap["MYSQL_PASSWORD"]) { $envMap["MYSQL_PASSWORD"] } else { "123456" }

if (-not $envMap["MYSQL_USER"]) { $envMap["MYSQL_USER"] = $dockerUser }
if (-not $envMap["MYSQL_PASSWORD"]) { $envMap["MYSQL_PASSWORD"] = $dockerPassword }
if (-not $envMap["MYSQL_ROOT_PASSWORD"]) { $envMap["MYSQL_ROOT_PASSWORD"] = $dockerRootPassword }
if (-not $envMap["MYSQL_DATABASE"]) { $envMap["MYSQL_DATABASE"] = $dockerDatabase }

$order = "MYSQL_USER","MYSQL_PASSWORD","MYSQL_ROOT_PASSWORD","MYSQL_DATABASE","DEEPSEEK_API_KEY","OSS_ACCESS_KEY_ID","OSS_ACCESS_KEY_SECRET","ALIYUN_OSS_ENDPOINT","ALIYUN_OSS_BUCKET","ALIYUN_OSS_REGION"
$lines = @("# Docker env - do not commit")
foreach ($key in $order) {
    if ($envMap.ContainsKey($key) -and $envMap[$key]) {
        $lines += ($key + "=" + $envMap[$key])
    }
}
Set-Content -Path $envFile -Value $lines -Encoding UTF8

$dumpFile = Join-Path $ProjectRoot "scripts\sql\sync_dump.sql"
$sqlDir = Join-Path $ProjectRoot "scripts\sql"
Write-Host "Source: ${SourceUser}@${SourceHost}:${SourcePort}/${SourceDatabase}"

Write-Host "Exporting source database..."
docker run --rm -v "${sqlDir}:/dump" mysql:8.0 mysqldump `
    -h $SourceHost -P $SourcePort -u $SourceUser -p"$SourcePassword" `
    --single-transaction --routines --triggers --set-gtid-purged=OFF `
    --default-character-set=utf8mb4 --no-tablespaces `
    -r /dump/sync_dump.sql $SourceDatabase

if (-not (Test-Path $dumpFile) -or (Get-Item $dumpFile).Length -lt 100) {
    throw "Export failed. Check source DB connectivity and credentials."
}
Write-Host "Export OK: $dumpFile ($('{0:N0}' -f (Get-Item $dumpFile).Length) bytes)"

Write-Host "Starting Docker MySQL..."
docker compose up -d mysql | Out-Null

$healthy = $false
for ($i = 0; $i -lt 30; $i++) {
    $status = docker inspect -f '{{.State.Health.Status}}' erp-mysql 2>$null
    if ($status -eq "healthy") { $healthy = $true; break }
    Start-Sleep -Seconds 2
}
if (-not $healthy) {
    throw "erp-mysql is not healthy. Run: docker logs erp-mysql"
}

Write-Host "Importing into Docker MySQL..."
$recreateSql = "DROP DATABASE IF EXISTS ``$dockerDatabase``; CREATE DATABASE ``$dockerDatabase`` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
$recreateSql | docker exec -i erp-mysql mysql -uroot -p"$dockerRootPassword"
docker cp $dumpFile erp-mysql:/tmp/sync_dump.sql
docker exec erp-mysql mysql -uroot -p"$dockerRootPassword" --default-character-set=utf8mb4 $dockerDatabase -e "source /tmp/sync_dump.sql"
docker exec erp-mysql rm -f /tmp/sync_dump.sql

Write-Host "Ensuring AUTO_INCREMENT..."
$ensureSql = Join-Path $ProjectRoot "scripts\sql\ensure_auto_increment.sql"
if (Test-Path $ensureSql) {
    docker cp $ensureSql erp-mysql:/tmp/ensure_auto_increment.sql | Out-Null
    docker exec erp-mysql mysql -uroot -p"$dockerRootPassword" --default-character-set=utf8mb4 -e "source /tmp/ensure_auto_increment.sql" 2>$null | Out-Null
    docker exec erp-mysql rm -f /tmp/ensure_auto_increment.sql | Out-Null
}

Write-Host "Ensuring app user $dockerUser ..."
$grantSql = "CREATE USER IF NOT EXISTS '$dockerUser'@'%' IDENTIFIED BY '$dockerPassword'; GRANT SELECT, INSERT, UPDATE, DELETE ON ``$dockerDatabase``.* TO '$dockerUser'@'%'; FLUSH PRIVILEGES;"
$grantSql | docker exec -i erp-mysql mysql -uroot -p"$dockerRootPassword"

Write-Host "Restarting erp-app and nginx..."
docker compose up -d erp-app nginx | Out-Null

Write-Host ""
Write-Host "Done."
Write-Host "  Source: ${SourceUser}@${SourceHost}/${SourceDatabase}"
Write-Host "  Target: Docker erp-mysql / $dockerDatabase"
Write-Host "  URL: http://localhost:100"
