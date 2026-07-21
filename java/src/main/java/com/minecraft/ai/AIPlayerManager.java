package com.minecraft.ai;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

/**
 * Singleton controller for the virtual AI player.
 *
 * <p>Owns the single {@link VirtualPlayer} instance and routes chat commands
 * such as «появись», «строй дом», «добывай», etc.
 *
 * <p>Obtain the instance via {@link #getInstance(JavaPlugin)} (first call)
 * or {@link #getInstance()} (subsequent calls).  Call {@link #shutdown()}
 * from {@code JavaPlugin.onDisable()} to despawn the NPC and free resources.
 */
public class AIPlayerManager {

    private static final Logger LOGGER = Logger.getLogger("AIPlayerManager");

    private static AIPlayerManager instance;

    private final JavaPlugin plugin;
    private VirtualPlayer virtualPlayer;
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
     * Spawns the AI near {@code player}, creating the VirtualPlayer and
     * FakePlayerNPC (via ProtocolLib) if needed.
     */
    public void spawnAI(Player player) {
        // Visual NPC via ProtocolLib (Steve skin)
        if (fakePlayerNPC == null) {
            fakePlayerNPC = new FakePlayerNPC(plugin, "AI_bot");
        }
        if (!fakePlayerNPC.isSpawned()) {
            fakePlayerNPC.spawn(player);
        }

        // Gameplay entity (zombie-based, handles movement/combat/building)
        if (virtualPlayer == null) {
            virtualPlayer = new VirtualPlayer(plugin, "AI_Bot");
        }
        if (virtualPlayer.isSpawned()) {
            player.sendMessage("§e🤖 AI уже активен!");
            return;
        }
        virtualPlayer.spawn(player);
        player.sendMessage("§a🤖 Виртуальный игрок §b" + virtualPlayer.getName()
                + " §aпоявился рядом с тобой!");
        LOGGER.info("AI spawned near " + player.getName());
    }

    /**
     * Despawns the AI if it is currently active.
     *
     * @param reporter player to notify (may be null)
     */
    public void despawnAI(Player reporter) {
        boolean wasActive = false;

        if (fakePlayerNPC != null && fakePlayerNPC.isSpawned()) {
            fakePlayerNPC.despawn();
            wasActive = true;
        }

        if (virtualPlayer != null && virtualPlayer.isSpawned()) {
            virtualPlayer.despawn();
            wasActive = true;
        }

        if (!wasActive) {
            if (reporter != null) reporter.sendMessage("§c🤖 AI не активен");
            return;
        }
        if (reporter != null) reporter.sendMessage("§c🤖 Виртуальный игрок ушёл");
        LOGGER.info("AI despawned");
    }

    /**
     * Routes an AI chat command to the appropriate handler.
     *
     * <p>Supported commands (Russian and English aliases):
     * <ul>
     *   <li>появись / spawn
     *   <li>уходи / despawn
     *   <li>строй дом / build house
     *   <li>следуй за мной / follow
     *   <li>призови всех / summon all
     *   <li>добывай &lt;материал&gt; / mine &lt;material&gt;
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

            case "строй дом":
            case "build house":
                buildHouse(player);
                break;

            case "следуй за мной":
            case "follow":
                followPlayer(player);
                break;

            case "призови всех":
            case "summon all":
                summonAll(player);
                break;

            default:
                if (cmd.startsWith("добывай ")) {
                    mineBlock(player, cmd.substring(8).trim());
                } else if (cmd.startsWith("mine ")) {
                    mineBlock(player, cmd.substring(5).trim());
                } else {
                    player.sendMessage("§c🤖 Неизвестная команда AI: §e" + rawCommand);
                    sendHelp(player);
                }
                break;
        }
    }

    /** Cleans up on plugin disable: despawns NPC and resets the singleton. */
    public void shutdown() {
        if (fakePlayerNPC != null && fakePlayerNPC.isSpawned()) {
            fakePlayerNPC.despawn();
        }
        fakePlayerNPC = null;
        if (virtualPlayer != null && virtualPlayer.isSpawned()) {
            virtualPlayer.despawn();
        }
        virtualPlayer = null;
        instance = null;
        LOGGER.info("AIPlayerManager shut down");
    }

