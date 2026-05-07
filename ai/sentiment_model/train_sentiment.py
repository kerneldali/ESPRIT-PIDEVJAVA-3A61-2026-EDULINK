import pandas as pd
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.pipeline import Pipeline
import joblib
import os

# 1. Load Data
data_path = 'sentiment_dataset.csv'
df = pd.read_csv(data_path)

# 2. Build Pipeline
model = Pipeline([
    ('tfidf', TfidfVectorizer()),
    ('clf', LogisticRegression())
])

# 3. Train
print("Training sentiment model...")
model.fit(df['text'], df['label'])

# 4. Save
joblib.dump(model, 'sentiment_model.joblib')
print("Model saved to sentiment_model.joblib")
