from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from edge_tts import Communicate
import os
import uuid
from fastapi.responses import FileResponse
import pickle

app = FastAPI(title="Edulink AI API")

# Modèles de données pour les requêtes
class PredictionRequest(BaseModel):
    challenge_title: str
    challenge_goal: str

# 1. Chargement des modèles au démarrage
MODEL_PATH = "models/challenge_model.pkl"
TASKS_PATH = "models/category_tasks.pkl"

if not os.path.exists(MODEL_PATH) or not os.path.exists(TASKS_PATH):
    raise RuntimeError("Les modèles n'ont pas été trouvés. Lancez 'train.py' d'abord.")

with open(MODEL_PATH, 'rb') as f:
    model = pickle.load(f)

with open(TASKS_PATH, 'rb') as f:
    category_tasks = pickle.load(f)

@app.post("/predict")
async def predict(request: PredictionRequest):
    try:
        # On prépare le texte comme lors de l'entraînement
        text = f"{request.challenge_title} {request.challenge_goal}"
        
        # 2. Prédiction de la catégorie
        category = model.predict([text])[0]
        
        # 3. Récupération des tâches associées à la catégorie
        tasks = category_tasks.get(category, [])
        
        return {
            "challenge_title": request.challenge_title,
            "predicted_category": category,
            "generated_tasks": tasks
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/status")
async def status():
    return {"status": "ready", "model_loaded": True}

@app.post("/voice")
async def text_to_speech(request: dict):
    text = request.get("text", "")
    if not text:
        return {"error": "No text provided"}
    
    # Créer un dossier temporaire pour les audios si besoin
    os.makedirs("temp_audio", exist_ok=True)
    filename = f"temp_audio/{uuid.uuid4()}.mp3"
    
    # Utiliser une voix française haute qualité (Henri ou Denise)
    communicate = Communicate(text, "fr-FR-HenriNeural")
    await communicate.save(filename)
    
    return FileResponse(filename, media_type="audio/mpeg")

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
