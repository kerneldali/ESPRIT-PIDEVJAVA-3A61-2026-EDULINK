import pandas as pd
import json
import pickle
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.ensemble import RandomForestClassifier
from sklearn.pipeline import Pipeline
import os

# 1. Chargement des données
df = pd.read_csv('dataset.csv')

# On combine le titre et le but pour donner plus de contexte au modèle
df['text'] = df['challenge_title'] + " " + df['challenge_goal']

# 2. Préparation du pipeline
# On utilise TF-IDF pour transformer le texte en vecteurs numériques
# Et un Random Forest pour classer dans les catégories
model = Pipeline([
    ('tfidf', TfidfVectorizer()),
    ('clf', RandomForestClassifier(n_estimators=100))
])

# 3. Entraînement pour la classification de catégorie
# Note: Dans une version plus avancée, on pourrait prédire directement le JSON, 
# mais ici nous allons prédire la catégorie et mapper les tâches associées.
model.fit(df['text'], df['category'])

# 4. Sauvegarde du modèle
if not os.path.exists('models'):
    os.makedirs('models')

with open('models/challenge_model.pkl', 'wb') as f:
    pickle.dump(model, f)

# On sauvegarde aussi un dictionnaire des tâches par catégorie pour la "génération"
category_tasks = df.groupby('category')['generated_tasks'].first().to_dict()
# On convertit les strings JSON en vrais objets listes
for cat in category_tasks:
    category_tasks[cat] = json.loads(category_tasks[cat])

with open('models/category_tasks.pkl', 'wb') as f:
    pickle.dump(category_tasks, f)

print("Modèle entraîné et sauvegardé avec succès dans le dossier 'models/'.")
