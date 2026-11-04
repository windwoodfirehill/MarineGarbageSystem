# Build backend with mvn package; install model-service requirements and frontend npm ci first.
# Set MODEL_DIR to an external directory containing v4b_best.pt, v4_best.pt and v3_best.pt.
$ErrorActionPreference = 'Stop'
$ProjectDir = Split-Path -Parent $PSScriptRoot
$PythonExe = if ($env:MARINE_PYTHON) { $env:MARINE_PYTHON } else { Join-Path $ProjectDir 'model-service/.venv/Scripts/python.exe' }
$JavaExe = (Get-Command java -ErrorAction Stop).Source
$NpmExe = (Get-Command npm.cmd -ErrorAction Stop).Source
$MediaExe = if ($env:MARINE_MEDIAMTX) { $env:MARINE_MEDIAMTX } else { (Get-Command mediamtx -ErrorAction Stop).Source }
$Jar = Join-Path $ProjectDir 'backend/target/marine-backend-1.0.0.jar'
if (-not (Test-Path -LiteralPath $PythonExe)) { throw 'Set MARINE_PYTHON or create model-service/.venv.' }
if (-not (Test-Path -LiteralPath $Jar)) { throw 'Build backend first: mvn package' }
foreach ($port in @(8000,8001,5173,1935)) { if (Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue) { throw "Port $port is occupied." } }
$MediaConfig = Join-Path $PSScriptRoot 'mediamtx.yml'
Start-Process -FilePath $MediaExe -ArgumentList @('"' + $MediaConfig + '"') -WorkingDirectory $PSScriptRoot -WindowStyle Hidden
Start-Process -FilePath $PythonExe -ArgumentList @('-m','uvicorn','app.model_api:app','--host','127.0.0.1','--port','8001') -WorkingDirectory (Join-Path $ProjectDir 'model-service') -WindowStyle Hidden
Start-Process -FilePath $JavaExe -ArgumentList @('-jar','target/marine-backend-1.0.0.jar') -WorkingDirectory (Join-Path $ProjectDir 'backend') -WindowStyle Hidden
Start-Process -FilePath $NpmExe -ArgumentList @('run','dev') -WorkingDirectory (Join-Path $ProjectDir 'frontend') -WindowStyle Hidden
Write-Host 'UI: http://localhost:5173  Java: 8000  YOLO: 8001  RTMP: 1935'
