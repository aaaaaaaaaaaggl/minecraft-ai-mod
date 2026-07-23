package com.minecraft.ai;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;
import java.util.logging.Logger;

/**
 * Handles all click interactions inside the {@link AIBotMenu} inventory.
 *
 * <p>Each interactive slot toggles a setting, adjusts a value, or performs
 * an action (spawn / despawn / reset).  After every mutation the settings
 * are persisted and the affected inventory items are refreshed.
 */
public class MenuListener implements Listener {

    private static final Logger LOGGER = Logger.getLogger("MenuListener");

    private final JavaPlugin       plugin;
    private final AIPlayerManager  aiPlayerManager;

    /** Tracks the open menu per player UUID so we can refresh it. */
    private final java.util.Map<UUID, AIBotMenu> openMenus =
            new java.util.concurrent.ConcurrentHashMap<>();

    public MenuListener(JavaPlugin plugin, AIPlayerManager aiPlayerManager) {
        this.plugin          = plugin;
        this.aiPlayerManager = aiPlayerManager;
    }

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Open the AI Bot menu for {@code player}.
     */
    public void openMenu(Player player) {
        AIBotMenu menu = new AIBotMenu(player);
        openMenus.put(player.getUniqueId(), menu);
        player.openInventory(menu.getInventory());
    }

    // ── Inventory events ───────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof AIBotMenu)) return;

        // Cancel all clicks inside our GUI so items cannot be taken
        event.setCancelled(true);

        if (event.getCurrentItem() == null) return;

        Player    player = (Player) event.getWhoClicked();
        AIBotMenu menu   = (AIBotMenu) holder;
        int       slot   = event.getRawSlot();
        boolean   right  = event.isRightClick();

