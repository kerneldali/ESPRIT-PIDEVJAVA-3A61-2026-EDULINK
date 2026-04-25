from flask import Flask, request, jsonify
import speech_recognition as sr
from model import model
import os
import sys
import google.generativeai as genai
import json
import re
import time

app = Flask(__name__)

# Configure Gemini
API_KEY = "AIzaSyDJpSdnVdsPzhZENBG8t5Thkixaxj8fY20"
genai.configure(api_key=API_KEY)
gemini_model = genai.GenerativeModel('models/gemini-2.0-flash')

# Diagnostic logging
LOG_PATH = os.path.join(os.environ.get('TEMP', 'C:\\Users\\deliz\\AppData\\Local\\Temp'), 'STT_LOG.txt')

def log_msg(msg):
    try:
        with open(LOG_PATH, 'a', encoding='utf-8') as f:
            t = time.strftime('%H:%M:%S')
            f.write(f"[{t}] {msg}\n")
            f.flush()
        print(f"[{t}] {msg}")
    except Exception as e:
        print(f"Logging error: {e}")

@app.route('/predict', methods=['POST'])
def predict():
    data = request.json
    text = data.get('text', '')
    sentiment = model.predict(text)
    return jsonify({"sentiment": sentiment})

@app.route('/record', methods=['GET'])
def record():
    recognizer = sr.Recognizer()
    recognizer.energy_threshold = 100 
    recognizer.dynamic_energy_threshold = False
    
    log_msg("--- [STT] Recording Session (with Fallback) ---")
    try:
        mic_list = sr.Microphone.list_microphone_names()
        device_index = None
        for i, name in enumerate(mic_list):
            if "Microphone Array" in name or ("Microphone" in name and "Realtek" in name):
                device_index = i
                break
        
        with sr.Microphone(device_index=device_index) as source:
            log_msg("[STT] Calibrating...")
            recognizer.adjust_for_ambient_noise(source, duration=0.5)
            log_msg("[STT] Listening...")
            audio = recognizer.listen(source, timeout=8, phrase_time_limit=12)
            
            temp_wav = os.path.join(os.environ.get('TEMP', 'C:\\Users\\deliz\\AppData\\Local\\Temp'), 'temp_voice_trans.wav')
            with open(temp_wav, "wb") as f:
                f.write(audio.get_wav_data())
            
            # --- Try Gemini First ---
            try:
                log_msg("[STT] Attempting Gemini 2.0...")
                audio_file = genai.upload_file(path=temp_wav)
                response = gemini_model.generate_content(["Transcribe accurately. ONLY text.", audio_file])
                transcription = response.text.strip()
                audio_file.delete()
                
                if transcription:
                    log_msg(f"[STT] Gemini Success: {transcription}")
                    return jsonify({"text": transcription})
            except Exception as ge:
                log_msg(f"[STT] Gemini Error (Quota?): {str(ge)}")
            
            # --- Fallback to Google Standard STT ---
            log_msg("[STT] Falling back to standard Google STT API...")
            try:
                transcription = recognizer.recognize_google(audio)
                log_msg(f"[STT] Google STT Success: {transcription}")
                return jsonify({"text": transcription})
            except sr.UnknownValueError:
                 return jsonify({"error": "No speech detected. Please speak louder."}), 408
            except Exception as e:
                 return jsonify({"error": f"Quota Exceeded. Please wait 1 minute. ({str(e)})"}), 429
            
    except sr.WaitTimeoutError:
        return jsonify({"error": "No speech detected. Please try again."}), 408
    except Exception as e:
        return jsonify({"error": f"System Error: {str(e)}"}), 500

@app.route('/summarize', methods=['POST'])
def summarize():
    data = request.json
    notes = data.get('notes', [])
    if not notes:
        return json.dumps({"summary": "No notes found", "category": "General"})
    
    combined_text = "\n".join([f"Title: {n['title']}\nContent: {n['content']}" for n in notes])
    
    try:
        log_msg("[Gemini] Generating summary...")
        response = gemini_model.generate_content(
            f"Summarize these notes concisely as JSON with 'summary' and 'category'.\n\nNotes:\n{combined_text}"
        )
        text_response = response.text.strip()
        match = re.search(r'\{.*\}', text_response, re.DOTALL)
        return match.group() if match else text_response
    except Exception as e:
        log_msg(f"[Gemini] Summary Error: {str(e)}")
        # Simple local summary fallback
        return json.dumps({
            "summary": "AI Quota limited. Showing first note as summary: " + (notes[0]['content'] if notes else ""),
            "category": "General"
        })

if __name__ == "__main__":
    log_msg("[Flask] AI Backend Online on Port 5001 (with Quota Resilience)")
    app.run(port=5001)
