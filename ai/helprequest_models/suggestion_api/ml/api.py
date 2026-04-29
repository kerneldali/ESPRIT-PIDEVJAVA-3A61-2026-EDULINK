"""
EduLink AI API - Local ML microservice
Endpoints:
  POST /predict          -> Toxicity detection (TF-IDF + LogReg)
  POST /detect-language  -> Language detection (langdetect)
  POST /sentiment        -> Sentiment analysis (TextBlob)
  POST /smart-match      -> TF-IDF cosine similarity for tutor matching
  GET  /health           -> Status check
"""
import os
import re
import joblib
import numpy as np
from flask import Flask, request, jsonify
from flask_cors import CORS
from langdetect import detect, LangDetectException
from textblob import TextBlob
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity
from sentence_transformers import SentenceTransformer, util as st_util

app = Flask(__name__)
CORS(app)

MODEL_PATH = os.path.join(os.path.dirname(__file__), 'model', 'toxicity_model.pkl')
model = None

LANGUAGE_NAMES = {
    'en': 'English', 'fr': 'French', 'ar': 'Arabic',
    'de': 'German',  'es': 'Spanish', 'it': 'Italian',
    'pt': 'Portuguese', 'nl': 'Dutch', 'ru': 'Russian',
    'zh': 'Chinese', 'ja': 'Japanese', 'ko': 'Korean',
    'tr': 'Turkish', 'pl': 'Polish', 'sv': 'Swedish',
}


def load_model():
    global model
    if not os.path.exists(MODEL_PATH):
        raise FileNotFoundError(f"Model not found at {MODEL_PATH}. Run train_model.py first.")
    model = joblib.load(MODEL_PATH)
    print(f"[OK] Toxicity model loaded from {MODEL_PATH}")


def clean_text(text):
    if not isinstance(text, str):
        return ''
    text = re.sub(r'<[^>]+>', ' ', text)
    text = re.sub(r'https?://\S+', ' ', text)
    text = re.sub(r'[^a-zA-Z\s]', ' ', text)
    text = re.sub(r'\s+', ' ', text).strip()
    return text.lower()


# ─── ENDPOINT 1: Toxicity Detection ─────────────────────────────────────────

@app.route('/predict', methods=['POST'])
def predict():
    data = request.get_json(silent=True)
    if not data or 'text' not in data:
        return jsonify({'error': 'Missing "text" field'}), 400

    text = data['text']
    if not text or not text.strip():
        return jsonify({'is_toxic': False, 'confidence': 1.0})

    cleaned = clean_text(text)
    if not cleaned.strip():
        return jsonify({'is_toxic': False, 'confidence': 1.0})

    prediction = model.predict([cleaned])[0]
    probabilities = model.predict_proba([cleaned])[0]
    confidence = float(max(probabilities))

    return jsonify({
        'is_toxic': bool(prediction),
        'confidence': round(confidence, 4)
    })


# ─── ENDPOINT 2: Language Detection ─────────────────────────────────────────

@app.route('/detect-language', methods=['POST'])
def detect_language():
    data = request.get_json(silent=True)
    if not data or 'text' not in data:
        return jsonify({'error': 'Missing "text" field'}), 400

    text = data['text'].strip()
    if len(text) < 3:
        return jsonify({'language_code': 'unknown', 'language_name': 'Unknown', 'confidence': 0.0})

    try:
        lang_code = detect(text)
        lang_name = LANGUAGE_NAMES.get(lang_code, lang_code.upper())
        return jsonify({
            'language_code': lang_code,
            'language_name': lang_name,
        })
    except LangDetectException:
        return jsonify({'language_code': 'unknown', 'language_name': 'Unknown'})


# ─── ENDPOINT 3: Sentiment Analysis ─────────────────────────────────────────

