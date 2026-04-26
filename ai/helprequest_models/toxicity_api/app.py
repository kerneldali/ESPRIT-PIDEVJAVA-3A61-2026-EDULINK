"""
EduLink Sentiment Analysis Microservice
Analyzes student journal notes and returns a motivational phrase if negative.
Uses NLTK VADER (Valence Aware Dictionary and sEntiment Reasoner) —
a rule-based sentiment model pre-trained and enhanced for educational context.
"""

import json
import os
import random

import nltk
from flask import Flask, jsonify, request
from flask_cors import CORS
from nltk.sentiment.vader import SentimentIntensityAnalyzer

# Download VADER lexicon on first run (cached after that)
nltk.download("vader_lexicon", quiet=True)

app = Flask(__name__)
CORS(app)  # Allow Symfony to call this API cross-origin

# Load motivational phrases from phrases.json
_phrases_path = os.path.join(os.path.dirname(__file__), "phrases.json")
with open(_phrases_path, encoding="utf-8") as f:
    MOTIVATIONAL_PHRASES = json.load(f)

# Enhance VADER with student/academic-specific vocabulary
# Positive: academic achievement words  |  Negative: stress/failure words
ACADEMIC_LEXICON = {
    # Stress / negativity signals (make VADER more sensitive)
    "overwhelmed": -2.5,
    "stressed": -2.2,
    "hopeless": -3.0,
    "confused": -1.5,
    "struggling": -2.0,
    "failing": -2.8,
    "exhausted": -2.0,
    "burned out": -2.8,
    "lost": -1.8,
    "demotivated": -2.5,
    "anxious": -2.2,
    "worried": -1.8,
    "scared": -2.0,
    "hate": -2.5,
    "difficult": -1.2,
    "impossible": -2.5,
    "can't": -1.5,
    "cannot": -1.5,
    "never": -1.5,
    "useless": -2.8,
    # Positive: achievement signals
    "aced": 3.0,
    "proud": 2.5,
    "understood": 2.0,
    "excited": 2.2,
    "motivated": 2.5,
    "learned": 1.8,
    "mastered": 2.8,
    "accomplished": 2.5,
    "inspired": 2.2,
    "confident": 2.0,
    "great": 2.0,
    "amazing": 2.5,
    "productive": 2.0,
}

# Apply custom lexicon to the analyzer
sia = SentimentIntensityAnalyzer()
sia.lexicon.update(ACADEMIC_LEXICON)


def analyze_text(text: str) -> dict:
    """
    Analyzes the sentiment of the given text.

    VADER compound score ranges:
      >= 0.05  → positive
      <= -0.05 → negative
      else     → neutral

    We use a stricter negative threshold (-0.1) to avoid
    flagging mildly uncertain notes as negative.
    """
    # Strip HTML tags (CKEditor wraps content in <p> tags)
    import re
    clean_text = re.sub(r"<[^>]+>", " ", text).strip()

    scores = sia.polarity_scores(clean_text)
    compound = scores["compound"]

    if compound >= 0.05:
        sentiment = "positive"
        phrase = None
    elif compound <= -0.10:
        sentiment = "negative"
        phrase = random.choice(MOTIVATIONAL_PHRASES)
    else:
        sentiment = "neutral"
        phrase = None

    return {
        "sentiment": sentiment,
        "score": round(compound, 4),
        "motivational_phrase": phrase,
        "details": {
            "positive": scores["pos"],
            "negative": scores["neg"],
            "neutral": scores["neu"],
        },
    }


@app.route("/analyze", methods=["POST"])
def analyze():
    """
    POST /analyze
    Body: { "text": "Note content here" }
    Returns: { "sentiment": "...", "score": 0.0, "motivational_phrase": "..." }
    """
    data = request.get_json(silent=True)

    if not data or "text" not in data:
        return jsonify({"error": "Missing 'text' field in request body"}), 400

    text = str(data["text"]).strip()
    if not text:
        return jsonify({"error": "Text cannot be empty"}), 400

    result = analyze_text(text)
    return jsonify(result), 200


@app.route("/health", methods=["GET"])
def health():
    """Health check endpoint for monitoring."""
    return jsonify({"status": "ok", "service": "edulink-sentiment-api"}), 200


if __name__ == "__main__":
    print("🧠 EduLink Sentiment API starting on port 5001...")
    print("   Endpoint: POST http://localhost:5001/analyze")
    print("   Health:   GET  http://localhost:5001/health")
    app.run(host="0.0.0.0", port=5001, debug=False)
