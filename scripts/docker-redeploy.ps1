# Build locally and redeploy to running Docker containers (no docker build pull)
$ErrorActionPreference = "Stop"
$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
Set-Location $ProjectRoot

$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
$env:Path = "$env:JAVA_HOME\bin;" + $env:Path
$mvn = "C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2025.2.6.1\plugins\maven\lib\maven3\bin\mvn.cmd"

Write-Host "==> Maven package..."
& $mvn -f "$ProjectRoot\pom.xml" clean package "-Dmaven.test.skip=true" -q
if ($LASTEXITCODE -ne 0) { throw "Maven build failed" }

$jar = Join-Path $ProjectRoot "erp-server\target\erp-server-1.0-SNAPSHOT.jar"
if (-not (Test-Path $jar)) { throw "Jar not found: $jar" }

Write-Host "==> Copy jar to erp-app..."
docker cp $jar erp-app:/app/app.jar

Write-Host "==> Ensure ERP_USER exists..."
$prevEap = $ErrorActionPreference
$ErrorActionPreference = 'Continue'
docker exec erp-mysql mysql -uroot -p123456 -e "CREATE USER IF NOT EXISTS 'ERP_USER'@'%' IDENTIFIED BY '123456'; GRANT SELECT, INSERT, UPDATE, DELETE ON erp.* TO 'ERP_USER'@'%'; FLUSH PRIVILEGES;" 2>&1 | Out-Null
$ErrorActionPreference = $prevEap

Write-Host "==> Ensure goods_supplier table..."
$prevEap = $ErrorActionPreference
$ErrorActionPreference = 'Continue'
docker exec erp-mysql mysql -uroot -p123456 erp -e "CREATE TABLE IF NOT EXISTS goods_supplier (id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, goods_id BIGINT NOT NULL, supplier_id BIGINT NOT NULL, supply_price DECIMAL(10,2) NULL, create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, UNIQUE KEY uk_goods_supplier (goods_id, supplier_id), KEY idx_supplier_id (supplier_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;" 2>&1 | Out-Null
$ErrorActionPreference = $prevEap

Write-Host "==> Restart containers..."
docker restart erp-app erp-nginx | Out-Null
Start-Sleep -Seconds 15

Write-Host "==> Health check..."
try {
    $health = Invoke-RestMethod -Uri "http://localhost:100/actuator/health" -TimeoutSec 10
    Write-Host "Health:" ($health | ConvertTo-Json -Compress)
} catch {
    Write-Host "WARN: health check via nginx failed, trying direct backend log..."
    docker logs erp-app --tail 8
}

Write-Host "Done. Open http://localhost:100/login.html"
