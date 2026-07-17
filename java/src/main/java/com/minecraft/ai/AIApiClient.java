package com.minecraft.ai;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * API клиент для подключения к Python AI серверу
 */
public class AIApiClient {
    
    private static final Logger LOGGER = Logger.getLogger("AIApiClient");
    private final String apiUrl;
    private final OkHttpClient httpClient;
    private final JavaPlugin plugin;
    
    public AIApiClient(String apiUrl, JavaPlugin plugin) {
        this.apiUrl = apiUrl;
        this.plugin = plugin;
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .build();
    }
    
    /**
     * Проверить доступность сервера
     */
    public boolean checkHealth() {
        try {
            Request request = new Request.Builder()
                .url(apiUrl + "/health")
                .build();
            
            Response response = httpClient.newCall(request).execute();
            boolean success = response.isSuccessful();
            response.close();
            return success;
        } catch (IOException e) {
            LOGGER.warning("❌ Не удалось подключиться к AI серверу: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Обработать команду чата через AI
     */
    public ChatCommandResponse processChatCommand(JsonObject json) {
        try {
            RequestBody body = RequestBody.create(
                json.toString(),
                MediaType.get("application/json; charset=utf-8")
            );
            
            Request request = new Request.Builder()
                .url(apiUrl + "/chat")
                .post(body)
                .build();
            
            Response response = httpClient.newCall(request).execute();
            
            if (response.isSuccessful()) {
                String responseBody = response.body().string();
                JsonObject responseJson = JsonParser.parseString(responseBody).getAsJsonObject();
                response.close();
                
                return new ChatCommandResponse(responseJson);
            } else {
                LOGGER.warning("❌ AI сервер вернул ошибку: " + response.code());
                response.close();
                return null;
            }
        } catch (IOException e) {
            LOGGER.warning("❌ Ошибка при отправке команды: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Отправить сообщение к AI серверу
     */
    public String sendMessage(String message) {
        try {
            JsonObject json = new JsonObject();
            json.addProperty("message", message);
            json.addProperty("player", "Minecraft");
            
            RequestBody body = RequestBody.create(
                json.toString(),
                MediaType.get("application/json; charset=utf-8")
            );
            
            Request request = new Request.Builder()
                .url(apiUrl + "/message")
                .post(body)
                .build();
            
            Response response = httpClient.newCall(request).execute();
            
            if (response.isSuccessful()) {
                String responseBody = response.body().string();
                JsonObject responseJson = JsonParser.parseString(responseBody).getAsJsonObject();
                response.close();
                return responseJson.get("response").getAsString();
            } else {
                LOGGER.warning("❌ AI сервер вернул ошибку: " + response.code());
                response.close();
                return null;
            }
        } catch (IOException e) {
            LOGGER.warning("❌ Ошибка при отправке сообщения: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Выполнить команду через AI
     */
    public boolean executeCommand(String command) {
        try {
            JsonObject json = new JsonObject();
            json.addProperty("command", command);
            
            RequestBody body = RequestBody.create(
                json.toString(),
                MediaType.get("application/json; charset=utf-8")
            );
            
            Request request = new Request.Builder()
                .url(apiUrl + "/execute")
                .post(body)
                .build();
            
            Response response = httpClient.newCall(request).execute();
            boolean success = response.isSuccessful();
            response.close();
            return success;
        } catch (IOException e) {
            LOGGER.warning("❌ Ошибка при выполнении команды: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Отправить сообщение в чат
     */
    public boolean broadcastMessage(String message, String type) {
        try {
            JsonObject json = new JsonObject();
            json.addProperty("message", message);
            json.addProperty("type", type);
            
            RequestBody body = RequestBody.create(
                json.toString(),
                MediaType.get("application/json; charset=utf-8")
            );
            
            Request request = new Request.Builder()
                .url(apiUrl + "/broadcast")
                .post(body)
                .build();
            
            Response response = httpClient.newCall(request).execute();
            boolean success = response.isSuccessful();
            response.close();
            return success;
        } catch (IOException e) {
            LOGGER.warning("❌ Ошибка при отправке сообщения в чат: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Внутренний класс для ответа на команду чата
     */
    public static class ChatCommandResponse {
        public boolean success;
        public String message;
        public String action;
        private Map<String, Object> properties;
        
        public ChatCommandResponse(JsonObject json) {
            this.success = json.has("success") ? json.get("success").getAsBoolean() : false;
            this.message = json.has("message") ? json.get("message").getAsString() : null;
            this.action = json.has("action") ? json.get("action").getAsString() : null;
            this.properties = new HashMap<>();
            
            // Парсим дополнительные свойства
            if (json.has("data")) {
                JsonObject data = json.getAsJsonObject("data");
                data.keySet().forEach(key -> {
                    properties.put(key, data.get(key));
                });
            }
        }
        
        /**
         * Получить свойство из ответа
         */
        public Object getProperty(String key) {
            if (properties.containsKey(key)) {
                Object obj = properties.get(key);
                if (obj instanceof com.google.gson.JsonElement) {
                    com.google.gson.JsonElement elem = (com.google.gson.JsonElement) obj;
                    if (elem.isJsonPrimitive()) {
                        com.google.gson.JsonPrimitive prim = elem.getAsJsonPrimitive();
                        if (prim.isString()) return prim.getAsString();
                        if (prim.isNumber()) return prim.getAsNumber();
                        if (prim.isBoolean()) return prim.getAsBoolean();
                    }
                }
                return obj;
            }
            return null;
        }
    }
}