@app.route('/sentiment', methods=['POST'])
def sentiment():
    data = request.get_json(silent=True)
    if not data or 'text' not in data:
        return jsonify({'error': 'Missing "text" field'}), 400

    text = data['text'].strip()
    if not text:
        return jsonify({'label': 'neutral', 'polarity': 0.0, 'subjectivity': 0.0})

    blob = TextBlob(text)
    polarity = round(blob.sentiment.polarity, 4)       # -1.0 to +1.0
    subjectivity = round(blob.sentiment.subjectivity, 4)  # 0 = objective, 1 = subjective

    if polarity > 0.1:
        label = 'positive'
    elif polarity < -0.1:
        label = 'negative'
    else:
        label = 'neutral'

    return jsonify({
        'label': label,
        'polarity': polarity,
        'subjectivity': subjectivity,
    })


# ─── ENDPOINT 4: TF-IDF Smart Matching ──────────────────────────────────────

@app.route('/smart-match', methods=['POST'])
def smart_match():
    """
    Computes TF-IDF cosine similarity between a help request and tutor histories.

    Input JSON:
    {
        "request_text": "I need help with Python recursion and dynamic programming",
        "tutors": [
            {"id": 1, "history": "helped with Python loops and functions recursion"},
            {"id": 2, "history": "helped with Java OOP design patterns"},
            {"id": 3, "history": "helped with algorithms sorting dynamic programming"}
        ]
    }

    Output JSON:
    {
        "scores": [
            {"id": 1, "similarity": 0.72},
            {"id": 2, "similarity": 0.15},
            {"id": 3, "similarity": 0.58}
        ]
    }
    """
    data = request.get_json(silent=True)
    if not data:
        return jsonify({'error': 'Missing JSON body'}), 400

    request_text = data.get('request_text', '').strip()
    tutors = data.get('tutors', [])

    if not request_text:
        return jsonify({'error': 'Missing "request_text" field'}), 400

    if not tutors or not isinstance(tutors, list):
        return jsonify({'scores': []})

    # Collect all texts: request first, then tutor histories
    tutor_histories = []
    tutor_ids = []
    for t in tutors:
        tid = t.get('id')
        history = t.get('history', '').strip()
        if tid is not None and history:
            tutor_ids.append(tid)
            tutor_histories.append(history)

    if not tutor_histories:
        return jsonify({'scores': []})

    # Build TF-IDF matrix: [request_text, tutor1_history, tutor2_history, ...]
    all_texts = [request_text] + tutor_histories

    try:
        vectorizer = TfidfVectorizer(
            stop_words='english',
            max_features=5000,
            ngram_range=(1, 2),   # unigrams + bigrams for better matching
            min_df=1,
        )
        tfidf_matrix = vectorizer.fit_transform(all_texts)

        # Cosine similarity between request (index 0) and each tutor
        request_vec = tfidf_matrix[0:1]
        tutor_vecs = tfidf_matrix[1:]
        similarities = cosine_similarity(request_vec, tutor_vecs)[0]

        scores = []
        for i, tid in enumerate(tutor_ids):
            scores.append({
                'id': tid,
                'similarity': round(float(similarities[i]), 4)
            })

        # Sort by similarity descending
        scores.sort(key=lambda x: x['similarity'], reverse=True)

        return jsonify({'scores': scores})

    except Exception as e:
        return jsonify({'error': f'TF-IDF computation failed: {str(e)}'}), 500


# ─── ENDPOINT 5: Local Help Request Classification ──────────────────────────

