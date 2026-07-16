"""
Обучение простой нейросети для Minecraft AI
Генерирует и сохраняет обученную модель
"""

import numpy as np
import tensorflow as tf
from sklearn.model_selection import train_test_split
import json

# ==================== ГЕНЕРАЦИЯ ДАННЫХ ====================

def generate_training_data(samples=1000):
    """Генерировать синтетические данные для обучения"""
    
    print(f"📊 Генерирование {samples} примеров обучения...")
    
    X = []
    y = []
    
    for _ in range(samples):
        x = np.random.uniform(0, 1)
        y_coord = np.random.uniform(0, 1)
        z = np.random.uniform(0, 1)
        block = np.random.uniform(0, 1)
        
        X.append([x, y_coord, z, block])
        
        if y_coord < 0.3 and block < 0.3:
            target = 2
        elif y_coord > 0.7 and block > 0.6:
            target = 0
        elif 0.3 <= y_coord <= 0.7:
            target = 1
        else:
            target = np.random.randint(0, 3)
        
        one_hot = np.zeros(3)
        one_hot[target] = 1
        y.append(one_hot)
    
    X = np.array(X, dtype=np.float32)
    y = np.array(y, dtype=np.float32)
    
    print(f"✅ Данные сгенерированы: {X.shape}")
    
    return X, y

# ==================== СОЗДАНИЕ И ОБУЧЕНИЕ МОДЕЛИ ====================

def create_model(input_shape=4):
    """Создать архитектуру нейросети"""
    
    model = tf.keras.Sequential([
        tf.keras.layers.Input(shape=(input_shape,)),
        tf.keras.layers.Dense(128, activation='relu', name='dense_1'),
        tf.keras.layers.Dropout(0.2),
        tf.keras.layers.Dense(64, activation='relu', name='dense_2'),
        tf.keras.layers.Dropout(0.2),
        tf.keras.layers.Dense(32, activation='relu', name='dense_3'),
        tf.keras.layers.Dropout(0.1),
        tf.keras.layers.Dense(3, activation='softmax', name='output')
    ])
    
    model.compile(
        optimizer=tf.keras.optimizers.Adam(learning_rate=0.001),
        loss='categorical_crossentropy',
        metrics=['accuracy']
    )
    
    return model

def train_and_save(output_path="model.h5"):
    """Основная функция: обучить и сохранить модель"""
    
    print("🤖 Minecraft AI Model Trainer")
    print("=" * 50)
    
    X, y = generate_training_data(samples=2000)
    
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42
    )
    
    print(f"📚 Тренировочный набор: {X_train.shape[0]}")
    print(f"📚 Тестовый набор: {X_test.shape[0]}")
    
    print("\n🔨 Создание модели...")
    model = create_model()
    model.summary()
    
    print("\n⏳ Обучение модели...")
    history = model.fit(
        X_train, y_train,
        validation_data=(X_test, y_test),
        epochs=50,
        batch_size=32,
        verbose=1,
        callbacks=[
            tf.keras.callbacks.EarlyStopping(
                monitor='val_loss',
                patience=5,
                restore_best_weights=True
            )
        ]
    )
    
    print("\n📊 Оценка модели...")
    test_loss, test_accuracy = model.evaluate(X_test, y_test, verbose=0)
    print(f"✅ Точность на тестовом наборе: {test_accuracy:.4f}")
    print(f"✅ Loss на тестовом наборе: {test_loss:.4f}")
    
    print(f"\n💾 Сохранение модели в {output_path}...")
    model.save(output_path)
    print(f"✅ Модель сохранена!")
    
    history_data = {
        "loss": [float(x) for x in history.history['loss']],
        "accuracy": [float(x) for x in history.history['accuracy']],
        "val_loss": [float(x) for x in history.history['val_loss']],
        "val_accuracy": [float(x) for x in history.history['val_accuracy']],
        "test_accuracy": float(test_accuracy),
        "test_loss": float(test_loss)
    }
    
    with open("training_history.json", "w") as f:
        json.dump(history_data, f, indent=2)
    
    print("✅ История обучения сохранена в training_history.json")
    
    return model

# ==================== ТЕСТИРОВАНИЕ ====================

def test_model(model_path="model.h5"):
    """Протестировать сохраненную модель"""
    
    print("\n🧪 Тестирование модели...")
    
    model = tf.keras.models.load_model(model_path)
    
    test_inputs = [
        [0.1, 0.2, 0.15, 0.1],
        [0.8, 0.9, 0.85, 0.9],
        [0.5, 0.5, 0.5, 0.5],
    ]
    
    action_names = ["build_structure", "spawn_mob", "generate_ore"]
    
    for i, test_input in enumerate(test_inputs):
        prediction = model.predict(np.array([test_input]), verbose=0)
        action_idx = np.argmax(prediction[0])
        confidence = prediction[0][action_idx]
        
        print(f"\nТест {i+1}: {test_input}")
        print(f"  → Действие: {action_names[action_idx]}")
        print(f"  → Уверенность: {confidence:.4f}")

if __name__ == "__main__":
    train_and_save("model.h5")
    test_model("model.h5")
    
    print("\n" + "=" * 50)
    print("✅ Обучение завершено!")
    print("📁 Файлы:")
    print("  - model.h5 (обученная модель)")
    print("  - training_history.json (история обучения)")
    print("\nТеперь запустите ai_server.py для использования API!")
