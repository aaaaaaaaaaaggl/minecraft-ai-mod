package com.minecraft.ai;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.logging.Logger;

/**
 * Слушатель команд в чате
 * Обрабатывает сообщения игроков и отправляет их AI
 */
public class ChatCommandListener implements Listener {
    
    private static final Logger LOGGER = Logger.getLogger("ChatCommandListener");
    private final JavaPlugin plugin;
    private final AIApiClient apiClient;
    private final Gson gson;
    
    public ChatCommandListener(JavaPlugin plugin, AIApiClient apiClient) {
        this.plugin = plugin;
        this.apiClient = apiClient;
        this.gson = new Gson();
    }
    
    /**
     * Обработать сообщение в чате
     */
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        
        // Проверить, является ли это командой AI
        if (message.startsWith("ai ") || message.startsWith("AI ") || 
            message.startsWith("строй ") || message.startsWith("СТРОЙ ") ||
            message.startsWith("призови ") || message.startsWith("ПРИЗОВИ ") ||
            message.startsWith("генерируй ") || message.startsWith("ГЕНЕРИРУЙ ") ||
            message.startsWith("помощь") || message.startsWith("ПОМОЩЬ")) {
            
            // Убрать префикс если есть
            String command = message;
            if (message.toLowerCase().startsWith("ai ")) {
                command = message.substring(3);
            }
            
            // Отправить на обработку асинхронно
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                processAIChatCommand(player, command);
            });
        }
    }
    
    /**
     * Обработать команду AI
     */
    private void processAIChatCommand(Player player, String command) {
        try {
            // Создать JSON запрос
            JsonObject json = new JsonObject();
            json.addProperty("player_name", player.getName());
            json.addProperty("message", command);
            json.addProperty("x", player.getLocation().getX());
            json.addProperty("y", player.getLocation().getY());
            json.addProperty("z", player.getLocation().getZ());
            
            // Отправить на AI сервер
            AIApiClient.ChatCommandResponse response = apiClient.processChatCommand(json);
            
            if (response != null) {
                if (response.success) {
                    // Отправить ответ игроку
                    String message = response.message;
                    if (message != null && !message.isEmpty()) {
                        player.sendMessage(message);
                    }
                    
                    // Выполнить действие если нужно
                    executeAction(player, response);
                    
                    LOGGER.info("✅ Команда выполнена для " + player.getName() + ": " + response.action);
                } else {
                    player.sendMessage(response.message != null ? response.message : 
                                     "§c❌ Ошибка обработки команды");
                    LOGGER.warning("⚠️  Ошибка для " + player.getName() + ": " + response.message);
                }
            } else {
                player.sendMessage("§cОшибка подключения к AI серверу");
                LOGGER.warning("❌ Не удалось получить ответ от AI сервера");
            }
            
        } catch (Exception e) {
            player.sendMessage("§c⚠️  Ошибка обработки команды: " + e.getMessage());
            LOGGER.warning("❌ Ошибка обработки команды: " + e.getMessage());
        }
    }
    
    /**
     * Выполнить действие на основе ответа AI
     */
    private void executeAction(Player player, AIApiClient.ChatCommandResponse response) {
        if (response.action == null) return;
        
        switch (response.action) {
            case "build_structure":
                executeBuild(player, response);
                break;
            case "spawn_mob":
                executeSpawn(player, response);
                break;
            case "generate_ore":
                executeGenerate(player, response);
                break;
            case "help":
                executeHelp(player, response);
                break;
            case "status":
                executeStatus(player, response);
                break;
            default:
                break;
        }
    }
    
    /**
     * Выполнить построение
     */
    private void executeBuild(Player player, AIApiClient.ChatCommandResponse response) {
        try {
            String structureType = (String) response.getProperty("structure_type");
            Integer height = ((Number) response.getProperty("height")).intValue();
            
            player.sendMessage("§a🏗️  Начинаю строительство структуры: " + structureType);
            
            // Здесь можно добавить логику построения
            // Например, использовать WorldEdit или собственную логику
            
            LOGGER.info(player.getName() + " заказал построение: " + structureType + " высотой " + height);
            
        } catch (Exception e) {
            LOGGER.warning("Ошибка при построении: " + e.getMessage());
        }
    }
    
    /**
     * Выполнить призыв моба
     */
    private void executeSpawn(Player player, AIApiClient.ChatCommandResponse response) {
        try {
            String mobType = (String) response.getProperty("mob_type");
            Integer count = ((Number) response.getProperty("count")).intValue();
            
            player.sendMessage("§a👹 Призываю " + count + " " + mobType + "...");
            
            // Здесь можно добавить логику призыва мобов
            
            LOGGER.info(player.getName() + " заказал призыв: " + count + "x " + mobType);
            
        } catch (Exception e) {
            LOGGER.warning("Ошибка при призыве моба: " + e.getMessage());
        }
    }
    
    /**
     * Выполнить генерацию руды
     */
    private void executeGenerate(Player player, AIApiClient.ChatCommandResponse response) {
        try {
            String oreType = (String) response.getProperty("ore_type");
            Integer veinSize = ((Number) response.getProperty("vein_size")).intValue();
            
            player.sendMessage("§a⛏️  Генерирую жилу " + oreType + " размером " + veinSize + "...");
            
            // Здесь можно добавить логику генерации руды
            
            LOGGER.info(player.getName() + " заказал генерацию: " + oreType + " (" + veinSize + " блоков)");
            
        } catch (Exception e) {
            LOGGER.warning("Ошибка при генерации руды: " + e.getMessage());
        }
    }
    
    /**
     * Выполнить команду помощи
     */
    private void executeHelp(Player player, AIApiClient.ChatCommandResponse response) {
        String message = response.message;
        if (message != null) {
            String[] lines = message.split("\\n");
            for (String line : lines) {
                if (!line.trim().isEmpty()) {
                    player.sendMessage(line);
                }
            }
        }
    }
    
    /**
     * Выполнить команду статуса
     */
    private void executeStatus(Player player, AIApiClient.ChatCommandResponse response) {
        String message = response.message;
        if (message != null) {
            player.sendMessage("§b" + message);
        }
    }
}