# Category keyword corpus — keys MUST match Java's category names exactly
CATEGORY_CORPUS = {
    'Mathematics': (
        'math maths mathematics algebra calculus geometry trigonometry equation integral derivative matrix '
        'probability statistics linear quadratic polynomial fraction decimal percentage arithmetic '
        'logarithm differential vector determinant eigenvalue limit series convergence permutation '
        'combination number theory proof theorem lemma axiom set function domain range interval '
        'exponent root radical binomial factorial coordinate plane graph parabola hyperbola ellipse '
        'sine cosine tangent radian complex number imaginary unit modulus argument polar form '
        'fourier laplace transform differential equation ordinary partial boundary value'
    ),
    'Programming': (
        'programming code algorithm data structure python java javascript typescript html css '
        'database sql api software development function variable loop array object class inheritance '
        'recursion sorting searching binary tree graph linked list stack queue hash map '
        'web application backend frontend framework django flask spring react angular vue '
        'git version control debugging error exception null pointer segfault compile runtime '
        'pointer memory heap stack overflow concurrency thread asynchronous async await '
        'machine learning neural network deep learning tensorflow pytorch keras '
        'operating system process scheduler file system network socket protocol '
        'object oriented functional programming design pattern singleton factory observer'
    ),
    'Physics': (
        'physics force energy momentum velocity acceleration gravity wave optics thermodynamics '
        'electricity magnetism quantum mechanics relativity circuit voltage resistance capacitor '
        'inductor electromagnetic field potential kinetic gravitational nuclear fission fusion '
        'particle photon electron proton neutron boson fermion entropy enthalpy pressure volume '
        'fluid dynamics buoyancy torque angular momentum moment of inertia centripetal '
        'doppler effect interference diffraction polarization refraction reflection '
        'coulomb faraday maxwell newton einstein planck heisenberg schrodinger'
    ),
    'Chemistry': (
        'chemistry atom molecule element reaction bond compound acid base organic inorganic '
        'periodic table electron proton neutron mole stoichiometry chemical equation oxidation '
        'reduction redox valence shell orbital hybridization isomer polymer catalyst equilibrium '
        'pH concentration molarity solution solubility precipitation titration electrolysis '
        'thermochemistry enthalpy entropy gibbs free energy activation energy kinetics '
        'functional group alkane alkene alkyne benzene ester ether ketone aldehyde alcohol amine'
    ),
    'Biology': (
        'biology cell dna rna genetics evolution organism plant animal ecosystem photosynthesis '
        'mitosis meiosis protein enzyme bacteria virus anatomy physiology ecology '
        'chromosome gene allele mutation natural selection adaptation biodiversity '
        'nervous system brain neuron synapse hormone endocrine immune system antibody '
        'respiration digestion circulation heart blood vessel muscle bone tissue organ '
        'microscope hypothesis experiment scientific method taxonomy kingdom phylum class order family genus species'
    ),
    'Language': (
        'language grammar vocabulary writing essay reading comprehension literature poetry '
        'translation conjugation tense verb noun adjective sentence paragraph composition '
        'spelling pronunciation syntax morphology phonology semantics pragmatics discourse '
        'fiction non-fiction novel short story narrative descriptive persuasive expository '
        'thesis statement argument citation bibliography references citation apa mla '
        'french english arabic spanish german italian portuguese latin greek punctuation'
    ),
    'History': (
        'history war revolution empire civilization ancient medieval modern century dynasty '
        'king queen president government politics treaty independence colony nation state '
        'world war cold war colonialism imperialism renaissance enlightenment reformation '
        'industrial revolution french revolution american revolution roman greek egyptian '
        'ottoman byzantine mongol persian byzantine crusades feudalism monarchy democracy'
    ),
    'Data Science': (
        'data science statistics regression classification clustering machine learning '
        'neural network deep learning dataset feature engineering model training validation '
        'cross validation overfitting underfitting bias variance pandas numpy matplotlib '
        'jupyter notebook r python scipy sklearn tensorflow pytorch visualization '
        'data cleaning preprocessing normalization standardization hypothesis test p-value '
        'confidence interval correlation coefficient principal component analysis pca '
        'natural language processing nlp text mining sentiment analysis word embedding '
        'random forest gradient boosting xgboost support vector machine svm k-means '
        'time series forecasting anomaly detection recommender system'
    ),
    'General': (
        'help question problem understand explain learn study homework assignment project '
        'review practice exercise solve approach method strategy tips guidance advice '
        'concept idea topic subject area field introduction beginner overview summary'
    ),
}

