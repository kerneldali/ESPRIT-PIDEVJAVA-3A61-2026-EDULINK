# EduLink AI Services Runner
# This script starts the 3 local AI APIs required for Help Requests

Write-Host "🚀 Starting EduLink AI Services..." -ForegroundColor Cyan

$basePath = "$PSScriptRoot\ai\helprequest_models"

# 1. Toxicity API (Port 5001)
Write-Host "Starting Toxicity API (Port 5001)..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$basePath\toxicity_api'; pip install -r requirements.txt; python app.py" -WindowStyle Normal

# 2. Suggestion API (Port 5004)
Write-Host "Starting Suggestion API (Port 5004)..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$basePath\suggestion_api'; pip install -r requirements.txt; python app.py" -WindowStyle Normal

# 3. Classification API (Port 8000)
Write-Host "Starting Classification API (Port 8000)..." -ForegroundColor Yellow
# Note: uses same dependencies as suggestion_api
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$basePath\classification_api'; pip install fastapi uvicorn pydantic edge-tts scikit-learn; python app.py" -WindowStyle Normal

Write-Host "✅ All AI services triggered. Keep the terminal windows open!" -ForegroundColor Green
