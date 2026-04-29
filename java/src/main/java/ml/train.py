import pandas as pd
from sklearn.ensemble import RandomForestClassifier
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.compose import ColumnTransformer
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import StandardScaler, FunctionTransformer
import joblib
import os

def extract_date_features(df):
    # expect a dataframe with 'dateStart' column
    df_dates = pd.to_datetime(df['dateStart'])
    return pd.DataFrame({
        'month': df_dates.dt.month,
        'dayofweek': df_dates.dt.dayofweek
    })

def combine_text(df):
    # expect a dataframe with 'title' and 'description' columns
    # Filling NaNs to avoid errors
    title = df['title'].fillna('').astype(str)
    desc = df['description'].fillna('').astype(str)
    return (title + " " + desc).values

def main():
    csv_path = 'fictive_events_dataset.csv'
    if not os.path.exists(csv_path):
        print(f"Dataset {csv_path} not found. Please place it in the same directory.")
        return

    df = pd.read_csv(csv_path)
    required_cols = ['title', 'description', 'dateStart', 'maxCapacity', 'target']
    for col in required_cols:
        if col not in df.columns:
            print(f"Missing required column: {col}")
            return

    X = df[['title', 'description', 'dateStart', 'maxCapacity']]
    y = df['target']

    # Text pipeline
    text_transformer = Pipeline(steps=[
        ('combine', FunctionTransformer(combine_text, validate=False)),
        ('tfidf', TfidfVectorizer(max_features=1000, stop_words=None)) # Using default (no stop words filter) to support multiple languages like French
    ])

    # Date pipeline
    date_transformer = Pipeline(steps=[
        ('extract', FunctionTransformer(extract_date_features, validate=False)),
        ('scaler', StandardScaler())
    ])

    # Preprocessor
    preprocessor = ColumnTransformer(
        transformers=[
            ('text', text_transformer, ['title', 'description']),
            ('date', date_transformer, ['dateStart']),
            ('num', StandardScaler(), ['maxCapacity'])
        ])

    # Model pipeline
    pipeline = Pipeline(steps=[
        ('preprocessor', preprocessor),
        ('model', RandomForestClassifier(n_estimators=100, random_state=42))
    ])

    print("Training model...")
    pipeline.fit(X, y)
    
    score = pipeline.score(X, y)
    print(f"Training accuracy: {score:.4f}")

    model_path = 'event_model.pkl'
    joblib.dump(pipeline, model_path)
    print(f"Model saved to {model_path}")

if __name__ == '__main__':
    main()
