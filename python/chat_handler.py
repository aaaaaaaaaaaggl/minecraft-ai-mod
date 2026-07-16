"""
Обработчик сообщений чата Minecraft
Получает команды из чата и отправляет ответы
"""

from flask import Blueprint, request, jsonify
from nlp_processor import NLPProcessor
import json
from typing import Dict

# Создание Blueprint для чата
chat_bp = Blueprint('chat', __name__)

# Инициализация NLP процессора
nlp = NLPProcessor()

# Хранение игроков и их истории команд
player_history = {}

@chat_bp.route('/chat/message', methods=['POST'])
def handle_chat_message():
    """
    Обработать сообщение из чата Minecraft
    
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
        data = request.get_json()
        
        if not data:
            return jsonify({"error": "No JSON data provided"}), 400
        
        required_fields = ["player_name", "message"]
        for field in required_fields:
            if field not in data:
                return jsonify({"error": f"Missing field: {field}"}), 400
        
        player_name = data["player_name"]
        message = data["message"]
        x = data.get("x", 0)
        y = data.get("y", 64)
        z = data.get("z", 0)
        
        # Сохранить в историю
        if player_name not in player_history:
            player_history[player_name] = []
        player_history[player_name].append(message)
        
        # Обработать команду
        result = nlp.process_command(message)
        
        # Добавить информацию об игроке
        result["player_name"] = player_name
        result["timestamp"] = __import__('datetime').datetime.now().isoformat()
        
        return jsonify(result), 200
        
    except Exception as e:
        print(f"❌ Ошибка в /chat/message: {e}")
        return jsonify({"error": str(e)}), 500

@chat_bp.route('/chat/broadcast', methods=['POST'])
def broadcast_message():
    """
    Отправить сообщение всем игрокам в чат
    
    JSON body:
    {
        "message": "Привет, все!",
        "type": "info"  # info, warning, success, error
    }
    """
    try:
        data = request.get_json()
        
        if not data or "message" not in data:
            return jsonify({"error": "Missing message field"}), 400
        
        message = data["message"]
        msg_type = data.get("type", "info")
        
        # Форматирование сообщения в зависимости от типа
        formatted = format_message(message, msg_type)
        
        return jsonify({
            "success": True,
            "message": formatted,
            "type": msg_type
        }), 200
        
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@chat_bp.route('/chat/reply/<player_name>', methods=['POST'])
def reply_to_player(player_name):
    """
    Отправить ответ конкретному игроку
    
    JSON body:
    {
        "message": "Привет, Player!"
    }
    """
    try:
        data = request.get_json()
        
        if not data or "message" not in data:
            return jsonify({"error": "Missing message field"}), 400
        
        message = data["message"]
        
        return jsonify({
            "success": True,
            "player_name": player_name,
            "message": message,
            "timestamp": __import__('datetime').datetime.now().isoformat()
        }), 200
        
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@chat_bp.route('/chat/history/<player_name>', methods=['GET'])
def get_player_history(player_name):
    """
    Получить историю команд игрока
    """
    history = player_history.get(player_name, [])
    
    return jsonify({
        "player_name": player_name,
        "commands": history,
        "count": len(history)
    }), 200

def format_message(message: str, msg_type: str) -> str:
    """
    Форматировать сообщение для чата
    
    Minecraft цветовые коды:
    §0 - черный
    §1 - синий
    §2 - зеленый
    §3 - светло-голубой
    §4 - красный
    §5 - фиолетовый
    §6 - золотой
    §7 - серый
    §8 - темно-серый
    §9 - синий
    §a - зеленый
    §b - светло-голубой
    §c - красный
    §d - магента
    §e - желтый
    §f - белый
    """
    
    if msg_type == "success":
        return f"§a✓ {message}§r"
    elif msg_type == "error":
        return f"§c✗ {message}§r"
    elif msg_type == "warning":
        return f"§e⚠ {message}§r"
    elif msg_type == "ai":
        return f"§b🤖 AI: {message}§r"
    else:  # info
        return f"§7[i] {message}§r"

def get_blueprint():
    """Получить Blueprint для регистрации в приложении"""
    return chat_bp
