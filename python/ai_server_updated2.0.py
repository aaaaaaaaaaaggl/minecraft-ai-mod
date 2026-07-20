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
import random

app = Flask(__name__)
CORS(app)

# Конфигурация
PORT = 5000
HOST = "0.0.0.0"

# Глобальные переменные
model = None
is_model_loaded = False
ai_enabled = True

# ==================== AI РАЗГОВОРНЫЙ ДВИЖОК ====================

class ConversationAI:
    """
    AI-подобный разговорный движок для Minecraft чата.
    Использует паттерны и контекст для генерации естественных ответов на русском языке.
    """

    def __init__(self):
        self.conversation_history = {}  # player_name -> list of messages
        self.max_history = 10  # максимальное количество пар (игрок + AI = 20 сообщений)

        # Паттерны для распознавания тематики
        self.patterns = [
            {
                "keywords": ["привет", "здравствуй", "здарова", "хей", "хай", "салют",
                             "добрый день", "добрый вечер", "доброе утро", "hi", "hello"],
                "responses": [
                    "Привет! 😊 Я AI помощник для Minecraft. Чем могу помочь?",
                    "Здравствуй! Рад тебя видеть! Что хочешь сделать сегодня в Minecraft?",
                    "Привет-привет! Готов помочь тебе покорить мир Minecraft! ⚔️",
                    "Хей! Я здесь и готов к работе! Строить, призывать, генерировать ресурсы — просто скажи! 🚀",
                ]
            },
            {
                "keywords": ["пока", "до свидания", "прощай", "до встречи", "увидимся", "bye", "goodbye"],
                "responses": [
                    "Пока! Возвращайся скорее, буду ждать! 👋",
                    "До свидания! Удачи в игре! 🎮",
                    "Пока-пока! Надеюсь, я тебе помог! 😊",
                ]
            },
            {
                "keywords": ["как дела", "как ты", "что нового", "как жизнь", "как поживаешь", "всё хорошо"],
                "responses": [
                    "Дела отлично! Я AI, так что у меня всегда хорошее настроение 😄 А у тебя как?",
                    "Хорошо, спасибо что спросил! Готов помогать тебе в Minecraft! Что строим? 🏗️",
                    "Замечательно! Обрабатываю команды, генерирую структуры — обычный день AI помощника! А ты как?",
                ]
            },
            {
                "keywords": ["блок", "материал", "камень", "дерево", "железо", "алмаз", "золото", "ресурс"],
                "responses": [
                    "В Minecraft тысячи блоков! Могу помочь генерировать руды: алмазы, золото, железо. Просто напиши 'генерируй алмазы'! ⛏️",
                    "Блоки — это основа Minecraft! Я могу генерировать жилы руд для тебя. Попробуй 'генерируй золото'! 💰",
                    "О ресурсах говоришь? Могу создать жилу любой руды прямо рядом с тобой! Напиши 'генерируй алмазы'! 💎",
                ]
            },
            {
                "keywords": ["строить", "построить", "здание", "замок", "архитектура", "постройка", "строение"],
                "responses": [
                    "Строительство — моя страсть! Могу построить дом, башню или мост. Попробуй 'строй дом'! 🏠",
                    "Хочешь что-то построить? Я умею строить дома, башни и мосты! Напиши 'строй башню'! 🗼",
                    "Архитектура в Minecraft бесконечна! Начнём с чего-нибудь простого? Попробуй 'строй дом'! 🏗️",
                ]
            },
            {
                "keywords": ["моб", "существо", "монстр", "враг", "зомби", "скелет", "крипер", "животное"],
                "responses": [
                    "Мобы делают Minecraft интереснее! Могу призвать зомби, скелета или крипера. Попробуй 'призови зомби'! 🧟",
                    "Хочешь поборзиться с монстрами? Напиши 'призови крипера' — но осторожно, взрывной характер! 💥",
                    "Могу призвать любого моба рядом с тобой! Напиши 'призови скелета' или выбери другого! ⚔️",
                ]
            },
            {
                "keywords": ["что умеешь", "что можешь", "команды", "возможности", "функции"],
                "responses": [
                    "Вот что я умею: 🌟\n• строй дом / башню / мост\n• призови зомби / скелета / крипера\n• генерируй алмазы / золото / железо\n• и просто поговорить с тобой! 😊",
                    "Мои суперспособности: строить здания, призывать мобов, генерировать ресурсы и вести умные беседы! 🤖",
                ]
            },
            {
                "keywords": ["кто ты", "что ты", "ты робот", "ты ai", "ты нейросеть", "ты бот", "ты человек"],
                "responses": [
                    "Я AI помощник для Minecraft! 🤖 Создан чтобы помогать тебе строить, призывать мобов и генерировать ресурсы. Спрашивай что угодно!",
                    "Я цифровой разум, встроенный в Minecraft! 🧠 Моя цель — сделать твою игру лучше. Чем могу помочь?",
                    "Я Minecraft AI — твой виртуальный помощник! Умею строить, призывать и добывать. Готов служить! ⚡",
                ]
            },
            {
                "keywords": ["летать", "прыгнуть", "бежать", "двигаться", "физика"],
                "responses": [
                    "Я не могу летать в реальной жизни, но зато могу помочь тебе строить невероятные здания в Minecraft! 🏗️",
                    "Летать? Мне не нужны крылья — у меня есть код! 😄 Лучше построю тебе башню до небес! 🗼",
                ]
            },
            {
                "keywords": ["спасибо", "благодарю", "thanks", "thank you", "thx", "tnx"],
                "responses": [
                    "Пожалуйста! Всегда рад помочь! 😊",
                    "Не за что! Обращайся если что-то ещё нужно! 🚀",
                    "Рад стараться! Это моя работа — помогать тебе! ⚡",
                ]
            },
            {
                "keywords": ["игра", "играть", "майнкрафт", "minecraft", "мод", "мир", "сервер"],
                "responses": [
                    "Minecraft — лучшая игра для творчества! 🎮 Я здесь чтобы сделать её ещё лучше. Что хочешь создать?",
                    "Мир Minecraft бесконечен, как и возможности! Я помогаю с постройками, мобами и ресурсами. Чем займёмся?",
                    "Этот мир Minecraft — наш с тобой! 🌍 Что будем строить или исследовать сегодня?",
                ]
            },
            {
                "keywords": ["крафт", "скрафтить", "создать", "сделать", "craft"],
                "responses": [
                    "Крафтинг — основа Minecraft! Хотя я не могу крафтить за тебя, зато могу генерировать ресурсы рядом с тобой! ⛏️",
                    "Нужны материалы для крафта? Напиши 'генерируй алмазы' или 'генерируй железо'! 💎",
                ]
            },
            {
                "keywords": ["выживание", "survival", "умереть", "смерть", "здоровье", "еда", "голод"],
                "responses": [
                    "В режиме выживания главное — не голодать! Могу помочь с ресурсами: 'генерируй железо' для инструментов! 🛡️",
                    "Выживание требует ресурсов! Напиши 'генерируй алмазы' чтобы получить лучшую броню и оружие! 💎",
                ]
            },
            {
                "keywords": ["ночь", "темно", "факел", "освещение", "свет"],
                "responses": [
                    "Ночью в Minecraft опасно! Построй дом чтобы укрыться: напиши 'строй дом'! 🏠",
                    "Темнота — друг монстров! Могу построить тебе убежище: попробуй 'строй дом'! 🌙",
                ]
            },
            {
                "keywords": ["да", "нет", "ок", "окей", "хорошо", "ладно", "понял", "ясно", "конечно"],
                "responses": [
                    "Отлично! Что делаем? 😊",
                    "Понял тебя! Чем могу помочь? 🚀",
                    "Договорились! Скажи что нужно! ⚡",
                ]
            },
        ]

        # Общие ответы когда паттерн не найден
        self.fallback_responses = [
            "Интересный вопрос! Я пока учусь, но постараюсь помочь. Попробуй написать 'помощь' чтобы увидеть что я умею! 🤖",
            "Хм, это сложный вопрос для AI... 🤔 Но я могу помочь со строительством, мобами и ресурсами! Напиши 'помощь'!",
            "Не совсем понял, но я готов помогать! Могу строить, призывать мобов или генерировать ресурсы. Что выберешь? 😊",
            "Обрабатываю... 🧠 Если хочешь узнать мои возможности — напиши 'помощь'! Там есть все команды.",
            "Интересно! Я AI помощник в мире Minecraft. Могу помочь с постройками и ресурсами. Что нужно? 🚀",
            "Мой нейронный интерфейс принял твоё сообщение! 🤖 Скажи что ты хочешь — строить, призвать кого-то, или добыть ресурсы?",
        ]

    def get_response(self, player_name: str, message: str) -> str:
        """
        Получить AI ответ на сообщение игрока.

        Args:
            player_name: имя игрока
            message: сообщение игрока

        Returns:
            str: ответ AI на русском языке
        """
        if player_name not in self.conversation_history:
            self.conversation_history[player_name] = []

        self.conversation_history[player_name].append({
            "role": "player",
            "message": message,
            "timestamp": datetime.now().isoformat()
        })

        message_lower = message.lower()
        response = self._find_response(message_lower, player_name)

        self.conversation_history[player_name].append({
            "role": "ai",
            "message": response,
            "timestamp": datetime.now().isoformat()
        })

        # Ограничиваем длину истории
        if len(self.conversation_history[player_name]) > self.max_history * 2:
            self.conversation_history[player_name] = \
                self.conversation_history[player_name][-self.max_history * 2:]

        return response

    def _find_response(self, message_lower: str, player_name: str) -> str:
        """Найти подходящий ответ на основе паттернов"""
        for pattern in self.patterns:
            for keyword in pattern["keywords"]:
                if keyword in message_lower:
                    return random.choice(pattern["responses"])

        history = self.conversation_history.get(player_name, [])
        if len(history) <= 1:
            return (
                f"Привет, {player_name}! Я AI помощник Minecraft. "
                "Не совсем понял команду, но готов помочь! "
                "Напиши 'помощь' чтобы увидеть все возможности. 🤖"
            )

        return random.choice(self.fallback_responses)

    def get_history(self, player_name: str) -> list:
        """Получить историю разговора игрока"""
        return self.conversation_history.get(player_name, [])

    def clear_history(self, player_name: str):
        """Очистить историю разговора игрока"""
        self.conversation_history.pop(player_name, None)


