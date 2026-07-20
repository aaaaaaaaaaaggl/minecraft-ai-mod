# 🤖 Minecraft AI Mod

Интеграция нейросетей в Minecraft через Python API и REST.

## ✨ Функции

- 🤖 Анализ окружения игрока нейросетью
- 🏗️ Генерация структур на основе AI
- 📡 REST API для связи Python + Minecraft
- ⚙️ Простая интеграция с модами
- 🎮 Спигот плагин для серверов

## 📁 Структура проекта

```
minecraft-ai-mod/
├── python/
│   ├── ai_server.py          # Flask сервер с нейросетью
│   ├── model_trainer.py      # Обучение модели
│   ├── requirements.txt       # Python зависимости
│   └── model.h5              # Обученная модель (игнорируется)
├── java/
│   ├── src/main/java/
│   │   └── com/minecraft/ai/
│   │       ├── AIPlugin.java
│   │       ├── AIApiClient.java
│   │       └── AIEventListener.java
│   ├── src/main/resources/
│   │   └── plugin.yml
│   └── pom.xml               # Maven конфигурация
├── config/
│   └── ai_config.yaml        # Конфигурация
└── SETUP.md                  # Инструкция по установке
```

## 🚀 Быстрый старт

### 1. Запуск AI сервера (Python)

```bash
cd python
pip install -r requirements.txt
python model_trainer.py      # Опционально: обучить модель
python ai_server_updated2.0.py          # Запустить сервер
```

Сервер запустится на `http://localhost:5000`

### 2. Сборка Java плагина

```bash
cd java
mvn clean package
# Jar файл: target/ai-mod-1.0.0.jar
```

### 3. Установка мода в Minecraft

1. Скопируйте JAR файл в папку `plugins` сервера
2. Перезагрузите сервер
3. Используйте команду `/ai status`

## 📡 API Endpoints

### GET `/health`
Проверка статуса сервера.

```bash
curl http://localhost:5000/health
```

**Ответ:**
```json
{
  "status": "online",
  "model_loaded": true,
  "version": "1.0.0"
}
```

### POST `/predict`
Получить предсказание AI для действия в игре.

```bash
curl -X POST http://localhost:5000/predict \
  -H "Content-Type: application/json" \
  -d '{
    "x": 100,
    "y": 64,
    "z": 200,
    "block_type": "grass"
  }'
```

**Ответ:**
```json
{
  "action": "build_structure",
  "structure_type": "tower",
  "height": 15,
  "confidence": 0.95,
  "x": 100,
  "y": 64,
  "z": 200
}
```

### POST `/train`
Обучить модель на новых данных.

```bash
curl -X POST http://localhost:5000/train \
  -H "Content-Type: application/json" \
  -d '{
    "x_data": [[100, 64, 200, 0.5], ...],
    "y_data": [[1, 0, 0], ...]
  }'
```

### GET `/info`
Получить информацию о сервере.

```bash
curl http://localhost:5000/info
```

## 🎮 Minecraft команды

```
/ai status                    # Статус AI сервера
/ai enable                    # Включить AI
/ai disable                   # Выключить AI
/ai build <type>             # Заказать построение
```

## 📋 Требования

- **Python:** 3.8+
- **Java:** 11+
- **Minecraft:** 1.19+
- **TensorFlow:** 2.x
- **Spigot/Paper:** для сервера
- по умолчанию Spigot

## 🔧 Конфигурация

Отредактируйте `config/ai_config.yaml`:

```yaml
ai_server:
  url: "http://localhost:5000"
  timeout: 5000

ai_settings:
  enabled: true
  min_confidence: 0.7
  check_interval: 20
```

## 📚 Документация

- **SETUP.md** - Полная инструкция по установке
- **STRUCTURE.md** - Описание структуры проекта
- **docs/API.md** - Детальная API документация

## 🐛 Решение проблем

### "Connection refused"
```bash
# Проверьте, запущен ли AI сервер
ps aux | grep ai_server.py
```

### "Model not found"
```bash
cd python
python model_trainer.py
```

### "Cannot compile Java"
```bash
java -version  # Требуется Java 11+
mvn clean package
```

## 💡 Примеры использования

### Пример 1: Простое предсказание

```python
import requests

response = requests.post('http://localhost:5000/predict', json={
    'x': 100,
    'y': 64,
    'z': 200,
    'block_type': 'grass'
})

result = response.json()
print(f"Действие: {result['action']}")
print(f"Уверенность: {result['confidence']}")
```

### Пример 2: Обучение модели

```bash
cd python
python model_trainer.py
```

## 🤝 Развитие проекта

Добавление новых функций:

1. **Новое действие AI**
   - Обновить модель в `model_trainer.py`
   - Добавить обработчик в `AIEventListener.java`

2. **Новые параметры**
   - Обновить JSON в `ai_server.py`
   - Изменить конфигурацию

## 📄 Лицензия

MIT License - см. LICENSE файл

## 📞 Контакты

- GitHub Issues: https://github.com/aaaaaaaaaaaggl/minecraft-ai-mod/issues
- Документация: /docs

---

**Версия:** 1.0.0  
**Автор:** Development Team  
**Последнее обновление:** 2024-01-01  

🎮 Enjoy your AI-powered Minecraft experience!
