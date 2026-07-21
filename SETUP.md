# 🚀 Инструкция по установке Minecraft AI Mod

## Требования

| Инструмент | Минимальная версия | Назначение |
|---|---|---|
| Java JDK | 11+ | Компиляция и запуск плагина |
| Apache Maven | 3.8+ | Сборка Java проекта |
| Python | 3.8+ | AI сервер |
| Minecraft (Spigot/Paper) | 1.19+ | Игровой сервер |

---

## 1️⃣ Установка JDK

### Windows

1. Скачайте JDK 11+ с [https://adoptium.net/](https://adoptium.net/) или [https://www.oracle.com/java/technologies/downloads/](https://www.oracle.com/java/technologies/downloads/)
2. Запустите установщик и следуйте инструкциям
3. Проверьте установку:

```cmd
java -version
javac -version
```

### Linux (Ubuntu/Debian)

```bash
sudo apt update
sudo apt install openjdk-17-jdk
java -version
```

### macOS

```bash
brew install openjdk@17
java -version
```

---

## 2️⃣ Установка Apache Maven

### Windows

1. Скачайте Maven с [https://maven.apache.org/download.cgi](https://maven.apache.org/download.cgi) (выберите `Binary zip archive`)
2. Распакуйте в `C:\Program Files\Maven\`
3. Добавьте `C:\Program Files\Maven\bin` в системную переменную `PATH`
4. Проверьте:

```cmd
mvn -version
```

### Linux (Ubuntu/Debian)

```bash
sudo apt install maven
mvn -version
```

### macOS

```bash
brew install maven
mvn -version
```

---

## 3️⃣ Установка Python AI Server

### Шаг 1: Установка зависимостей

```bash
cd python
pip install -r requirements.txt
```

Основные Python пакеты (`requirements.txt`):
- `Flask` — HTTP сервер для AI API
- `tensorflow` — нейросетевая модель
- `numpy` — математические вычисления

### Шаг 2: Обучение модели (опционально)

```bash
python model_trainer.py
```

Это создаст файл `model.h5` с обученной моделью.

### Шаг 3: Запуск AI сервера

```bash
python ai_server_updated2.0.py
```

Вы должны увидеть:
```
🚀 Запуск AI Server для Minecraft...
📡 Адрес: http://0.0.0.0:5000
✅ Модель загружена: model.h5
✅ Сервер готов!
```

---

## 4️⃣ Загрузка зависимостей Maven

Maven автоматически скачает все Java зависимости при первой сборке. Зависимости проекта:

| Артефакт | Версия | Откуда скачивается |
|---|---|---|
| `spigot-api` | `1.20.1-R0.1-SNAPSHOT` | https://hub.spigotmc.org/nexus/ |
| `okhttp` | `4.11.0` | Maven Central |
| `gson` | `2.10.1` | Maven Central |

Репозитории уже прописаны в `java/pom.xml`. Просто запустите сборку — Maven скачает всё сам:

```bash
cd java
mvn dependency:resolve
```

---

## 5️⃣ Сборка Java плагина

```bash
cd java
mvn clean package
```

После успешной сборки JAR файл будет создан в `java/target/ai-mod-1.0.0.jar`

Вывод при успехе:
```
[INFO] BUILD SUCCESS
[INFO] Total time: X.XXX s
```

---

## 6️⃣ Установка плагина на сервер Minecraft

### Шаг 1: Скачайте Spigot/Paper сервер

**Spigot** (нужен BuildTools):
```bash
# Скачайте BuildTools.jar с https://hub.spigotmc.org/jenkins/job/BuildTools/
java -jar BuildTools.jar --rev 1.20.1
```

**Paper** (рекомендуется, проще):
- Скачайте готовый JAR с [https://papermc.io/downloads](https://papermc.io/downloads)

### Шаг 2: Установите плагин

Скопируйте JAR в папку `plugins` сервера:

```bash
cp java/target/ai-mod-1.0.0.jar /path/to/minecraft-server/plugins/
```

### Шаг 3: Запустите сервер

```bash
cd /path/to/minecraft-server
java -Xmx2G -Xms1G -jar paper.jar nogui
```

### Проверка загрузки плагина

В логах сервера вы должны увидеть:
```
[12:34:56] [Server thread/INFO]: [Minecraft AI Plugin] ✅ Плагин успешно загружен!
[12:34:57] [Server thread/INFO]: ✅ Подключение к AI серверу успешно!
```

---

## 7️⃣ Использование

### Команды в игре:

```
/ai status        - Статус AI сервера
/ai enable        - Включить AI
/ai disable       - Выключить AI
/ai build <type>  - Заказать построение (house / tower / mansion / bridge)
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

### Проблема: "Cannot compile Java" / "BUILD FAILURE"

**Решение:** Проверьте версию Java и Maven:
```bash
java -version   # требуется 11+
mvn -version    # требуется 3.8+
```

Если Maven не может скачать зависимости — проверьте интернет-соединение и репозиторий:
```bash
mvn dependency:resolve -U
```

### Проблема: "org.bukkit.block.UPPER / LOWER not found"

**Решение:** Убедитесь, что используется `Bisected.Half.valueOf("UPPER")` вместо `Bisected.Half.UPPER` в исходном коде. В данном проекте это уже исправлено в методе `placeDoor()` файла `ActionExecutor.java`.

---

## 📚 Дополнительная информация

- Документация TensorFlow: https://www.tensorflow.org/
- Документация Spigot API: https://hub.spigotmc.org/javadocs/spigot/
- Документация Paper: https://docs.papermc.io/
- REST API примеры в папке `examples/`

---

## ✅ Готово!

Теперь у вас работает Minecraft AI Mod! 🎮🤖
