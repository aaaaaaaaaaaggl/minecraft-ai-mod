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
    private final ActionExecutor actionExecutor;
    private final AIPlayer aiPlayer;
    private final Gson gson;

    public ChatCommandListener(JavaPlugin plugin, AIApiClient apiClient,
                               ActionExecutor actionExecutor, AIPlayer aiPlayer) {
        this.plugin = plugin;
        this.apiClient = apiClient;
        this.actionExecutor = actionExecutor;
        this.aiPlayer = aiPlayer;
        this.gson = new Gson();
    }

    /**
     * Обработать сообщение в чате
     */
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();

        // Проверить, является ли это командой AI (требуется префикс "ai ")
        if (message.toLowerCase().startsWith("ai ")) {

            // Убрать префикс "ai "
            final String command = message.substring(3);

            // Отправить на обработку асинхронно
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                processAIChatCommand(player, command);
            });
        }
    }

    /**
     * Обработать команду AI (выполняется в async thread)
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
                    // Показать ответное сообщение игроку
                    if (response.message != null && !response.message.isEmpty()) {
                        player.sendMessage(response.message);
                    }

                    // Выполнить действие на главном потоке сервера
                    executeAction(player, response);

                    LOGGER.info("✅ Команда выполнена для " + player.getName()
                            + ": " + response.action);
                } else {
                    player.sendMessage(response.message != null ? response.message
                            : "§c❌ Ошибка обработки команды");
                    LOGGER.warning("⚠️  Ошибка для " + player.getName()
                            + ": " + response.message);
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
     * Диспетчер действий — перенаправляет на главный поток для Bukkit-операций
     */
    private void executeAction(Player player, AIApiClient.ChatCommandResponse response) {
        if (response.action == null) return;

        switch (response.action) {
            case "build_structure":
                Bukkit.getScheduler().runTask(plugin,
                        () -> executeBuild(player, response));
                break;
            case "spawn_mob":
                Bukkit.getScheduler().runTask(plugin,
                        () -> executeSpawn(player, response));
                break;
            case "generate_ore":
                Bukkit.getScheduler().runTask(plugin,
                        () -> executeGenerate(player, response));
                break;
            case "ai_join":
                Bukkit.getScheduler().runTask(plugin, () -> aiPlayer.join(player));
                break;
            case "ai_leave":
                Bukkit.getScheduler().runTask(plugin, () -> aiPlayer.leave());
                break;
            case "mine_blocks":
                Bukkit.getScheduler().runTask(plugin,
                        () -> executeMine(player, response));
                break;
            case "follow_player":
                Bukkit.getScheduler().runTask(plugin, () -> aiPlayer.follow(player));
                break;
            case "stop_follow":
                Bukkit.getScheduler().runTask(plugin, () -> aiPlayer.stopFollow(player));
                break;
            case "gather_blocks":
                Bukkit.getScheduler().runTask(plugin,
                        () -> executeGather(player, response));
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
     * Выполнить построение (главный поток)
     */
    private void executeBuild(Player player, AIApiClient.ChatCommandResponse response) {
        try {
            String structureType = getStringProperty(response, "structure_type", "house");
            actionExecutor.buildStructure(player, structureType);
        } catch (Exception e) {
            player.sendMessage("§c❌ Ошибка при построении: " + e.getMessage());
            LOGGER.warning("Ошибка при построении: " + e.getMessage());
        }
    }

    /**
     * Выполнить призыв моба (главный поток)
     */
    private void executeSpawn(Player player, AIApiClient.ChatCommandResponse response) {
        try {
            String mobType = getStringProperty(response, "mob_type", "zombie");
            int count = getIntProperty(response, "count", 3);
            actionExecutor.spawnMob(player, mobType, count);
        } catch (Exception e) {
            player.sendMessage("§c❌ Ошибка при призыве моба: " + e.getMessage());
            LOGGER.warning("Ошибка при призыве моба: " + e.getMessage());
        }
    }

    /**
     * Выполнить генерацию руды (главный поток)
     */
    private void executeGenerate(Player player, AIApiClient.ChatCommandResponse response) {
        try {
            String oreType = getStringProperty(response, "ore_type", "iron");
            int veinSize = getIntProperty(response, "vein_size", 8);
            actionExecutor.generateOre(player, oreType, veinSize);
        } catch (Exception e) {
            player.sendMessage("§c❌ Ошибка при генерации руды: " + e.getMessage());
            LOGGER.warning("Ошибка при генерации руды: " + e.getMessage());
        }
    }

    /**
     * Выполнить добычу блоков AI игроком (главный поток)
     */
    private void executeMine(Player player, AIApiClient.ChatCommandResponse response) {
        try {
            int radius = getIntProperty(response, "radius", 5);
            aiPlayer.mine(player, radius);
        } catch (Exception e) {
            player.sendMessage("§c❌ Ошибка при добыче: " + e.getMessage());
            LOGGER.warning("Ошибка при добыче: " + e.getMessage());
        }
    }

    /**
     * Выполнить сбор блоков AI игроком (главный поток)
     */
    private void executeGather(Player player, AIApiClient.ChatCommandResponse response) {
        try {
            String blockType = getStringProperty(response, "block_type", "stone");
            int radius = getIntProperty(response, "radius", 8);
            aiPlayer.gather(player, blockType, radius);
        } catch (Exception e) {
            player.sendMessage("§c❌ Ошибка при сборе: " + e.getMessage());
            LOGGER.warning("Ошибка при сборе: " + e.getMessage());
        }
    }

    /**
     * Вывести подсказку
     */
    private void executeHelp(Player player, AIApiClient.ChatCommandResponse response) {
        String message = response.message;
        if (message != null) {
            for (String line : message.split("\\n")) {
                if (!line.trim().isEmpty()) {
                    player.sendMessage(line);
                }
            }
        }
    }

    /**
     * Вывести статус
     */
    private void executeStatus(Player player, AIApiClient.ChatCommandResponse response) {
        String message = response.message;
        if (message != null) {
            player.sendMessage("§b" + message);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String getStringProperty(AIApiClient.ChatCommandResponse response,
                                     String key, String defaultValue) {
        Object value = response.getProperty(key);
        return (value != null) ? value.toString() : defaultValue;
    }

    private int getIntProperty(AIApiClient.ChatCommandResponse response,
                               String key, int defaultValue) {
        Object value = response.getProperty(key);
        if (value instanceof Number) return ((Number) value).intValue();
        if (value != null) {
            try { return Integer.parseInt(value.toString()); } catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }
}
