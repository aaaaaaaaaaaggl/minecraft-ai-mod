"""
AI Server для Minecraft с поддержкой чата
Запуск: python ai_server_updated2.0.py
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
PORT = 5000
HOST = "0.0.0.0"

# Глобальные переменные
model = None
is_model_loaded = False
ai_enabled = True

# ==================== ИНИЦИАЛИЗАЦИЯ ====================

def load_model():
    """Загрузить или создать модель"""
    global model, is_model_loaded
    
    try:
        model = tf.keras.Sequential([
            tf.keras.layers.Dense(64, activation='relu', input_shape=(4,)),
            tf.keras.layers.Dense(32, activation='relu'),
            tf.keras.layers.Dense(3, activation='softmax')
        ])
        model.compile(optimizer='adam', loss='categorical_crossentropy')
        is_model_loaded = True
        print("✅ Модель инициализирована")
    except Exception as e:
        print(f"❌ Ошибка при инициализации модели: {e}")

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

@app.route('/chat', methods=['POST'])
def handle_chat():
    """
    Главный endpoint для обработки команд чата из Java плагина
    
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
                "message": "§cAI отключён!",
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
        
        # Простая обработка команд
        message_lower = message.lower()
        
        if "помощь" in message_lower or "help" in message_lower:
            action = "help"
            response_msg = "§6=== AI Помощь ===\n§aДоступные команды:\n§e- строй дом\n§e- призови зомби\n§e- генерируй золото"
        elif "строй" in message_lower or "build" in message_lower:
            action = "build_structure"
            response_msg = "§a🏗️  Начинаю строительство дома..."
        elif "призови" in message_lower or "spawn" in message_lower:
            action = "spawn_mob"
            response_msg = "§a👹 Призываю мобов..."
        elif "генерируй" in message_lower or "generate" in message_lower:
            action = "generate_ore"
            response_msg = "§a⛏️  Генерирую руду..."
        else:
            action = "unknown"
            response_msg = "§c❌ Команда не распознана. Напиши 'помощь'"
        
        result = {
            "success": True,
            "message": response_msg,
            "action": action,
            "player_name": player_name,
            "x": x,
            "y": y,
            "z": z,
            "timestamp": datetime.now().isoformat()
        }
        
        print(f"💬 {player_name}: {message} -> {action}")
        
        return jsonify(result), 200
        
    except Exception as e:
        print(f"❌ Ошибка в /chat: {e}")
        return jsonify({
            "success": False,
            "error": str(e),
            "message": "§c❌ Ошибка обработки команды"
        }), 500

@app.route('/ai/enable', methods=['POST'])
def enable_ai():
    """Включить AI"""
    global ai_enabled
    ai_enabled = True
    return jsonify({
        "success": True,
        "message": "§a✅ AI включён!",
        "ai_enabled": ai_enabled
    }), 200

@app.route('/ai/disable', methods=['POST'])
def disable_ai():
    """Выключить AI"""
    global ai_enabled
    ai_enabled = False
    return jsonify({
        "success": True,
        "message": "§c❌ AI отключён",
        "ai_enabled": ai_enabled
    }), 200

@app.route('/ai/status', methods=['GET'])
def ai_status():
    """Получить статус AI"""
    return jsonify({
        "ai_enabled": ai_enabled,
        "model_loaded": is_model_loaded,
        "status": "✅ Готов" if ai_enabled else "❌ Отключен",
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
            "/ai/enable - Включить AI",
            "/ai/disable - Выключить AI",
            "/ai/status - Статус AI",
            "/info - Информация о сервере"
        ],
        "model_loaded": is_model_loaded,
        "ai_enabled": ai_enabled
    }), 200

# ==================== ЗАПУСК СЕРВЕРА ====================

if __name__ == "__main__":
    print("🚀 Запуск Minecraft AI Chat Server...")
    print(f"📡 Адрес: http://{HOST}:{PORT}")
    print("")
    
    load_model()
    print("✅ Сервер готов к работе!")
    print("📚 API документация: http://localhost:5000/info")
    print("")
    print("💬 Примеры команд в чате:")
    print("   - 'строй дом'")
    print("   - 'призови зомби'")
    print("   - 'генерируй золото'")
    print("   - 'помощь'")
    print("")
    
    app.run(host=HOST, port=PORT, debug=False, threaded=True)