# Difficulty indicators
DIFFICULTY_KEYWORDS = {
    'Hard': [
        'advanced', 'complex', 'difficult', 'prove', 'derive', 'analyze', 'research',
        'thesis', 'optimization', 'abstract', 'theorem', 'graduate', 'university',
        'challenging', 'sophisticated', 'rigorous', 'formal proof', 'derivation',
    ],
    'Easy': [
        'basic', 'simple', 'introduction', 'beginner', 'elementary', 'easy',
        'understand', 'explain', 'what is', 'definition', 'overview', 'fundamentals',
        'getting started', 'first time', 'never learned', 'can you help',
    ],
}

# Subject-specific keyword boosters: if ANY term matches, add a bonus to that category score
KEYWORD_BOOSTERS = {
    'Mathematics': [
        'integral', 'derivative', 'calculus', 'algebra', 'geometry', 'trigonometry',
        'equation', 'matrix', 'determinant', 'eigenvalue', 'vector', 'probability',
        'statistics', 'theorem', 'proof', 'differential', 'polynomial', 'logarithm',
        'arithmetic', 'factorial', 'permutation', 'combination', 'limit', 'series',
        'convergence', 'fourier', 'laplace', 'linear algebra', 'number theory',
        'sin', 'cos', 'tan', 'angle', 'triangle', 'circle', 'parabola', 'hyperbola',
        'math', 'maths', 'mathematical',
    ],
    'Programming': [
        'code', 'coding', 'program', 'algorithm', 'function', 'loop', 'array',
        'class', 'object', 'inheritance', 'recursion', 'pointer', 'compile', 'debug',
        'python', 'java', 'javascript', 'c++', 'html', 'css', 'sql', 'api',
        'framework', 'library', 'github', 'git', 'bug', 'error', 'exception',
        'stack overflow', 'runtime', 'async', 'thread', 'database', 'backend', 'frontend',
    ],
    'Physics': [
        'physics', 'force', 'energy', 'momentum', 'velocity', 'acceleration', 'gravity',
        'wave', 'optics', 'thermodynamics', 'electricity', 'magnetism', 'quantum',
        'relativity', 'circuit', 'voltage', 'resistance', 'newton', 'einstein', 'photon',
        'kinetic', 'potential', 'friction', 'buoyancy', 'torque', 'electromagnetic',
        'newton law', 'laws of motion', 'mass acceleration',
    ],
    'Chemistry': [
        'chemistry', 'atom', 'molecule', 'reaction', 'bond', 'compound', 'acid',
        'base', 'organic', 'inorganic', 'periodic', 'mole', 'stoichiometry',
        'oxidation', 'reduction', 'redox', 'catalyst', 'equilibrium', 'titration',
        'alkane', 'alkene', 'benzene', 'ester', 'polymer', 'enthalpy', 'nucleophilic',
        'sn1', 'sn2', 'substitution', 'organic chemistry',
    ],
    'Biology': [
        'biology', 'cell', 'dna', 'rna', 'genetics', 'evolution', 'organism',
        'photosynthesis', 'mitosis', 'meiosis', 'protein', 'enzyme', 'bacteria',
        'virus', 'anatomy', 'physiology', 'ecology', 'chromosome', 'gene', 'mutation',
        'neuron', 'hormone', 'immune', 'respiration', 'digestion', 'replication',
        'eukaryotic', 'prokaryotic', 'ecosystem', 'taxonomy', 'species',
        'dna replication', 'cell division', 'polymerase',
    ],
    'History': [
        'history', 'war', 'revolution', 'empire', 'civilization', 'ancient', 'medieval',
        'century', 'dynasty', 'king', 'queen', 'president', 'government', 'treaty',
        'independence', 'colony', 'fascism', 'versailles', 'napoleon', 'renaissance',
        'enlightenment', 'colonialism', 'feudalism', 'monarchy', 'democracy', 'crusades',
        'world war', 'cold war', 'world war ii', 'historical',
    ],
    'Language': [
        'grammar', 'vocabulary', 'conjugation', 'tense', 'verb', 'noun', 'adjective',
        'sentence', 'paragraph', 'composition', 'spelling', 'pronunciation', 'syntax',
        'literature', 'poetry', 'essay', 'translation', 'subjunctive', 'participle',
        'french verb', 'arabic grammar', 'spanish grammar', 'german grammar',
    ],
    'Data Science': [
        'machine learning', 'neural network', 'deep learning', 'dataset', 'pandas',
        'numpy', 'matplotlib', 'sklearn', 'tensorflow', 'pytorch', 'regression',
        'classification', 'clustering', 'model training', 'overfitting', 'accuracy',
        'data science', 'data analysis', 'visualization', 'feature', 'prediction',
        'nlp', 'sentiment', 'text mining', 'random forest', 'xgboost', 'pca',
    ],
}

