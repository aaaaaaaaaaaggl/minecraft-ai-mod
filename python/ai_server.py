"""
AI Server для интеграции с Minecraft модом
Запуск: python ai_server.py
"""

from flask import Flask, request, jsonify
from flask_cors import CORS
import numpy as np
import tensorflow as tf
from datetime import datetime
import os
import json

app = Flask(__name__)
CORS(app)

# Конфигурация
AI_MODEL_PATH = "model.h5"
PORT = 5000
HOST = "0.0.0.0"

# Глобальные переменные
model = None
is_model_loaded = False

# ==================== ИНИЦИАЛИЗАЦИЯ ====================

def load_model():
    """Загрузить обученную модель"""
    global model, is_model_loaded
    
    try:
        if os.path.exists(AI_MODEL_PATH):
            model = tf.keras.models.load_model(AI_MODEL_PATH)
            is_model_loaded = True
            print(f"✅ Модель загружена: {AI_MODEL_PATH}")
        else:
            print(f"⚠️  Модель не найдена: {AI_MODEL_PATH}")
            print("   Используется демо-модель...")
            create_dummy_model()
    except Exception as e:
        print(f"❌ Ошибка при загрузке модели: {e}")
        create_dummy_model()

def create_dummy_model():
    """Создать простую демо-модель для тестирования"""
    global model, is_model_loaded
    
    model = tf.keras.Sequential([
        tf.keras.layers.Dense(64, activation='relu', input_shape=(4,)),
        tf.keras.layers.Dense(32, activation='relu'),
        tf.keras.layers.Dense(3, activation='softmax')
    ])
    model.compile(optimizer='adam', loss='categorical_crossentropy')
    is_model_loaded = True
    print("✅ Демо-модель создана")

# ==================== API ENDPOINTS ====================

@app.route('/health', methods=['GET'])
def health():
    """Проверка статуса сервера"""
    return jsonify({
        "status": "online",
        "timestamp": datetime.now().isoformat(),
        "model_loaded": is_model_loaded,
        "version": "1.0.0"
    }), 200

@app.route('/predict', methods=['POST'])
def predict():
    """Получить предсказание AI для действия в игре"""
    try:
        data = request.get_json()
        
        if not data:
            return jsonify({"error": "No JSON data provided"}), 400
        
        required_fields = ["x", "y", "z", "block_type"]
        for field in required_fields:
            if field not in data:
                return jsonify({"error": f"Missing field: {field}"}), 400
        
        x, y, z = float(data["x"]), float(data["y"]), float(data["z"])
        block_type_map = {
            "grass": 0, "stone": 1, "dirt": 2, "water": 3,
            "sand": 4, "wood": 5, "leaves": 6, "other": 7
        }
        block_encoded = block_type_map.get(data["block_type"].lower(), 7)
        
        input_data = np.array([[
            (x % 256) / 256,
            y / 256,
            (z % 256) / 256,
            block_encoded / 8
        ]])
        
        prediction = model.predict(input_data, verbose=0)
        action_idx = np.argmax(prediction[0])
        confidence = float(prediction[0][action_idx])
        
        action_map = {
            0: "build_structure",
            1: "spawn_mob",
            2: "generate_ore"
        }
        structure_types = ["tower", "house", "bridge"]
        mob_types = ["zombie", "skeleton", "creeper"]
        ore_types = ["diamond", "gold", "iron"]
        
        action = action_map[action_idx]
        
        response = {
            "action": action,
            "confidence": confidence,
            "x": x,
            "y": y,
            "z": z,
            "timestamp": datetime.now().isoformat()
        }
        
        if action == "build_structure":
            response["structure_type"] = structure_types[action_idx % len(structure_types)]
            response["height"] = int(5 + confidence * 10)
        elif action == "spawn_mob":
            response["mob_type"] = mob_types[action_idx % len(mob_types)]
            response["count"] = int(1 + confidence * 5)
        elif action == "generate_ore":
            response["ore_type"] = ore_types[action_idx % len(ore_types)]
            response["vein_size"] = int(5 + confidence * 20)
        
        return jsonify(response), 200
        
    except Exception as e:
        print(f"❌ Ошибка в /predict: {e}")
        return jsonify({"error": str(e)}), 500

@app.route('/train', methods=['POST'])
def train_model():
    """Обучить модель на новых данных"""
    try:
        data = request.get_json()
        
        if not data or "x_data" not in data or "y_data" not in data:
            return jsonify({"error": "Missing x_data or y_data"}), 400
        
        x_train = np.array(data["x_data"], dtype=np.float32)
        y_train = np.array(data["y_data"], dtype=np.float32)
        
        if x_train.shape[0] != y_train.shape[0]:
            return jsonify({"error": "x_data and y_data size mismatch"}), 400
        
        if x_train.shape[1] != 4:
            return jsonify({"error": "x_data must have 4 features"}), 400
        
        history = model.fit(
            x_train, y_train,
            epochs=10,
            batch_size=32,
            verbose=0
        )
        
        model.save(AI_MODEL_PATH)
        
        return jsonify({
            "status": "trained",
            "samples": len(x_train),
            "loss": float(history.history["loss"][-1]),
            "model_saved": True
        }), 200
        
    except Exception as e:
        print(f"❌ Ошибка в /train: {e}")
        return jsonify({"error": str(e)}), 500

@app.route('/info', methods=['GET'])
def info():
    """Получить информацию о сервере и модели"""
    return jsonify({
        "name": "Minecraft AI Server",
        "version": "1.0.0",
        "endpoints": [
            "/health - Проверка статуса",
            "/predict - Предсказание действия",
            "/train - Обучение модели",
            "/info - Информация о сервере"
        ],
        "model_loaded": is_model_loaded,
        "model_path": AI_MODEL_PATH
    }), 200

# ==================== ЗАПУСК СЕРВЕРА ====================

if __name__ == "__main__":
    print("🚀 Запуск AI Server для Minecraft...")
    print(f"📡 Адрес: http://{HOST}:{PORT}")
    
    load_model()
    
    print("✅ Сервер готов!")
    print("📚 API документация: http://localhost:5000/info")
    
    app.run(host=HOST, port=PORT, debug=False, threaded=True)
