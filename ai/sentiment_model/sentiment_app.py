from flask import Flask, request, jsonify
import joblib
import os

app = Flask(__name__)

# Load model
model_path = os.path.join(os.path.dirname(__file__), 'sentiment_model.joblib')
if os.path.exists(model_path):
    model = joblib.load(model_path)
else:
    model = None

@app.route('/predict', methods=['POST'])
def predict():
    if model is None:
        return jsonify({"error": "Model not trained"}), 500
    
    data = request.json
    text = data.get('text', '')
    if not text:
        return jsonify({"sentiment": "neutral"})
    
    prediction = model.predict([text])[0]
    return jsonify({"sentiment": prediction})

if __name__ == "__main__":
    print("Sentiment ML Service online on Port 5005")
    app.run(port=5005)
