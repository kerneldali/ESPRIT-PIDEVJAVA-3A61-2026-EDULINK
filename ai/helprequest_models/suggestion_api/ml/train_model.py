"""
Toxicity Detection Model Training
Dataset: Jigsaw Toxic Comment Classification (train.csv)
Model: TF-IDF + Logistic Regression pipeline
"""
import os
import re
import pandas as pd
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.pipeline import Pipeline
from sklearn.model_selection import train_test_split
from sklearn.metrics import classification_report
import joblib

DATASET_PATH = os.path.join(os.path.dirname(__file__), 'train.csv', 'train.csv')
MODEL_DIR = os.path.join(os.path.dirname(__file__), 'model')
MODEL_PATH = os.path.join(MODEL_DIR, 'toxicity_model.pkl')


def clean_text(text):
    """Basic text preprocessing."""
    if not isinstance(text, str):
        return ''
    text = re.sub(r'<[^>]+>', ' ', text)        # Remove HTML tags
    text = re.sub(r'https?://\S+', ' ', text)    # Remove URLs
    text = re.sub(r'[^a-zA-Z\s]', ' ', text)     # Keep only letters
    text = re.sub(r'\s+', ' ', text).strip()      # Normalize whitespace
    return text.lower()


def main():
    print("=" * 60)
    print("  TOXICITY MODEL TRAINING")
    print("=" * 60)

    # 1. Load dataset
    print("\n[1/5] Loading dataset...")
    df = pd.read_csv(DATASET_PATH)
    print(f"  Loaded {len(df)} rows")

    # 2. Create binary toxic label (any toxicity column = 1 means toxic)
    print("[2/5] Preparing labels...")
    toxic_cols = ['toxic', 'severe_toxic', 'obscene', 'threat', 'insult', 'identity_hate']
    df['is_toxic'] = df[toxic_cols].max(axis=1)
    print(f"  Toxic: {df['is_toxic'].sum()} | Clean: {(df['is_toxic'] == 0).sum()}")

    # 3. Clean text
    print("[3/5] Cleaning text...")
    df['clean_text'] = df['comment_text'].apply(clean_text)

    # 4. Train/test split
    X_train, X_test, y_train, y_test = train_test_split(
        df['clean_text'], df['is_toxic'],
        test_size=0.2, random_state=42, stratify=df['is_toxic']
    )
    print(f"  Train: {len(X_train)} | Test: {len(X_test)}")

    # 5. Build and train pipeline
    print("[4/5] Training model (TF-IDF + Logistic Regression)...")
    pipeline = Pipeline([
        ('tfidf', TfidfVectorizer(
            max_features=50000,
            ngram_range=(1, 2),
            min_df=2, #hthi tnajm ttna7a
            max_df=0.95,
            sublinear_tf=True
        )),
        ('clf', LogisticRegression(
            max_iter=1000,
            C=1.0,
            solver='liblinear',
            class_weight='balanced' # ki ywlou yasser klmet mriglin ma w klma 5yba ma ygoulch mrigla l ljomla
        ))
    ])

    pipeline.fit(X_train, y_train)

    # 6. Evaluate
    print("[5/5] Evaluating...")
    y_pred = pipeline.predict(X_test)
    print("\n" + classification_report(y_test, y_pred, target_names=['clean', 'toxic']))

    # 7. Save model
    os.makedirs(MODEL_DIR, exist_ok=True)
    joblib.dump(pipeline, MODEL_PATH)
    model_size = os.path.getsize(MODEL_PATH) / (1024 * 1024)
    print(f"Model saved to: {MODEL_PATH}")
    print(f"Model size: {model_size:.1f} MB")
    print("\nDone!")


if __name__ == '__main__':
    main()