# Minimum TF-IDF score a category must already have before a booster is applied.
# Set to 0.0 to allow keyword boosters to fully override TF-IDF for short inputs (like "maths")
BOOSTER_MIN_FLOOR = 0.0
BOOSTER_WEIGHT    = 0.20   # bonus added when a booster keyword matches AND floor is met


@app.route('/classify', methods=['POST'])
def classify():
    """
    Classifies a help request into category + difficulty using TF-IDF + keyword boosting.

    Input:  {"title": "...", "description": "..."}
    Output: {"category": "Mathematics", "difficulty": "MEDIUM", "confidence": 0.82, "source": "LOCAL_ML"}
    """
    data = request.get_json(silent=True)
    if not data:
        return jsonify({'error': 'Missing JSON body'}), 400

    title       = data.get('title', '').strip()
    description = data.get('description', '').strip()
    # Title is weighted 2× — more signal-rich than description
    text = f"{title} {title} {description}".strip()

    if not text:
        return jsonify({'category': 'General', 'difficulty': 'MEDIUM', 'confidence': 0.0, 'source': 'LOCAL_ML'})

    categories   = list(CATEGORY_CORPUS.keys())
    corpus_texts = list(CATEGORY_CORPUS.values())
    all_texts    = [text] + corpus_texts

    try:
        vectorizer = TfidfVectorizer(
            stop_words='english',
            max_features=5000,
            ngram_range=(1, 2),
            min_df=1,
            sublinear_tf=True,
        )
        tfidf_matrix  = vectorizer.fit_transform(all_texts)
        input_vec     = tfidf_matrix[0:1]
        category_vecs = tfidf_matrix[1:]
        similarities  = cosine_similarity(input_vec, category_vecs)[0].tolist()

        # ── Keyword-booster pass ────────────────────────────────────────────
        text_lower = text.lower()
        for cat, keywords in KEYWORD_BOOSTERS.items():
            if cat in categories:
                idx = categories.index(cat)
                # Only boost if TF-IDF already sees some signal for this category
                if similarities[idx] >= BOOSTER_MIN_FLOOR:
                    for kw in keywords:
                        if kw in text_lower:
                            similarities[idx] = min(1.0, similarities[idx] + BOOSTER_WEIGHT)
                            break   # one boost per category is enough

        best_idx   = int(np.argmax(similarities))
        best_score = float(similarities[best_idx])

        if best_score < 0.03:
            category   = 'General'
            confidence = 0.0
        else:
            category   = categories[best_idx]
            confidence = round(best_score, 4)

        # ── Difficulty detection ────────────────────────────────────────────
        difficulty = 'MEDIUM'
        for word in DIFFICULTY_KEYWORDS['Hard']:
            if word in text_lower:
                difficulty = 'HARD'
                break
        if difficulty == 'MEDIUM':
            for word in DIFFICULTY_KEYWORDS['Easy']:
                if word in text_lower:
                    difficulty = 'EASY'
                    break

        return jsonify({
            'category':   category,
            'difficulty': difficulty,
            'confidence': confidence,
            'source':     'LOCAL_ML',
        })

    except Exception as e:
        return jsonify({'error': f'Classification failed: {str(e)}'}), 500


