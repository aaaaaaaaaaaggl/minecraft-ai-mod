package com.minecraft.ai;

import org.bukkit.plugin.java.JavaPlugin;
import java.util.logging.Logger;

/**
 * Главный класс плагина Minecraft AI
 */
public class MinecraftAIPlugin extends JavaPlugin {
    
    private static final Logger LOGGER = Logger.getLogger("MinecraftAI");
    private AIApiClient apiClient;
    private ActionExecutor actionExecutor;
    private AIPlayerManager aiPlayerManager;
    private ChatCommandListener chatListener;
    
    @Override
    public void onEnable() {
        LOGGER.info("✅ Minecraft AI плагин загружается...");
        
        // Сохранить конфиг по умолчанию
        saveDefaultConfig();
        
        // Получить URL AI сервера из конфига
        String apiUrl = getConfig().getString("ai-server.url", "http://localhost:5000");
        int timeout = getConfig().getInt("ai-server.timeout", 5);
        
        LOGGER.info("🔗 AI сервер URL: " + apiUrl);
        LOGGER.info("⏱️  Timeout: " + timeout + " сек");
        
        // Создать API клиент, исполнитель действий и менеджер AI-игрока
        apiClient = new AIApiClient(apiUrl, this);
        actionExecutor = new ActionExecutor(this);
        aiPlayerManager = AIPlayerManager.getInstance(this);
        
        // Проверить подключение к серверу
        if (apiClient.checkHealth()) {
            LOGGER.info("✅ Подключение к AI серверу успешно!");
        } else {
            LOGGER.warning("⚠️  Не удалось подключиться к AI серверу!");
            LOGGER.warning("   Убедись, что Python AI сервер запущен на " + apiUrl);
        }
        
        // Зарегистрировать слушателя событий
        chatListener = new ChatCommandListener(this, apiClient, actionExecutor, aiPlayerManager);
        getServer().getPluginManager().registerEvents(chatListener, this);
        
        // Зарегистрировать команды
        registerCommands();
        
        LOGGER.info("✅ Minecraft AI плагин успешно загружен!");
    }
    
    @Override
    public void onDisable() {
        LOGGER.info("❌ Minecraft AI плагин отключается...");
        if (aiPlayerManager != null) {
            aiPlayerManager.shutdown();
        }
        LOGGER.info("❌ Minecraft AI плагин выгружен");
    }
    
    /**
     * Зарегистрировать команды плагина
     */
    private void registerCommands() {
        if (getCommand("ai") != null) {
            getCommand("ai").setExecutor((sender, cmd, label, args) -> {
                if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
                    sender.sendMessage("§6=== Minecraft AI Команды ===");
                    sender.sendMessage("§a/ai enable §f- включить AI");
                    sender.sendMessage("§a/ai disable §f- отключить AI");
                    sender.sendMessage("§a/ai status §f- статус AI");
                    sender.sendMessage("§a/ai help §f- эта справка");
                    return true;
                }
                
                if (args[0].equalsIgnoreCase("enable")) {
                    sender.sendMessage("§a✅ AI включен!");
                    return true;
                }
                
                if (args[0].equalsIgnoreCase("disable")) {
                    sender.sendMessage("§c❌ AI отключен!");
                    return true;
                }
                
                if (args[0].equalsIgnoreCase("status")) {
                    if (apiClient.checkHealth()) {
                        sender.sendMessage("§a✅ AI сервер: ОНЛАЙН");
                    } else {
                        sender.sendMessage("§c❌ AI сервер: ОФФЛАЙН");
                    }
                    return true;
                }
                
                return false;
            });
        }

        if (getCommand("ai_bot") != null) {
            getCommand("ai_bot").setExecutor((sender, cmd, label, args) -> {
                if (args.length == 0) {
                    sender.sendMessage("§b🤖 AI Bot команды§r");
                    sender.sendMessage("§eИспользование: /ai_bot <combat|fight>");
                    return true;
                }

                if (!(sender instanceof org.bukkit.entity.Player)) {
                    sender.sendMessage("§cЭта команда доступна только игрокам.");
                    return true;
                }

                org.bukkit.entity.Player player = (org.bukkit.entity.Player) sender;
                String sub = args[0].toLowerCase();

                if (sub.equals("combat") || sub.equals("fight")) {
                    actionExecutor.startCombat(player);
                    return true;
                }

                sender.sendMessage("§cНеизвестная команда: " + args[0]);
                sender.sendMessage("§eИспользование: /ai_bot <combat|fight>");
                return true;
            });
        }
    }
    
    /**
     * Получить API клиент
     */
    public AIApiClient getApiClient() {
        return apiClient;
    }

    /**
     * Получить исполнитель действий
     */
    public ActionExecutor getActionExecutor() {
        return actionExecutor;
    }

    /**
     * Получить менеджер AI-игрока
     */
    public AIPlayerManager getAIPlayerManager() {
        return aiPlayerManager;
    }
}