    /** Returns the managed VirtualPlayer (may be null if not yet created). */
    public VirtualPlayer getVirtualPlayer() {
        return virtualPlayer;
    }

    // ── Private command handlers ─────────────────────────────────────────────

    private void buildHouse(Player player) {
        if (!ensureSpawned(player)) return;
        if (virtualPlayer.isBusy()) {
            player.sendMessage("§e🤖 AI сейчас занят!");
            return;
        }
        StructurePlan plan = StructurePlan.simpleHouse(player.getLocation().clone());
        virtualPlayer.buildStructure(plan, player);
    }

    private void followPlayer(Player player) {
        if (!ensureSpawned(player)) return;
        virtualPlayer.follow(player);
        player.sendMessage("§a🤖 " + virtualPlayer.getName() + " следует за тобой!");
    }

    private void summonAll(Player player) {
        if (!ensureSpawned(player)) return;
        plugin.getServer().getOnlinePlayers().forEach(p -> {
            if (!p.equals(player)) {
                p.teleport(virtualPlayer.getLocation());
                p.sendMessage("§a🤖 " + virtualPlayer.getName()
                        + " §aпризвал тебя к себе!");
            }
        });
        player.sendMessage("§a🤖 Все игроки телепортированы к AI!");
    }

    private void mineBlock(Player player, String blockName) {
        if (!ensureSpawned(player)) return;
        if (virtualPlayer.isBusy()) {
            player.sendMessage("§e🤖 AI сейчас занят!");
            return;
        }
        Material mat = resolveMaterial(blockName);
        if (mat == null) {
            player.sendMessage("§c❌ Неизвестный материал: §e" + blockName);
            return;
        }
        virtualPlayer.mineBlocks(mat, 5, player);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Returns false and notifies the player if the NPC is not spawned. */
    private boolean ensureSpawned(Player player) {
        if (virtualPlayer == null || !virtualPlayer.isSpawned()) {
            player.sendMessage("§c❌ AI не активен. Напиши §eai появись §cчтобы заспавнить.");
            return false;
        }
        return true;
    }

    /** Sends the list of available AI player commands to {@code player}. */
    private void sendHelp(Player player) {
        player.sendMessage("§7Команды AI-игрока: §eai появись §7| §eai строй дом §7| "
                + "§eai добывай [материал] §7| §eai следуй за мной §7| "
                + "§eai призови всех §7| §eai уходи");
    }

    /**
     * Resolves a Russian or English material name to a {@link Material}.
     * Returns {@code null} if the name is unrecognised.
     */
    private Material resolveMaterial(String name) {
        if (name == null || name.isEmpty()) return null;
        switch (name.toLowerCase()) {
            case "камень":  case "stone":   return Material.STONE;
            case "уголь":   case "coal":    return Material.COAL_ORE;
            case "железо":  case "iron":    return Material.IRON_ORE;
            case "золото":  case "gold":    return Material.GOLD_ORE;
            case "алмаз":   case "diamond": return Material.DIAMOND_ORE;
            case "дерево":  case "wood":
            case "бревно":  case "log":     return Material.OAK_LOG;
            case "песок":   case "sand":    return Material.SAND;
            case "гравий":  case "gravel":  return Material.GRAVEL;
            case "земля":   case "dirt":    return Material.DIRT;
            case "трава":   case "grass":   return Material.GRASS_BLOCK;
            default:
                // Fall back to direct Material name lookup
                try {
                    return Material.valueOf(name.toUpperCase());
                } catch (IllegalArgumentException ignored) {
                    return null;
                }
        }
    }
}
