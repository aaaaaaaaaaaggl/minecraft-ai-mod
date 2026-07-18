"""
Обновленный AI Server с поддержкой чата и команд
Запуск: python ai_server_updated.py
"""

from flask import Flask, request, jsonify
from flask_cors import CORS
import numpy as np
import tensorflow as tf
from datetime import datetime
import os
import json
from nlp_processor import NLPProcessor
from chat_handler import get_blueprint, format_message

app = Flask(__name__)
CORS(app)

# Конфигурация
AI_MODEL_PATH = "model.h5"
PORT = 5000
HOST = "0.0.0.0"

# Глобальные переменные
model = None
is_model_loaded = False
nlp_processor = NLPProcessor()
ai_enabled = True

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
        "ai_enabled": ai_enabled,
        "version": "2.0.0"
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

@app.route('/chat', methods=['POST'])
def handle_chat():
    """
    Основной endpoint для обработки команд чата из Java плагина
    
    JSON body:
    {
        "player_name": "PlayerName",
        "message": "строй дом",
        "x": 100,
        "y": 64,
        "z": 200
    }
    """
    try:
        if not ai_enabled:
            return jsonify({
                "success": False,
                "message": "§cAI отключён! Используйте /ai enable§r",
                "action": "none"
            }), 403
        
        data = request.get_json()
        
        if not data:
            return jsonify({"error": "No JSON data provided"}), 400
        
        player_name = data.get("player_name", "Unknown")
        message = data.get("message", "")
        x = data.get("x", 0)
        y = data.get("y", 64)
        z = data.get("z", 0)
        
        # Обработать команду через NLP
        result = nlp_processor.process_command(message)
        
        # Добавить информацию
        result["player_name"] = player_name
        result["x"] = x
        result["y"] = y
        result["z"] = z
        result["timestamp"] = datetime.now().isoformat()
        
        print(f"💬 {player_name}: {message} -> {result['action']}")
        
        return jsonify(result), 200
        
    except Exception as e:
        print(f"❌ Ошибка в /chat: {e}")
        return jsonify({"error": str(e)}), 500

@app.route('/chat/command', methods=['POST'])
def handle_chat_command():
    """
    Обработать команду из чата (совместимость)
    
    JSON body:
    {
        "player_name": "PlayerName",
        "message": "строй дом",
        "x": 100,
        "y": 64,
        "z": 200
    }
    """
    return handle_chat()

@app.route('/chat/broadcast', methods=['POST'])
def broadcast_message():
    """
    Отправить сообщение всем игрокам
    """
    try:
        data = request.get_json()
        
        if not data or "message" not in data:
            return jsonify({"error": "Missing message field"}), 400
        
        message = data["message"]
        msg_type = data.get("type", "ai")
        
        formatted = format_message(message, msg_type)
        
        return jsonify({
            "success": True,
            "formatted_message": formatted,
            "message": message,
            "type": msg_type
        }), 200
        
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route('/ai/enable', methods=['POST'])
def enable_ai():
    """Включить AI"""
    global ai_enabled
    ai_enabled = True
    return jsonify({
        "success": True,
        "message": "§a✓ AI включён!§r",
        "ai_enabled": ai_enabled
    }), 200

@app.route('/ai/disable', methods=['POST'])
def disable_ai():
    """Выключить AI"""
    global ai_enabled
    ai_enabled = False
    return jsonify({
        "success": True,
        "message": "§c✗ AI отключён§r",
        "ai_enabled": ai_enabled
    }), 200

@app.route('/ai/status', methods=['GET'])
def ai_status():
    """Получить статус AI"""
    return jsonify({
        "ai_enabled": ai_enabled,
        "model_loaded": is_model_loaded,
        "status": "✓ Готов" if ai_enabled else "✗ Отключен",
        "version": "2.0.0"
    }), 200

@app.route('/info', methods=['GET'])
def info():
    """Получить информацию о сервере"""
    return jsonify({
        "name": "Minecraft AI Chat Server",
        "version": "2.0.0",
        "endpoints": [
            "/health - Проверка статуса",
            "/chat - Обработка команд из чата",
            "/chat/command - Обработка команд из чата (совместимость)",
            "/chat/broadcast - Отправка сообщения всем",
            "/ai/enable - Включить AI",
            "/ai/disable - Выключить AI",
            "/ai/status - Статус AI",
            "/predict - Предсказание действия",
            "/info - Информация о сервере"
        ],
        "model_loaded": is_model_loaded,
        "ai_enabled": ai_enabled
    }), 200

# ==================== ЗАПУСК СЕРВЕРА ====================

if __name__ == "__main__":
    print("🚀 Запуск AI Chat Server для Minecraft...")
    print(f"📡 Адрес: http://{HOST}:{PORT}")
    
    load_model()
    print("✅ NLP процессор инициализирован")
    print("✅ Сервер готов!")
    print("📚 API документация: http://localhost:5000/info")
    print("💬 Примеры команд чата:")
    print("   - 'строй дом'")
    print("   - 'призови 5 зомби'")
    print("   - 'генерируй алмазы'")
    print("   - 'помощь'")
    print("")
    
    app.run(host=HOST, port=PORT, debug=False, threaded=True)
