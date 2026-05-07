"""
EduLink AI Models - Comprehensive Test Suite
Tests all endpoints on port 5000 (api.py) and port 5001 (toxicity_api/app.py).

Run:
    python test_all_models.py
"""
import sys
import io
import json
import urllib.request
import urllib.error

# Force UTF-8 output on Windows so the script never crashes on encoding
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")

BASE_5000 = "http://localhost:5000"
BASE_5001 = "http://localhost:5001"

results = {"passed": 0, "failed": 0, "skipped": 0}


# ── helpers ──────────────────────────────────────────────────────────────────

def post(url, payload, timeout=6):
    data = json.dumps(payload).encode("utf-8")
    req = urllib.request.Request(
        url, data=data, headers={"Content-Type": "application/json"}
    )
    try:
        with urllib.request.urlopen(req, timeout=timeout) as r:
            return json.loads(r.read().decode())
    except urllib.error.URLError:
        return None
    except Exception as e:
        return {"__error__": str(e)}


def get(url, timeout=4):
    try:
        with urllib.request.urlopen(url, timeout=timeout) as r:
            return json.loads(r.read().decode())
    except Exception:
        return None


def check(label, condition, actual=None):
    if condition:
        results["passed"] += 1
        tag = "[PASS]"
    else:
        results["failed"] += 1
        tag = "[FAIL]"
    detail = f"  => {actual}" if actual is not None else ""
    print(f"  {tag} {label}{detail}")


def skip(label, reason="API not running"):
    results["skipped"] += 1
    print(f"  [SKIP] {label}  ({reason})")


def section(title):
    print(f"\n{'='*60}")
    print(f"  {title}")
    print(f"{'='*60}")


# ═══════════════════════════════════════════════════════════════
# HEALTH CHECKS
# ═══════════════════════════════════════════════════════════════
section("HEALTH CHECKS")

health5000 = get(f"{BASE_5000}/health")
health5001 = get(f"{BASE_5001}/health")

port5000_up = health5000 is not None
port5001_up = health5001 is not None

check("Port 5000 /health reachable", port5000_up, health5000)
check("Port 5001 /health reachable", port5001_up, health5001)
if port5000_up:
    check("Port 5000 toxicity model loaded",
          health5000.get("model_loaded") is True, health5000)


# ═══════════════════════════════════════════════════════════════
# 1. CLASSIFICATION  (port 5000 /classify)
# ═══════════════════════════════════════════════════════════════
section("1. CLASSIFICATION  (port 5000 /classify)")

CLASSIFY_CASES = [
    ("Help with calculus integral",
     "I need step by step help computing the definite integral of a polynomial. "
     "Derivative rules and integration by parts are confusing me.",
     "Mathematics"),
    ("Python recursion bug",
     "My recursive function to traverse a binary tree causes a stack overflow. "
     "I need help debugging the base case in my algorithm code.",
     "Programming"),
    ("Newton laws of motion",
     "Can someone explain Newton's three laws with examples? "
     "Especially the relationship between force mass and acceleration.",
     "Physics"),
    ("Organic chemistry reactions",
     "I need help understanding nucleophilic substitution SN1 SN2 mechanisms. "
     "Alkyl halides and reaction conditions are confusing.",
     "Chemistry"),
    ("Cell biology DNA replication",
     "How does DNA replication work in eukaryotic cells? "
     "DNA polymerase and the lagging strand are confusing me.",
     "Biology"),
    ("Machine learning overfitting fix",
     "My neural network has high training accuracy but low validation accuracy. "
     "How do I fix overfitting with regularization cross-validation sklearn?",
     "Data Science"),
    ("French grammar conjugation",
     "I need help with French verb conjugation in the subjunctive mood. "
     "Irregular verbs like etre and avoir and their tenses are confusing.",
     "Language"),
    ("World War II causes",
     "Can someone explain the main causes of World War II? "
     "I need to understand the Treaty of Versailles and the rise of fascism.",
     "History"),
]

if not port5000_up:
    for t, _, exp in CLASSIFY_CASES:
        skip(f"classify '{t[:40]}' => {exp}")
