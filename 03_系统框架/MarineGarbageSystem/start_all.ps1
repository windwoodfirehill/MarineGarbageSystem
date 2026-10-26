# -*- powershell -*-
# 一键启动：后端 FastAPI + 前端 Vue dev server
# 用法：右键"使用 PowerShell 运行"，或 powershell -File start_all.ps1

$BackendDir = "C:\Users\93130\Desktop\MarineGarbageSystem\backend"
$FrontendDir = "C:\Users\93130\Desktop\MarineGarbageSystem\frontend"
$Py = "C:\Users\93130\Desktop\WaterWasteDetection\venv\Scripts\python.exe"

Write-Host "启动后端 (http://127.0.0.1:8000) ..." -ForegroundColor Green
Start-Process -FilePath $Py -ArgumentList "-m","uvicorn","app.main:app","--host","127.0.0.1","--port","8000" -WorkingDirectory $BackendDir -WindowStyle Minimized

Write-Host "启动前端 (http://127.0.0.1:5173) ..." -ForegroundColor Green
Start-Process -FilePath "cmd.exe" -ArgumentList "/c","npm run dev" -WorkingDirectory $FrontendDir -WindowStyle Minimized

Start-Sleep -Seconds 3
Start-Process "http://127.0.0.1:5173"
Write-Host "已启动。前端 5173，后端 8000。关闭本窗口不影响服务。" -ForegroundColor Cyan