# ─── ENDPOINT 6: Neural Reply Suggestions (SentenceTransformer) ──────────────

# Reply candidate bank — the model will rank these semantically
REPLY_CANDIDATES = [
    # Empathetic / Encouraging
    "Don't worry, this is a tricky topic. Let's work through it together.",
    "Take your time — there's absolutely no rush.",
    "I know this feels hard right now, but you're making real progress.",
    "Everyone struggles with this concept at first. You're not alone.",
    "This is a tough topic, but I believe you can get through it.",
    # Clarifying / Probing
    "Which specific part is causing you the most confusion?",
    "Can you walk me through what you've tried so far?",
    "What's the last step where things still made sense to you?",
    "Let me rephrase this — does this version make more sense?",
    "Can you tell me what you expect the answer to look like?",
    # Teaching / Explaining
    "Let me break this down into smaller, simpler steps.",
    "Here's a different way to think about this concept.",
    "Let me give you a concrete example to illustrate this.",
    "The key idea is actually simpler than it seems. Let me show you.",
    "Let me walk you through the solution step by step.",
    # Positive reinforcement
    "Great job! That's exactly the right approach.",
    "You're really getting the hang of this! Well done.",
    "That's a perfect answer — you clearly understand the concept.",
    "Awesome work! You're making excellent progress.",
    "Nice! That's correct. Ready for the next challenge?",
    # Programming / Code help
    "Can you share the error message you're seeing?",
    "Let's debug this together — what output are you getting?",
    "Try adding a print statement to check the intermediate values.",
    "The issue is likely in the logic flow. Let me trace through it.",
    "Check your variable types — that's a common source of this error.",
    # Math / Science help
    "Let's start by identifying which formula applies here.",
    "Try solving a simpler version of this problem first.",
    "Drawing a diagram might help visualize what's happening.",
    "Remember to check your units throughout the calculation.",
    "Let me show you the general pattern, then you can apply it.",
    # General / Transition
    "I understand. Let me help you with that.",
    "Good question — here's what I'd suggest.",
    "Sure! Let's work on this step by step.",
    "Can you explain a bit more about what you need?",
    "Let me think about the best way to approach this.",
]

# Pre-load sentence transformer model + pre-compute reply embeddings
reply_model = None
reply_embeddings = None


def load_reply_model():
    """Loads the SentenceTransformer model and pre-computes embeddings for all reply candidates."""
    global reply_model, reply_embeddings
    try:
        print("[...] Loading SentenceTransformer model (all-MiniLM-L6-v2)...")
        reply_model = SentenceTransformer('all-MiniLM-L6-v2')
        reply_embeddings = reply_model.encode(REPLY_CANDIDATES, convert_to_tensor=True)
        print(f"[OK] Reply model loaded. {len(REPLY_CANDIDATES)} candidates embedded ({reply_embeddings.shape[1]}d vectors).")
    except Exception as e:
        print(f"[WARN] SentenceTransformer load failed: {e}. Suggest-replies will use TF-IDF fallback.")
        reply_model = None
        reply_embeddings = None