else:
    for title, desc, expected in CLASSIFY_CASES:
        resp = post(f"{BASE_5000}/classify", {"title": title, "description": desc})
        if resp is None:
            skip(f"classify '{title[:40]}' => {expected}")
            continue
        got_cat  = resp.get("category", "?")
        got_diff = resp.get("difficulty", "?")
        got_conf = resp.get("confidence", 0)
        correct  = got_cat.lower() == expected.lower()
        check(
            f"'{title[:45]}' => {expected}",
            correct,
            f"got='{got_cat}' diff={got_diff} conf={got_conf:.3f}"
        )

    # edge cases
    resp = post(f"{BASE_5000}/classify", {"title": "", "description": ""})
    check("Empty input => category='General'",
          resp is not None and resp.get("category") == "General", resp)

    resp = post(f"{BASE_5000}/classify",
                {"title": "I need help", "description": "please help me study"})
    check("Generic 'help me' => returns a category (no crash)",
          resp is not None and "category" in resp, resp)


# ═══════════════════════════════════════════════════════════════
# 2. TOXICITY DETECTION  (port 5000 /predict)
# ═══════════════════════════════════════════════════════════════
section("2. TOXICITY DETECTION  (port 5000 /predict)")

TOXIC_CASES = [
    ("You are stupid and useless, I hate you!",     True,  "insult => toxic"),
    ("I will kill you if you do not help me",       True,  "threat => toxic"),
    ("Can you help me understand calculus?",        False, "polite request => clean"),
    ("Thanks for the explanation it really helps!", False, "positive feedback => clean"),
    ("This idiot does not know anything at all",    True,  "personal attack => toxic"),
    ("The weather is nice today how are you",       False, "neutral text => clean"),
    ("You are a complete moron and fool",            True,  "profanity => toxic"),
    ("Great explanation I finally understand!",     False, "positive => clean"),
]

if not port5000_up:
    for _, _, label in TOXIC_CASES:
        skip(f"toxicity: {label}")
else:
    for text, expected_toxic, label in TOXIC_CASES:
        resp = post(f"{BASE_5000}/predict", {"text": text})
        if resp is None:
            skip(f"toxicity: {label}")
            continue
        is_toxic   = resp.get("is_toxic", None)
        confidence = resp.get("confidence", 0)
        check(
            label,
            is_toxic == expected_toxic,
            f"is_toxic={is_toxic} confidence={confidence:.3f}"
        )

    # edge cases
    resp = post(f"{BASE_5000}/predict", {"text": ""})
    check("Empty text => is_toxic=False (no crash)",
          resp is not None and resp.get("is_toxic") is False, resp)

    resp = post(f"{BASE_5000}/predict", {})
    check("Missing text field => error returned (no crash)",
          resp is not None, resp)


# ═══════════════════════════════════════════════════════════════
# 3. SUGGESTED REPLIES  (port 5000 /suggest-replies)
# ═══════════════════════════════════════════════════════════════
section("3. SUGGESTED REPLIES  (port 5000 /suggest-replies)")

SUGGEST_CASES = [
    {
        "label": "Student stuck on math",
        "messages": [
            "Student: I do not understand how to solve this integral",
            "Tutor: Which part is confusing you?",
            "Student: I do not know how to start, I am really lost",
        ],
    },
    {
        "label": "Student confirms understanding",
        "messages": [
            "Tutor: Does that make sense now?",
            "Student: Yes that was a great explanation, I finally get it!",
        ],
    },
    {
        "label": "Debug Python error",
        "messages": [
            "Student: I am getting a TypeError in my Python code",
            "Tutor: Can you share the error message?",
            "Student: TypeError unsupported operand type for int and str",
        ],
    },
]

if not port5000_up:
    for case in SUGGEST_CASES:
        skip(f"suggest-replies: {case['label']}")
else:
    for case in SUGGEST_CASES:
        resp = post(f"{BASE_5000}/suggest-replies",
                    {"messages": case["messages"]}, timeout=12)
        if resp is None:
            skip(f"suggest-replies: {case['label']}")
            continue
        suggestions = resp.get("suggestions", [])
        method      = resp.get("method", "?")
        check(
            f"{case['label']} => 3 suggestions",
            len(suggestions) >= 3,
            f"count={len(suggestions)} method={method}"
        )
        print(f"    [INFO] Suggestions:")
        for i, s in enumerate(suggestions[:3], 1):
            print(f"      {i}. {s}")

    resp = post(f"{BASE_5000}/suggest-replies", {"messages": []})
    check("Empty messages => fallback (no crash)",
          resp is not None and "suggestions" in resp, resp)