        handleSlot(player, menu, slot, right);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof AIBotMenu)) return;

        Player player = (Player) event.getPlayer();
        openMenus.remove(player.getUniqueId());
    }

    // ── Slot dispatch ──────────────────────────────────────────────────────────

    private void handleSlot(Player player, AIBotMenu menu, int slot, boolean rightClick) {
        AIBotSettings settings = menu.getSettings();
        UUID          uuid     = player.getUniqueId();

        switch (slot) {

            // ── Combat ────────────────────────────────────────────────────────
            case AIBotMenu.SLOT_ATTACK_MOBS:
                settings.setAttackMobs(!settings.isAttackMobs());
                log(player, "attackMobs", settings.isAttackMobs());
                break;

            case AIBotMenu.SLOT_ATTACK_PLAYERS:
                settings.setAttackPlayers(!settings.isAttackPlayers());
                log(player, "attackPlayers", settings.isAttackPlayers());
                break;

            case AIBotMenu.SLOT_ATTACK_RANGE:
                settings.setAttackRange(settings.getAttackRange() + (rightClick ? -1 : 1));
                log(player, "attackRange", settings.getAttackRange());
                break;

            case AIBotMenu.SLOT_ATTACK_DAMAGE:
                settings.setAttackDamage(settings.getAttackDamage() + (rightClick ? -1 : 1));
                log(player, "attackDamage", settings.getAttackDamage());
                break;

            // ── Build ─────────────────────────────────────────────────────────
            case AIBotMenu.SLOT_AUTO_BUILD:
                settings.setAutoBuild(!settings.isAutoBuild());
                log(player, "autoBuild", settings.isAutoBuild());
                break;

            case AIBotMenu.SLOT_BUILD_TYPE:
                settings.cycleBuildType();
                log(player, "buildType", settings.getBuildType());
                break;

            case AIBotMenu.SLOT_BUILD_AROUND:
                settings.setBuildAround(!settings.isBuildAround());
                log(player, "buildAround", settings.isBuildAround());
                break;

            // ── Behaviour ─────────────────────────────────────────────────────
            case AIBotMenu.SLOT_SPAWN_CMD:
                if (rightClick) {
                    settings.setSpawnCommand("");
                    player.sendMessage("§7Команда при появлении очищена.");
                    log(player, "spawnCommand", "");
                } else {
                    // Close menu and prompt the player to type the command in chat
                    player.closeInventory();
                    player.sendMessage("§eВведите в чат команду для выполнения при появлении AI Bot:");
                    player.sendMessage("§7(начните с /, например /say Привет!)");
                    waitForChatInput(player, uuid, "spawnCommand");
                    return; // settings saved by waitForChatInput
                }
                break;

            case AIBotMenu.SLOT_DESPAWN_CMD:
                if (rightClick) {
                    settings.setDespawnCommand("");
                    player.sendMessage("§7Команда при исчезновении очищена.");
                    log(player, "despawnCommand", "");
                } else {
                    player.closeInventory();
                    player.sendMessage("§eВведите в чат команду для выполнения при исчезновении AI Bot:");
                    player.sendMessage("§7(начните с /, например /say Пока!)");
                    waitForChatInput(player, uuid, "despawnCommand");
                    return;
                }
                break;

            case AIBotMenu.SLOT_MOVEMENT_SPEED:
                settings.setMovementSpeed(settings.getMovementSpeed() + (rightClick ? -0.5 : 0.5));
                aiPlayerManager.applyMovementSpeed(settings.getMovementSpeed());
                log(player, "movementSpeed", settings.getMovementSpeed());
                break;

            // ── Control ───────────────────────────────────────────────────────
            case AIBotMenu.SLOT_SPAWN_BOT:
                player.closeInventory();
                aiPlayerManager.spawnAI(player);
                return;

            case AIBotMenu.SLOT_DESPAWN_BOT:
                player.closeInventory();
                aiPlayerManager.despawnAI(player);
                return;

            case AIBotMenu.SLOT_RESET_SETTINGS:
                AIBotSettings.reset(uuid);
                player.sendMessage("§a✅ Настройки AI Bot сброшены.");
                LOGGER.info(player.getName() + " сбросил настройки AI Bot");
                // Reopen with fresh defaults
                Bukkit.getScheduler().runTask(plugin, () -> openMenu(player));
                player.closeInventory();
                return;

            default:
                return; // separator or header — nothing to do
        }

        // Persist and refresh the dynamic slots
        settings.save(uuid);
        menu.updateDynamic();
    }

    // ── Chat-input prompt ─────────────────────────────────────────────────────

    /** Seconds to wait for a chat response before auto-cancelling the prompt. */
    private static final long CHAT_PROMPT_TIMEOUT_TICKS = 20L * 30; // 30 seconds

    /**
     * Registers a one-shot chat listener that captures the next message from
     * {@code player} and stores it in the given {@code field} of their settings.
     * The temporary listener is always unregistered — either on capture or after
     * {@link #CHAT_PROMPT_TIMEOUT_TICKS} ticks — to prevent handler leaks.
     *
     * <p>{@code @SuppressWarnings("deprecation")} is needed because
     * {@code AsyncPlayerChatEvent} is deprecated in Spigot 1.19+ (replaced by
     * {@code AsyncChatEvent}) but remains the most portable option across all
     * 1.20.x builds without requiring Paper-specific API.
     */
    @SuppressWarnings("deprecation")
    private void waitForChatInput(Player player, UUID uuid, String field) {
        org.bukkit.event.Listener tempListener = new org.bukkit.event.Listener() {};
        Bukkit.getPluginManager().registerEvents(tempListener, plugin);

        final boolean[] captured = {false};

        // Timeout task: auto-unregister if no input arrives within 30 seconds
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!captured[0]) {
                captured[0] = true;
                org.bukkit.event.HandlerList.unregisterAll(tempListener);
                if (player.isOnline()) {
                    player.sendMessage("§c⏰ Время ввода истекло. Попробуйте снова.");
                }
            }
        }, CHAT_PROMPT_TIMEOUT_TICKS);

        Bukkit.getPluginManager().registerEvent(
                org.bukkit.event.player.AsyncPlayerChatEvent.class,
                tempListener,
                org.bukkit.event.EventPriority.HIGHEST,
                (l, event) -> {
                    if (!(event instanceof org.bukkit.event.player.AsyncPlayerChatEvent)) return;
                    org.bukkit.event.player.AsyncPlayerChatEvent chatEvent =
                            (org.bukkit.event.player.AsyncPlayerChatEvent) event;
                    if (!chatEvent.getPlayer().getUniqueId().equals(uuid)) return;
                    if (captured[0]) return;
                    captured[0] = true;
                    chatEvent.setCancelled(true);

                    String input = chatEvent.getMessage().trim();
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        AIBotSettings s = AIBotSettings.load(uuid);
                        if ("spawnCommand".equals(field)) {
                            s.setSpawnCommand(input);
                            player.sendMessage("§aКоманда при появлении сохранена: §f" + input);
                            log(player, "spawnCommand", input);
                        } else {
                            s.setDespawnCommand(input);
                            player.sendMessage("§aКоманда при исчезновении сохранена: §f" + input);
                            log(player, "despawnCommand", input);
                        }
                        s.save(uuid);
                        org.bukkit.event.HandlerList.unregisterAll(tempListener);
                    });
                },
                plugin,
                true);
    }

    // ── Logging ────────────────────────────────────────────────────────────────

    private static void log(Player player, String param, Object value) {
        LOGGER.info("[AI Bot] " + player.getName() + " изменил " + param + " → " + value);
    }
}
