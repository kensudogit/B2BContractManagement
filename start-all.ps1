$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
$env:COMPOSE_BAKE = "false"
Remove-Item Env:BUILDX_BUILDER -ErrorAction SilentlyContinue
docker context use desktop-linux | Out-Null

Write-Host "Starting db / api / web (UI :3040, API :8040, Postgres :5435) ..."
docker compose up --build -d
docker compose ps
Write-Host "Open http://localhost:3040"
Write-Host "Demo users: sales / approver / legal / admin  (password)"