@app.route('/suggest-replies', methods=['POST'])
def suggest_replies():
    """
    Generates 3 contextual reply suggestions using neural sentence embeddings.

    ML Pipeline:
      1. Encode conversation context into a 384-dimensional vector (MiniLM)
      2. Compute cosine similarity against pre-embedded reply candidates
      3. Apply sentiment-based score boosting (TextBlob)
      4. Return top-3 most semantically relevant replies

    Input:  {"messages": ["User: I'm stuck on this problem", "Tutor: Which part?"]}
    Output: {"suggestions": ["Let me break this down...", "Which step...?", "..."], "method": "neural"}
    """
    data = request.get_json(silent=True)
    fallback = REPLY_CANDIDATES[:3]

    if not data:
        return jsonify({'suggestions': fallback, 'method': 'fallback'})

    messages = data.get('messages', [])
    if not messages or not isinstance(messages, list):
        return jsonify({'suggestions': fallback, 'method': 'fallback'})

    # Combine last 5 messages into a context string
    recent = messages[-5:]
    context_text = ' '.join(recent)

    # === PRIMARY: Neural SentenceTransformer Approach ===
    if reply_model is not None and reply_embeddings is not None:
        try:
            # 1. Encode conversation context into embedding space
            context_embedding = reply_model.encode(context_text, convert_to_tensor=True)

            # 2. Compute cosine similarity against all pre-embedded candidates
            similarities = st_util.cos_sim(context_embedding, reply_embeddings)[0]
            scores = similarities.cpu().numpy().astype(float)

            # 3. Sentiment-based boosting
            last_msg = recent[-1]
            if ':' in last_msg:
                last_msg = last_msg.split(':', 1)[1].strip()
            polarity = TextBlob(last_msg).sentiment.polarity

            # Boost empathetic replies for negative sentiment, positive for positive
            for i, reply in enumerate(REPLY_CANDIDATES):
                if polarity < -0.15:
                    # Student is frustrated → boost empathetic + encouraging
                    if any(w in reply.lower() for w in ['worry', 'tough', 'struggle', 'time', 'rush', 'together']):
                        scores[i] += 0.15
                elif polarity > 0.25:
                    # Student is positive → boost reinforcement
                    if any(w in reply.lower() for w in ['great', 'awesome', 'correct', 'nice', 'well done', 'progress']):
                        scores[i] += 0.15

            # 4. Get top-3 unique suggestions
            ranked_indices = np.argsort(scores)[::-1]
            suggestions = []
            for idx in ranked_indices:
                candidate = REPLY_CANDIDATES[idx]
                if candidate not in suggestions:
                    suggestions.append(candidate)
                if len(suggestions) >= 3:
                    break

            return jsonify({
                'suggestions': suggestions,
                'method': 'neural',
                'model': 'all-MiniLM-L6-v2',
                'context_sentiment': round(polarity, 3),
            })

        except Exception as e:
            print(f"[WARN] Neural suggestion failed: {e}, falling back to TF-IDF")

    # === FALLBACK: TF-IDF Approach ===
    try:
        all_texts = [context_text] + REPLY_CANDIDATES
        vectorizer = TfidfVectorizer(stop_words='english', max_features=1000)
        tfidf_matrix = vectorizer.fit_transform(all_texts)
        sims = cosine_similarity(tfidf_matrix[0:1], tfidf_matrix[1:])[0]
        ranked = np.argsort(sims)[::-1]
        suggestions = [REPLY_CANDIDATES[i] for i in ranked[:3]]
        return jsonify({'suggestions': suggestions, 'method': 'tfidf_fallback'})
    except Exception:
        return jsonify({'suggestions': fallback, 'method': 'fallback'})


# ─── HEALTH CHECK ────────────────────────────────────────────────────────────

@app.route('/health', methods=['GET'])
def health():
    return jsonify({
        'status': 'ok',
        'model_loaded': model is not None,
        'endpoints': ['/predict', '/detect-language', '/sentiment', '/smart-match', '/classify', '/suggest-replies']
    })


if __name__ == '__main__':
    load_model()
    load_reply_model()
    print("[OK] EduLink AI API running on http://127.0.0.1:5000")
    print("     Endpoints: /predict  /detect-language  /sentiment  /smart-match  /classify  /suggest-replies  /health")
    app.run(host='0.0.0.0', port=5000, debug=False)

