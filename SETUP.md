# 🚀 Инструкция по установке Minecraft AI Mod

## Требования

- **Python 3.8+**
- **Java 11+**
- **Minecraft 1.19+**
- **Maven** (для сборки Java плагина)

---

## 1️⃣ Установка Python AI Server

### Шаг 1: Установка зависимостей

```bash
cd python
pip install -r requirements.txt
```

### Шаг 2: Обучение модели (опционально)

```bash
python model_trainer.py
```

Это создаст файл `model.h5` с обученной моделью.

### Шаг 3: Запуск AI сервера

```bash
python ai_server.py
```

Вы должны увидеть:
```
🚀 Запуск AI Server для Minecraft...
📡 Адрес: http://0.0.0.0:5000
✅ Модель загружена: model.h5
✅ Сервер готов!
```

---

## 2️⃣ Сборка Java плагина

### Шаг 1: Установка Maven

Скачайте Maven: https://maven.apache.org/download.cgi

### Шаг 2: Сборка плагина

```bash
cd java
mvn clean package
```

JAR файл будет создан в `java/target/ai-mod-1.0.0.jar`

### Шаг 3: Установка в Minecraft

Скопируйте JAR файл в папку `plugins` вашего сервера:
```bash
cp java/target/ai-mod-1.0.0.jar /path/to/minecraft/plugins/
```

---

## 3️⃣ Запуск Minecraft сервера

### Для Spigot/Paper:

1. Скачайте Spigot BuildTools
2. Поместите плагин в папку `plugins`
3. Запустите сервер

```bash
java -Xmx1024M -Xms1024M -jar spigot.jar nogui
```

### Проверка

В логах сервера вы должны увидеть:
```
[12:34:56] [Server thread/INFO]: [Minecraft AI Plugin] ✅ Плагин успешно загружен!
[12:34:57] [Server thread/INFO]: ✅ Подключение к AI серверу успешно!
```

---

## 4️⃣ Использование

### Команды:

```
/ai status        - Статус AI сервера
/ai enable        - Включить AI
/ai disable       - Выключить AI
/ai build <type>  - Заказать построение
```

### API Endpoints:

- **GET** `/health` - Проверка статуса
- **POST** `/predict` - Предсказание действия
- **POST** `/train` - Обучение модели
- **GET** `/info` - Информация о сервере

---

## 🧪 Тестирование

### Тест AI сервера:

```bash
curl http://localhost:5000/health
```

Ответ:
```json
{
  "status": "online",
  "timestamp": "2024-01-01T12:34:56.789Z",
  "model_loaded": true,
  "version": "1.0.0"
}
```

### Тест предсказания:

```bash
curl -X POST http://localhost:5000/predict \
  -H "Content-Type: application/json" \
  -d '{"x": 100, "y": 64, "z": 200, "block_type": "grass"}'
```

---

## ⚙️ Конфигурация

### Python (`python/ai_server.py`):

Измените параметры в начале файла:
```python
PORT = 5000
HOST = "0.0.0.0"
AI_MODEL_PATH = "model.h5"
```

### Java (`config/ai_config.yaml`):

```yaml
ai_server:
  url: "http://localhost:5000"
  timeout: 5000
ai_settings:
  min_confidence: 0.7
  check_interval: 20
```

---

## 🐛 Решение проблем

### Проблема: "Connection refused"

**Решение:** Убедитесь, что AI сервер запущен:
```bash
ps aux | grep ai_server.py
```

### Проблема: "Model not found"

**Решение:** Обучите модель:
```bash
cd python
python model_trainer.py
```

### Проблема: "Cannot compile Java"

**Решение:** Проверьте версию Java:
```bash
java -version
```

Требуется Java 11 или выше.

---

## 📚 Дополнительная информация

- Документация TensorFlow: https://www.tensorflow.org/
- Документация Spigot: https://www.spigotmc.org/wiki/
- REST API примеры в папке `examples/`

---

## ✅ Готово!

Теперь у вас работает Minecraft AI Mod! 🎮🤖
