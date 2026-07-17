package com.minecraft.ai;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Основной класс Minecraft AI плагина
 */
public class MinecraftAIPlugin extends JavaPlugin {

    private AIApiClient_Updated apiClient;
    private ChatCommandListener chatListener;

    @Override
    public void onEnable() {
        // Сохранить конфигурацию по умолчанию
        saveDefaultConfig();

        // Получить URL AI сервера из конфига
        String serverUrl = getConfig().getString("ai-server.url", "http://localhost:5000");

        // Инициализировать API клиент
        apiClient = new AIApiClient_Updated(serverUrl, this);

        // Зарегистрировать слушатель чата
        chatListener = new ChatCommandListener(this, apiClient);
        getServer().getPluginManager().registerEvents(chatListener, this);

        // Зарегистрировать команду /ai
        AICommand aiCommand = new AICommand(this, apiClient);
        getCommand("ai").setExecutor(aiCommand);

        getLogger().info("Minecraft AI плагин включен! Сервер: " + serverUrl);
    }

    @Override
    public void onDisable() {
        getLogger().info("Minecraft AI плагин выключен.");
    }
}