# ═══════════════════════════════════════════════════════════════
# 4. SENTIMENT ANALYSIS  (port 5000 /sentiment)
# ═══════════════════════════════════════════════════════════════
section("4. SENTIMENT ANALYSIS  (port 5000 /sentiment)")

SENTIMENT_CASES = [
    ("I love this explanation it is absolutely perfect!", "positive"),
    ("I hate this it is terrible and completely useless", "negative"),
    ("The integral equals five",                          "neutral"),
    ("This is amazing I finally understand the concept!", "positive"),
    ("This is terrible I do not understand it at all",    "negative"),
]

if not port5000_up:
    for text, expected in SENTIMENT_CASES:
        skip(f"sentiment: '{text[:40]}' => {expected}")
else:
    for text, expected in SENTIMENT_CASES:
        resp = post(f"{BASE_5000}/sentiment", {"text": text})
        if resp is None:
            skip(f"sentiment: '{text[:40]}' => {expected}")
            continue
        label    = resp.get("label", "?")
        polarity = resp.get("polarity", 0)
        check(f"'{text[:42]}' => {expected}",
              label == expected, f"got='{label}' polarity={polarity}")


# ═══════════════════════════════════════════════════════════════
# 5. LANGUAGE DETECTION  (port 5000 /detect-language)
# ═══════════════════════════════════════════════════════════════
section("5. LANGUAGE DETECTION  (port 5000 /detect-language)")

LANG_CASES = [
    ("Help me understand this calculus problem please", "en"),
    ("Je ne comprends pas ce probleme de mathematiques", "fr"),
    ("Necesito ayuda con algebra y matematicas avanzadas", "es"),
]

if not port5000_up:
    for text, lang in LANG_CASES:
        skip(f"detect-language: '{text[:35]}' => {lang}")
else:
    for text, expected_lang in LANG_CASES:
        resp = post(f"{BASE_5000}/detect-language", {"text": text})
        if resp is None:
            skip(f"detect-language: '{text[:35]}' => {expected_lang}")
            continue
        lang_code = resp.get("language_code", "?")
        check(f"'{text[:42]}' => {expected_lang}",
              lang_code == expected_lang, f"got='{lang_code}'")


# ═══════════════════════════════════════════════════════════════
# 6. SMART TUTOR MATCH  (port 5000 /smart-match)
# ═══════════════════════════════════════════════════════════════
section("6. SMART TUTOR MATCH  (port 5000 /smart-match)")

if not port5000_up:
    skip("smart-match tutor ranking")
else:
    resp = post(f"{BASE_5000}/smart-match", {
        "request_text": "I need help with calculus integration and differential equations math",
        "tutors": [
            {"id": 1, "history": "helped with calculus integrals derivatives differential equations math"},
            {"id": 2, "history": "helped with Java programming OOP design patterns algorithms"},
            {"id": 3, "history": "helped with chemistry reactions organic molecules acid base"},
        ]
    })
    if resp:
        scores  = resp.get("scores", [])
        top_id  = scores[0]["id"] if scores else None
        top_sim = scores[0]["similarity"] if scores else 0
        check("Math request => math tutor (id=1) ranked #1",
              top_id == 1, f"top_id={top_id} similarity={top_sim}")
        print(f"    [INFO] All scores: {scores}")
    else:
        skip("smart-match (no response)")


# ═══════════════════════════════════════════════════════════════
# 7. VADER ACADEMIC SENTIMENT  (port 5001 /analyze)
# ═══════════════════════════════════════════════════════════════
section("7. VADER ACADEMIC SENTIMENT  (port 5001 /analyze)")

VADER_CASES = [
    ("I am overwhelmed stressed about exams I feel hopeless and failing", "negative"),
    ("I aced the test I feel proud motivated and accomplished",           "positive"),
    ("The assignment is due tomorrow at midnight",                        "neutral"),
]

