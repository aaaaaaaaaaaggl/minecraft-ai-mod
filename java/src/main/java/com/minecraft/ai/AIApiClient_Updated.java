package com.minecraft.ai;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Обновленный клиент для взаимодействия с AI REST API
 * Включает обработку команд чата
 */
public class AIApiClient_Updated {
    
    private static final Logger LOGGER = Logger.getLogger("AIApiClient");
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    
    private final String baseUrl;
    private final OkHttpClient client;
    private final Gson gson;
    
    public AIApiClient_Updated(String baseUrl, JavaPlugin plugin) {
        this.baseUrl = baseUrl;
        this.gson = new Gson();
        
        this.client = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .writeTimeout(5, TimeUnit.SECONDS)
                .build();
    }
    
    /**
     * Проверить здоровье сервера
     */
    public boolean checkHealth() throws Exception {
        String url = baseUrl + "/health";
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        
        try (Response response = client.newCall(request).execute()) {
            return response.isSuccessful();
        }
    }
    
    /**
     * Обработать команду из чата
     */
    public ChatCommandResponse processChatCommand(JsonObject commandData) {
        try {
            String url = baseUrl + "/chat/command";
            
            RequestBody body = RequestBody.create(commandData.toString(), JSON);
            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    LOGGER.warning("API ошибка: " + response.code() + " " + response.message());
                    return null;
                }
                
                String responseBody = response.body().string();
                return gson.fromJson(responseBody, ChatCommandResponse.class);
            }
            
        } catch (Exception e) {
            LOGGER.warning("Ошибка обработки команды: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Отправить сообщение в чат всем
     */
    public boolean broadcastMessage(String message, String type) {
        try {
            String url = baseUrl + "/chat/broadcast";
            
            JsonObject json = new JsonObject();
            json.addProperty("message", message);
            json.addProperty("type", type);
            
            RequestBody body = RequestBody.create(json.toString(), JSON);
            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();
            
            try (Response response = client.newCall(request).execute()) {
                return response.isSuccessful();
            }
            
        } catch (Exception e) {
            LOGGER.warning("Ошибка трансляции: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Получить предсказание AI
     */
    public AIPrediction predict(double x, double y, double z, String blockType) {
        try {
            String url = baseUrl + "/predict";
            
            JsonObject json = new JsonObject();
            json.addProperty("x", x);
            json.addProperty("y", y);
            json.addProperty("z", z);
            json.addProperty("block_type", blockType);
            
            RequestBody body = RequestBody.create(json.toString(), JSON);
            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    LOGGER.warning("API ошибка: " + response.code());
                    return null;
                }
                
                String responseBody = response.body().string();
                return gson.fromJson(responseBody, AIPrediction.class);
            }
            
        } catch (Exception e) {
            LOGGER.warning("Ошибка предсказания: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Класс для ответа на команду чата
     */
    public static class ChatCommandResponse {
        public String action;
        public boolean success;
        public String message;
        public String player_name;
        public String structure_type;
        public int height;
        public String mob_type;
        public int count;
        public String ore_type;
        public int vein_size;
        
        public Object getProperty(String name) {
            try {
                return this.getClass().getDeclaredField(name).get(this);
            } catch (IllegalAccessException | NoSuchFieldException e) {
                return null;
            }
        }
    }
    
    /**
     * Класс для предсказания AI
     */
    public static class AIPrediction {
        public String action;
        public double confidence;
        public double x;
        public double y;
        public double z;
        public String timestamp;
        public String structure_type;
        public int height;
        public String mob_type;
        public int count;
        public String ore_type;
        public int vein_size;
    }
}
