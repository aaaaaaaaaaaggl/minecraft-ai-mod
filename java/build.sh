#!/bin/bash

# Build script for Minecraft AI Mod

echo "🔨 Компилирование Java файлов..."
mkdir -p target/classes

# Компилируем Java файлы
javac -d target/classes src/main/java/com/minecraft/ai/*.java

if [ $? -eq 0 ]; then
    echo "✅ Компиляция успешна!"
else
    echo "❌ Ошибка при компиляции!"
    exit 1
fi

echo "📦 Копирование ресурсов..."
cp -r src/main/resources/* target/classes/ 2>/dev/null || true

echo "📦 Создание JAR файла..."
cd target
jar cvf ai-mod-1.0.0.jar -C classes .
cd ..

if [ -f "target/ai-mod-1.0.0.jar" ]; then
    echo "✅ JAR файл успешно создан!"
    echo "📍 Файл находится в: java/target/ai-mod-1.0.0.jar"
    ls -lh target/ai-mod-1.0.0.jar
else
    echo "❌ Ошибка при создании JAR!"
    exit 1
fi