if not port5001_up:
    for text, expected in VADER_CASES:
        skip(f"VADER: '{text[:50]}' => {expected}")
else:
    for text, expected in VADER_CASES:
        resp = post(f"{BASE_5001}/analyze", {"text": text})
        if resp is None:
            skip(f"VADER: '{text[:50]}' => {expected}")
            continue
        sentiment = resp.get("sentiment", "?")
        score     = resp.get("score", 0)
        check(f"'{text[:50]}' => {expected}",
              sentiment == expected, f"got='{sentiment}' score={score}")


# ═══════════════════════════════════════════════════════════════
# 8. INTEGRATION: simulating all 3 Java service calls together
# ═══════════════════════════════════════════════════════════════
section("8. JAVA SERVICE INTEGRATION SIMULATION")

if not port5000_up:
    skip("Integration tests (port 5000 not running)")
else:
    # --- AiClassificationService.classify() ---
    print("\n  [AiClassificationService] classify(title, description):")
    cases = [
        ("Help me solve a math integral problem",
         "I need to compute the definite integral from 0 to pi of sin(x) dx step by step",
         "Mathematics"),
        ("Python list comprehension help",
         "I do not understand how to write a list comprehension with filter condition in Python",
         "Programming"),
        ("DNA replication process",
         "Can you explain how DNA replication works and the role of DNA polymerase",
         "Biology"),
    ]
    for title, desc, expected in cases:
        resp = post(f"{BASE_5000}/classify", {"title": title, "description": desc})
        cat  = resp.get("category") if resp else "?"
        diff = resp.get("difficulty") if resp else "?"
        conf = resp.get("confidence", 0) if resp else 0
        check(
            f"classify('{title[:40]}') => {expected}",
            resp is not None and cat.lower() == expected.lower(),
            f"category={cat} difficulty={diff} confidence={conf:.3f}"
        )

    # --- ToxicityService.analyze() ---
    print("\n  [ToxicityService] analyze(text):")
    check("Insulting message => is_toxic=True",
          post(f"{BASE_5000}/predict", {"text": "You are the worst tutor ever I hate you"}).get("is_toxic") is True)
    check("Positive message  => is_toxic=False",
          post(f"{BASE_5000}/predict", {"text": "Thank you so much for your help today!"}).get("is_toxic") is False)
    check("Threat message    => is_toxic=True",
          post(f"{BASE_5000}/predict", {"text": "I will report you and destroy your reputation"}).get("is_toxic") is True)

    # --- SuggestedRepliesService.suggest() ---
    print("\n  [SuggestedRepliesService] suggest(messages):")
    resp = post(f"{BASE_5000}/suggest-replies", {
        "messages": [
            "Student: I am really struggling with this Python recursion problem",
            "Tutor: What error are you getting?",
            "Student: I keep getting an AttributeError on the recursive call",
        ]
    }, timeout=12)
    sugs = resp.get("suggestions", []) if resp else []
    method = resp.get("method", "?") if resp else "?"
    check(f"Chat context => 3 suggestions returned (method={method})",
          len(sugs) >= 3, f"count={len(sugs)}")
    for i, s in enumerate(sugs[:3], 1):
        print(f"      {i}. {s}")


# ═══════════════════════════════════════════════════════════════
# SUMMARY
# ═══════════════════════════════════════════════════════════════
print(f"\n{'='*60}")
total = results["passed"] + results["failed"]
acc   = (results["passed"] / total * 100) if total > 0 else 0
print(f"  RESULTS:  {results['passed']} passed  |  "
      f"{results['failed']} failed  |  {results['skipped']} skipped")
print(f"  Accuracy: {acc:.1f}%  ({results['passed']}/{total} tests passed)")

if not port5000_up:
    print("\n  [!] Port 5000 DOWN - start it first:")
    print("      cd ai/helprequest_models/suggestion_api/ml && python api.py")
if not port5001_up:
    print("\n  [!] Port 5001 DOWN - start it first:")
    print("      cd ai/helprequest_models/toxicity_api && python app.py")
print(f"{'='*60}\n")

sys.exit(0 if results["failed"] == 0 else 1)
