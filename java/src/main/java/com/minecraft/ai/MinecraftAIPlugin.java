package com.minecraft.ai;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.logging.Logger;

/**
 * Главный класс плагина Minecraft AI
 */
public class MinecraftAIPlugin extends JavaPlugin {

    private static final Logger LOGGER = Logger.getLogger("MinecraftAI");
    private AIApiClient apiClient;
    private ActionExecutor actionExecutor;
    private AIPlayer aiPlayer;
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

        // Создать API клиент, исполнитель действий и AI игрока
        apiClient = new AIApiClient(apiUrl, this);
        actionExecutor = new ActionExecutor(this);
        aiPlayer = new AIPlayer(this, actionExecutor);

        // Проверить подключение к серверу
        if (apiClient.checkHealth()) {
            LOGGER.info("✅ Подключение к AI серверу успешно!");
        } else {
            LOGGER.warning("⚠️  Не удалось подключиться к AI серверу!");
            LOGGER.warning("   Убедись, что Python AI сервер запущен на " + apiUrl);
        }

        // Зарегистрировать слушателя событий
        chatListener = new ChatCommandListener(this, apiClient, actionExecutor, aiPlayer);
        getServer().getPluginManager().registerEvents(chatListener, this);

        // Зарегистрировать команды
        registerCommands();

        LOGGER.info("✅ Minecraft AI плагин успешно загружен!");
    }

    @Override
    public void onDisable() {
        if (aiPlayer != null && aiPlayer.isActive()) {
            aiPlayer.leave();
        }
        LOGGER.info("❌ Minecraft AI плагин выгружен");
    }

    /**
     * Зарегистрировать команды плагина
     */
    private void registerCommands() {
        if (getCommand("ai") == null) return;

        getCommand("ai").setExecutor((sender, cmd, label, args) -> {
            if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
                sender.sendMessage("§6=== Minecraft AI Команды ===");
                sender.sendMessage("§a/ai join §f- AI игрок заходит на сервер");
                sender.sendMessage("§a/ai leave §f- AI игрок уходит с сервера");
                sender.sendMessage("§a/ai build [structure] §f- AI строит (house/tower/bridge/mansion)");
                sender.sendMessage("§a/ai mine [radius] §f- AI добывает блоки");
                sender.sendMessage("§a/ai follow §f- AI следует за тобой");
                sender.sendMessage("§a/ai stop §f- AI останавливается");
                sender.sendMessage("§a/ai gather [block] §f- AI собирает конкретный блок");
                sender.sendMessage("§a/ai enable §f- включить AI сервер");
                sender.sendMessage("§a/ai disable §f- отключить AI сервер");
                sender.sendMessage("§a/ai status §f- статус AI");
                sender.sendMessage("§a/ai help §f- эта справка");
                sender.sendMessage("§7Или пиши в чате: §eai появись§7, §eai добывай§7, §eai следуй§7 ...");
                return true;
            }

            switch (args[0].toLowerCase()) {
                case "join": {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage("§cТолько игроки могут использовать эту команду!");
                        return true;
                    }
                    Player player = (Player) sender;
                    Bukkit.getScheduler().runTask(this, () -> aiPlayer.join(player));
                    return true;
                }
                case "leave": {
                    Bukkit.getScheduler().runTask(this, () -> aiPlayer.leave());
                    return true;
                }
                case "build": {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage("§cТолько игроки могут использовать эту команду!");
                        return true;
                    }
                    Player player = (Player) sender;
                    String structure = args.length > 1 ? args[1] : "house";
                    Bukkit.getScheduler().runTask(this, () -> aiPlayer.build(player, structure));
                    return true;
                }
                case "mine": {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage("§cТолько игроки могут использовать эту команду!");
                        return true;
                    }
                    Player player = (Player) sender;
                    int radius = 5;
                    if (args.length > 1) {
                        try { radius = Integer.parseInt(args[1]); } catch (NumberFormatException ignored) {}
                    }
                    final int r = radius;
                    Bukkit.getScheduler().runTask(this, () -> aiPlayer.mine(player, r));
                    return true;
                }
                case "follow": {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage("§cТолько игроки могут использовать эту команду!");
                        return true;
                    }
                    Player player = (Player) sender;
                    Bukkit.getScheduler().runTask(this, () -> aiPlayer.follow(player));
                    return true;
                }
                case "stop": {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage("§cТолько игроки могут использовать эту команду!");
                        return true;
                    }
                    Player player = (Player) sender;
                    Bukkit.getScheduler().runTask(this, () -> aiPlayer.stopFollow(player));
                    return true;
                }
                case "gather": {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage("§cТолько игроки могут использовать эту команду!");
                        return true;
                    }
                    Player player = (Player) sender;
                    String blockType = args.length > 1 ? args[1] : "stone";
                    int radius = args.length > 2 ? parseIntSafe(args[2], 8) : 8;
                    Bukkit.getScheduler().runTask(this, () -> aiPlayer.gather(player, blockType, radius));
                    return true;
                }
                case "enable":
                    sender.sendMessage("§a✅ AI включен!");
                    return true;
                case "disable":
                    sender.sendMessage("§c❌ AI отключен!");
                    return true;
                case "status":
                    if (apiClient.checkHealth()) {
                        sender.sendMessage("§a✅ AI сервер: ОНЛАЙН");
                    } else {
                        sender.sendMessage("§c❌ AI сервер: ОФФЛАЙН");
                    }
                    sender.sendMessage("§bAI игрок: " + (aiPlayer.isActive() ? "§aАКТИВЕН" : "§cОТСУТСТВУЕТ"));
                    return true;
                default:
                    sender.sendMessage("§cНеизвестная команда. Используй /ai help");
                    return false;
            }
        });
    }

    private int parseIntSafe(String s, int defaultVal) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return defaultVal; }
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
     * Получить AI игрока
     */
    public AIPlayer getAiPlayer() {
        return aiPlayer;
    }
}
