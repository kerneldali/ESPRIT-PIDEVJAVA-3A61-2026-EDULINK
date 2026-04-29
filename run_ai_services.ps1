# EduLink AI Services Runner
# This script starts the 2 local AI APIs required for Help Requests

Write-Host "🚀 Starting EduLink AI Services..." -ForegroundColor Cyan

$basePath = "$PSScriptRoot\ai\helprequest_models"

# 1. Main AI Hub (Port 5000) - Classification, Suggestion, Toxicity, Sentiment, Language, Smart-Match
Write-Host "Starting Main AI Hub (Port 5000)..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$basePath\suggestion_api\ml'; pip install -r requirements.txt; python api.py" -WindowStyle Normal

# 2. VADER Academic Sentiment API (Port 5001)
Write-Host "Starting VADER Sentiment API (Port 5001)..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$basePath\toxicity_api'; pip install -r requirements.txt; python app.py" -WindowStyle Normal

Write-Host "✅ All AI services triggered. Keep the terminal windows open!" -ForegroundColor Green
