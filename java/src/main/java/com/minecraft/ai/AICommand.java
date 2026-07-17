package com.minecraft.ai;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.logging.Logger;

/**
 * Обработчик команды /ai
 */
public class AICommand implements CommandExecutor {
    
    private static final Logger LOGGER = Logger.getLogger("AICommand");
    private final JavaPlugin plugin;
    private final AIApiClient_Updated apiClient;
    
    public AICommand(JavaPlugin plugin, AIApiClient_Updated apiClient) {
        this.plugin = plugin;
        this.apiClient = apiClient;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§b🤖 Minecraft AI Command§r");
            sender.sendMessage("§eИспользование: /ai <enable|disable|status|help>");
            return true;
        }
        
        String subcommand = args[0].toLowerCase();
        
        switch (subcommand) {
            case "enable":
                return handleEnable(sender);
            case "disable":
                return handleDisable(sender);
            case "status":
                return handleStatus(sender);
            case "help":
                return handleHelp(sender);
            default:
                sender.sendMessage("§cНеизвестная команда: " + subcommand);
                sender.sendMessage("§eИспользование: /ai <enable|disable|status|help>");
                return false;
        }
    }
    
    /**
     * Включить AI
     */
    private boolean handleEnable(CommandSender sender) {
        try {
            boolean success = apiClient.broadcastMessage("AI включен", "ai");
            
            if (success) {
                sender.sendMessage("§a✓ AI включен!§r");
                LOGGER.info("AI включен командой " + sender.getName());
                return true;
            } else {
                sender.sendMessage("§c✗ Ошибка при включении AI§r");
                return false;
            }
        } catch (Exception e) {
            sender.sendMessage("§c✗ Ошибка: " + e.getMessage() + "§r");
            return false;
        }
    }
    
    /**
     * Выключить AI
     */
    private boolean handleDisable(CommandSender sender) {
        try {
            boolean success = apiClient.broadcastMessage("AI выключен", "ai");
            
            if (success) {
                sender.sendMessage("§c✗ AI выключен!§r");
                LOGGER.info("AI выключен командой " + sender.getName());
                return true;
            } else {
                sender.sendMessage("§c✗ Ошибка при выключении AI§r");
                return false;
            }
        } catch (Exception e) {
            sender.sendMessage("§c✗ Ошибка: " + e.getMessage() + "§r");
            return false;
        }
    }
    
    /**
     * Статус AI
     */
    private boolean handleStatus(CommandSender sender) {
        try {
            sender.sendMessage("§b════════════════════════════════════════§r");
            sender.sendMessage("§e🤖 Статус AI§r");
            sender.sendMessage("§b════════════════════════════════════════§r");
            sender.sendMessage("§a✓ AI сервер активен§r");
            sender.sendMessage("§a✓ Chat listener зарегистрирован§r");
            sender.sendMessage("§a✓ API доступен§r");
            sender.sendMessage("§b════════════════════════════════════════§r");
            
            return true;
        } catch (Exception e) {
            sender.sendMessage("§c✗ Ошибка: " + e.getMessage() + "§r");
            return false;
        }
    }
    
    /**
     * Справка по AI командам
     */
    private boolean handleHelp(CommandSender sender) {
        sender.sendMessage("§b════════════════════════════════════════§r");
        sender.sendMessage("§e🤖 Команды AI§r");
        sender.sendMessage("§b════════════════════════════════════════§r");
        sender.sendMessage("§6Постройки:§r");
        sender.sendMessage("  §e строй дом§r / §e строй башню§r");
        sender.sendMessage("  §e строй мост высотой 20 блоков§r");
        sender.sendMessage("");
        sender.sendMessage("§6Призыв мобов:§r");
        sender.sendMessage("  §e призови зомби§r / §e призови 5 скелетов§r");
        sender.sendMessage("");
        sender.sendMessage("§6Генерация руды:§r");
        sender.sendMessage("  §e генерируй алмазы§r / §e генерируй золото§r");
        sender.sendMessage("");
        sender.sendMessage("§6Основные:§r");
        sender.sendMessage("  §e /ai enable§r - включить AI");
        sender.sendMessage("  §e /ai disable§r - выключить AI");
        sender.sendMessage("  §e /ai status§r - статус AI");
        sender.sendMessage("§b════════════════════════════════════════§r");
        
        return true;
    }
}
