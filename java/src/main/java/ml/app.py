from flask import Flask, request, jsonify
import joblib
import pandas as pd
import os

app = Flask(__name__)

# Note: We need to import the custom functions used in the pipeline
# so joblib can unpickle the model properly.
from train import extract_date_features, combine_text

model_path = 'event_model.pkl'
model = None

if os.path.exists(model_path):
    model = joblib.load(model_path)
else:
    print(f"Warning: {model_path} not found. Predictions will fail until the model is trained.")

@app.route('/predict', methods=['POST'])
def predict():
    global model
    if model is None:
        # Try to reload in case it was trained after starting
        if os.path.exists(model_path):
            model = joblib.load(model_path)
        else:
            return jsonify({'error': 'Model not trained yet. Run train.py first.'}), 500

    data = request.json
    if not data:
        return jsonify({'error': 'No JSON payload provided'}), 400

    try:
        df = pd.DataFrame([data])
        required_cols = ['title', 'description', 'dateStart', 'maxCapacity']
        for col in required_cols:
            if col not in df.columns:
                return jsonify({'error': f'Missing column {col}'}), 400

        prediction = model.predict(df)[0]
        return jsonify({'predicted_target': int(prediction)})
    except Exception as e:
        return jsonify({'error': str(e)}), 500

if __name__ == '__main__':
    app.run(port=5000, debug=True)