# Глобальный экземпляр разговорного AI
conversation_ai = ConversationAI()

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
        
        # Simple command dispatch with action-specific parameters
        message_lower = message.lower()
        extra = {}

        if "помощь" in message_lower or "help" in message_lower:
            action = "help"
            response_msg = "§6=== AI Помощь ===\n§aДоступные команды:\n§e- строй дом\n§e- строй башню\n§e- строй мост\n§e- призови зомби\n§e- призови скелета\n§e- призови крипера\n§e- генерируй алмазы\n§e- генерируй золото\n§e- генерируй железо"
        elif "строй" in message_lower or "build" in message_lower:
            action = "build_structure"
            if "башн" in message_lower or "tower" in message_lower:
                structure_type = "tower"
            elif "мост" in message_lower or "bridge" in message_lower:
                structure_type = "bridge"
            else:
                structure_type = "house"
            response_msg = f"§a🏗️  Начинаю строительство: {structure_type}..."
            extra = {"structure_type": structure_type}
        elif "призови" in message_lower or "spawn" in message_lower:
            action = "spawn_mob"
            if "скелет" in message_lower or "skeleton" in message_lower:
                mob_type = "skeleton"
            elif "крипер" in message_lower or "creeper" in message_lower:
                mob_type = "creeper"
            else:
                mob_type = "zombie"
            # Parse count if given (e.g. "призови 5 зомби")
            count = 3
            for word in message_lower.split():
                if word.isdigit():
                    count = max(1, min(int(word), 10))
                    break
            response_msg = f"§a👹 Призываю {count} {mob_type}..."
            extra = {"mob_type": mob_type, "count": count}
        elif "генерируй" in message_lower or "generate" in message_lower:
            action = "generate_ore"
            if "алмаз" in message_lower or "diamond" in message_lower:
                ore_type = "diamond"
                vein_size = 6
            elif "золот" in message_lower or "gold" in message_lower:
                ore_type = "gold"
                vein_size = 8
            else:
                ore_type = "iron"
                vein_size = 10
            response_msg = f"§a⛏️  Генерирую жилу {ore_type} ({vein_size} блоков)..."
            extra = {"ore_type": ore_type, "vein_size": vein_size}
        else:
            action = "chat"
            ai_response = conversation_ai.get_response(player_name, message)
            response_msg = f"§b🤖 AI: §f{ai_response}"
        
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
        result.update(extra)
        
        print(f"💬 {player_name}: {message} -> {action}")
        
        return jsonify(result), 200
        
    except Exception as e:
        print(f"❌ Ошибка в /chat: {e}")
        return jsonify({
            "success": False,
            "error": str(e),
            "message": "§c❌ Ошибка обработки команды"
        }), 500

@app.route('/chat/history', methods=['GET'])
def chat_history():
    """
    Получить историю разговора игрока

    Query params:
        player_name: имя игрока
    """
    player_name = request.args.get("player_name", "")
    if not player_name:
        return jsonify({"success": False, "error": "Missing player_name parameter"}), 400
    history = conversation_ai.get_history(player_name)
    return jsonify({
        "success": True,
        "player_name": player_name,
        "history": history,
        "count": len(history)
    }), 200

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
            "/chat - Обработка команд и AI разговор",
            "/chat/history - История разговора игрока",
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
    print("   - 'привет, как дела?' (AI разговор)")
    print("   - 'расскажи про блоки' (AI разговор)")
    print("")
    
    app.run(host=HOST, port=PORT, debug=False, threaded=True)
