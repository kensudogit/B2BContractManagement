$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

Write-Host "Starting PostgreSQL on :5435 ..."
docker compose up -d db
docker compose exec -T db pg_isready -U contracts_app -d contracts | Out-Null

Write-Host "Starting API on :8040 ..."
Start-Process -WorkingDirectory "$PSScriptRoot\api" powershell -ArgumentList "-NoExit","-Command","mvn spring-boot:run"

Write-Host "Starting Web on :3040 ..."
if (-not (Test-Path "$PSScriptRoot\web\node_modules")) {
  npm --prefix "$PSScriptRoot\web" install
}
Start-Process -WorkingDirectory "$PSScriptRoot\web" powershell -ArgumentList "-NoExit","-Command","npm run dev"

Write-Host "Open http://localhost:3040  (admin / password)"
