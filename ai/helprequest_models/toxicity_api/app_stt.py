from flask import Flask, request, jsonify
import os
import requests

app = Flask(__name__)

GROQ_API_KEY = "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"
GROQ_STT_URL = "https://api.groq.com/openai/v1/audio/transcriptions"

@app.route('/transcribe', methods=['POST'])
def transcribe():
    if 'file' not in request.files:
        return jsonify({"error": "No file uploaded"}), 400

    audio_file = request.files['file']
    temp_wav = os.path.join(os.environ.get('TEMP', 'C:\\Users\\deliz\\AppData\\Local\\Temp'), 'stt_check.wav')
    audio_file.save(temp_wav)
    print(f"STT: Received file of size {os.path.getsize(temp_wav)} bytes")

    try:
        with open(temp_wav, 'rb') as f:
            response = requests.post(
                GROQ_STT_URL,
                headers={"Authorization": f"Bearer {GROQ_API_KEY}"},
                files={"file": ("audio.wav", f, "audio/wav")},
                data={"model": "whisper-large-v3-turbo", "response_format": "json", "language": "en"}
            )
        
        print(f"Groq STT Response: {response.status_code} - {response.text[:200]}")
        
        if response.status_code == 200:
            result = response.json()
            text = result.get("text", "").strip()
            return jsonify({"text": text})
        else:
            return jsonify({"error": f"Groq STT Error: {response.text}"}), response.status_code

    except Exception as e:
        import traceback
        traceback.print_exc()
        return jsonify({"error": str(e)}), 500

if __name__ == "__main__":
    print("STT Service (Groq Whisper) online on Port 5003")
    app.run(port=5003)
