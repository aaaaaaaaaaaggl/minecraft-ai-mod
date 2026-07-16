# 📁 Структура проекта Minecraft AI Mod

```
minecraft-ai-mod/
│
├── 📂 python/                          # Python компонент
│   ├── ai_server.py                   # Flask REST API сервер (ОСНОВНОЙ ФАЙЛ)
│   ├── model_trainer.py               # Скрипт для обучения нейросети
│   ├── requirements.txt                # Python зависимости
│   ├── .gitignore                     # Игнорирование Python файлов
│   └── model.h5                       # Обученная модель (создается после обучения)
│
├── 📂 java/                            # Java компонент (Spigot плагин)
│   ├── src/main/java/com/minecraft/ai/
│   │   ├── AIPlugin.java              # Главный класс плагина
│   │   ├── AIApiClient.java           # HTTP клиент для API
│   │   └── AIEventListener.java       # Слушатель событий Minecraft
│   ├── src/main/resources/
│   │   └── plugin.yml                 # Конфигурация плагина
│   ├── pom.xml                        # Maven конфигурация
│   └── target/                        # Скомпилированные JAR файлы
│
├── 📂 config/                          # Конфигурационные файлы
│   └── ai_config.yaml                 # Основная конфигурация
│
├── 📂 docs/                            # Документация
│   ├── API.md                         # API документация
│   └── TROUBLESHOOTING.md             # Решение проблем
│
├── .gitignore                         # Git игнорирование
├── README.md                          # Главная документация
├── SETUP.md                           # Инструкция по установке
├── STRUCTURE.md                       # Этот файл
└── LICENSE                            # Лицензия MIT
```

---

## 📋 Описание основных файлов

### Python компонент

#### `ai_server.py` (Основной файл)
```python
Функции:
- load_model()              # Загрузить нейро��еть
- create_dummy_model()      # Создать демо-модель
- /health                   # GET - проверка статуса
- /predict                  # POST - предсказание действия
- /train                    # POST - обучение модели
- /info                     # GET - информация о сервере
```

#### `model_trainer.py`
```python
Функции:
- generate_training_data()  # Генерировать синтетические данные
- create_model()            # Создать архитектуру нейросети
- train_and_save()          # Обучить и сохранить модель
- test_model()              # Протестировать модель
```

### Java компонент

#### `AIPlugin.java` (Главный класс)
```java
Методы:
- onEnable()               # Инициализация плагина
- onDisable()              # Выключение плагина
- startAITask()            # Запуск асинхронного таска
- registerCommands()       # Регистрация команд
- checkServerConnection()  # Проверка подключения
```

#### `AIApiClient.java` (HTTP клиент)
```java
Методы:
- checkHealth()            # Проверить здоровье сервера
- predict()                # Получить предсказание AI
- trainModel()             # Обучить модель
- getServerInfo()          # Получить информацию сервера
```

#### `AIEventListener.java` (События)
```java
Методы:
- onPlayerJoin()           # Присоединение игрока
- onPlayerQuit()           # Отключение игрока
- onBlockPlace()           # Установка блока
- processPlayerAI()        # Обработка AI для игрока
- executeAIAction()        # Выполнение действия
```

---

## 🔄 Поток данных

```
Minecraft Client
    ↓
Spigot Server (Java плагин)
    ↓
AIApiClient (HTTP POST)
    ↓
Flask AI Server (Python)
    ↓
TensorFlow Model (нейросеть)
    ↓
Prediction (JSON ответ)
    ↓
AIEventListener (обработка)
    ↓
Действие в Minecraft
```

---

## 📦 Зависимости

### Python
- Flask 2.3.3
- TensorFlow 2.13.0
- NumPy 1.24.3
- Requests 2.31.0

### Java
- Bukkit/Spigot API 1.20
- OkHttp3 4.11.0
- Gson 2.10.1

---

## 🚀 Запуск процесса

```bash
# 1. Обучить модель (опционально)
cd python
python model_trainer.py

# 2. Запустить AI сервер
python ai_server.py

# 3. Собрать Java плагин (в другом окне)
cd ../java
mvn clean package

# 4. Копировать плагин в Minecraft
cp target/ai-mod-1.0.0.jar /path/to/minecraft/plugins/

# 5. Запустить Minecraft сервер
cd /path/to/minecraft
java -Xmx1024M -jar spigot.jar nogui
```

---

## 📊 API Endpoints

```
GET  /health                      Проверка статуса
POST /predict                     Получить предсказание
POST /train                       Обучение модели
GET  /info                        Информация сервера
```

---

## 🎮 Minecraft команды

```
/ai status                        Статус AI
/ai enable                        Включить AI
/ai disable                       Выключить AI
/ai build <type>                  Заказать построение
```

---

**Версия:** 1.0.0  
**Последнее обновление:** 2024-01-01
