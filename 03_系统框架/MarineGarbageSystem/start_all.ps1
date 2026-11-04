# 启动模型服务、Java 后端和 Vue 前端。
$ErrorActionPreference = "Stop"
$ProjectDir = $PSScriptRoot
$PythonExe = if ($env:MARINE_PYTHON) { $env:MARINE_PYTHON } else { "D:\MarineGarbageEnv\Scripts\python.exe" }
$Jar = Join-Path $ProjectDir "java-backend/target/marine-backend-1.0.0.jar"
$JavaExe = (Get-Command java -ErrorAction Stop).Source
$NpmExe = (Get-Command npm.cmd -ErrorAction Stop).Source
if (-not (Test-Path $PythonExe)) { throw "未找到 Python 模型环境，请设置 MARINE_PYTHON 或安装 D:\MarineGarbageEnv。" }
if (-not (Test-Path $Jar)) { throw "未找到 Java 构建文件，请先运行 Maven package。" }
& $PythonExe -c "import fastapi, uvicorn, multipart, ultralytics, torch"
if ($LASTEXITCODE -ne 0) { throw "模型依赖不完整，请在 backend 目录安装 requirements.txt。" }
if (-not (Test-Path (Join-Path $ProjectDir "frontend/node_modules"))) { throw "前端依赖未安装，请先在 frontend 目录执行 npm ci。" }
foreach ($port in @(8000,8001,5173)) { if (Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue) { throw "端口 $port 已被占用，请先停止对应服务。" } }
if (-not (Get-NetTCPConnection -LocalPort 1935 -State Listen -ErrorAction SilentlyContinue)) {
    Start-Process -FilePath "D:\MarineGarbageTools\mediamtx.exe" -WorkingDirectory "D:\MarineGarbageTools" -WindowStyle Hidden
}
Start-Process -FilePath $PythonExe -ArgumentList @("-m", "uvicorn", "app.model_api:app", "--host", "127.0.0.1", "--port", "8001") -WorkingDirectory (Join-Path $ProjectDir "backend") -WindowStyle Hidden
Start-Process -FilePath $JavaExe -ArgumentList @("-jar", "target/marine-backend-1.0.0.jar") -WorkingDirectory (Join-Path $ProjectDir "java-backend") -WindowStyle Hidden
Start-Process -FilePath $NpmExe -ArgumentList @("run", "dev") -WorkingDirectory (Join-Path $ProjectDir "frontend") -WindowStyle Hidden
Write-Host "正在启动：模型服务 8001，Java 后端 8000，前端 5173。"
