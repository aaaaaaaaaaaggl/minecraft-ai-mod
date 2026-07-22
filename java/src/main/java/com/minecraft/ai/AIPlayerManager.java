package com.minecraft.ai;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

/**
 * Singleton controller for the AI player NPC.
 *
 * <p>Owns the single {@link FakePlayerNPC} instance (Citizens-based) and routes
 * chat commands such as «появись» and «уходи».
 *
 * <p>Obtain the instance via {@link #getInstance(JavaPlugin)} (first call)
 * or {@link #getInstance()} (subsequent calls).  Call {@link #shutdown()}
 * from {@code JavaPlugin.onDisable()} to despawn the NPC and free resources.
 */
public class AIPlayerManager {

    private static final Logger LOGGER = Logger.getLogger("AIPlayerManager");

    private static AIPlayerManager instance;

    private final JavaPlugin plugin;
    private FakePlayerNPC fakePlayerNPC;

    private AIPlayerManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /** Returns (and creates if necessary) the singleton, binding it to {@code plugin}. */
    public static synchronized AIPlayerManager getInstance(JavaPlugin plugin) {
        if (instance == null) {
            instance = new AIPlayerManager(plugin);
        }
        return instance;
    }

    /** Returns the existing singleton, or {@code null} if not yet initialised. */
    public static synchronized AIPlayerManager getInstance() {
        return instance;
    }

    // ── Public API ──────────────────────────────────────────────────────────

    /**
     * Spawns the AI NPC near {@code player} via Citizens.
     */
    public void spawnAI(Player player) {
        if (fakePlayerNPC == null) {
            fakePlayerNPC = new FakePlayerNPC(plugin, "AI_bot");
        }
        if (fakePlayerNPC.isSpawned()) {
            player.sendMessage("§e🤖 AI уже активен!");
            return;
        }
        fakePlayerNPC.spawn(player);
        player.sendMessage("§a🤖 AI Bot §aпоявился рядом с тобой!");
        LOGGER.info("AI spawned near " + player.getName());
    }

    /**
     * Despawns the AI NPC if it is currently active.
     *
     * @param reporter player to notify (may be null)
     */
    public void despawnAI(Player reporter) {
        if (fakePlayerNPC == null || !fakePlayerNPC.isSpawned()) {
            if (reporter != null) reporter.sendMessage("§c🤖 AI не активен.");
            return;
        }
        fakePlayerNPC.despawn();
        if (reporter != null) reporter.sendMessage("§c🤖 AI Bot §cушёл");
        LOGGER.info("AI despawned");
    }

    public void followPlayer(Player player) {
        if (fakePlayerNPC == null || !fakePlayerNPC.isSpawned()) {
            player.sendMessage("§c🤖 Сначала призови AI бота.");
            return;
        }
        fakePlayerNPC.startFollowing(player);
        player.sendMessage("§a🤖 AI теперь следует за тобой.");
    }

    public void stopFollowing(Player player) {
        if (fakePlayerNPC == null || !fakePlayerNPC.isSpawned()) {
            player.sendMessage("§c🤖 AI не активен.");
            return;
        }
        fakePlayerNPC.stopFollowing();
        player.sendMessage("§e🤖 AI остановился.");
    }

    /**
     * Routes an AI chat command to the appropriate handler.
     *
     * <p>Supported commands (Russian and English aliases):
     * <ul>
     *   <li>появись / spawn
     *   <li>уходи / despawn
     * </ul>
     *
     * @param rawCommand the command text (without the "ai " prefix)
     * @param player     the player issuing the command
     */
    public void commandAI(String rawCommand, Player player) {
        String cmd = rawCommand.trim().toLowerCase();

        switch (cmd) {
            case "появись":
            case "spawn":
                spawnAI(player);
                break;

            case "уходи":
            case "despawn":
                despawnAI(player);
                break;

            case "следуй":
            case "следуй за мной":
            case "follow":
                followPlayer(player);
                break;

            case "стоп":
            case "остановись":
            case "не следуй":
            case "stop":
            case "unfollow":
                stopFollowing(player);
                break;

            default:
                player.sendMessage("§c🤖 Неизвестная команда AI: §e" + rawCommand);
                sendHelp(player);
                break;
        }
    }

    /** Cleans up on plugin disable: despawns NPC and resets the singleton. */
    public void shutdown() {
        if (fakePlayerNPC != null && fakePlayerNPC.isSpawned()) {
            fakePlayerNPC.despawn();
        }
        fakePlayerNPC = null;
        instance = null;
        LOGGER.info("AIPlayerManager shut down");
    }

    // ── Private command handlers ─────────────────────────────────────────────

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Sends the list of available AI player commands to {@code player}. */
    private void sendHelp(Player player) {
        player.sendMessage("§7Команды AI-игрока: §eai появись §7| §eai уходи §7| §eai follow §7| §eai stop");
    }
}
