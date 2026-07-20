package com.minecraft.ai;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * AI bot player that can autonomously build, mine, follow and gather resources.
 *
 * The AI is represented in-world as a fully-equipped {@link ArmorStand} entity.
 * All public methods that touch the world MUST be called from the main server thread.
 */
public class AIPlayer {

    private static final Logger LOGGER = Logger.getLogger("AIPlayer");

    /** Display name shown above the entity. */
    private static final String AI_NAME = "§b§lAI_Игрок";

    /** Maximum blocks mined/gathered per operation. */
    private static final int MAX_MINE_BLOCKS = 64;

    /** Ticks between each block-break during mine/gather operations. */
    private static final int MINE_TICKS_PER_BLOCK = 3;

    /** Ticks between each follow-movement step. */
    private static final long FOLLOW_PERIOD_TICKS = 5L;

    /** Distance (blocks) at which the AI stops trying to close the gap. */
    private static final double FOLLOW_CLOSE_DISTANCE = 3.0;

    /** Speed multiplier for each follow step (blocks per tick). */
    private static final double FOLLOW_STEP_SIZE = 0.8;

    /**
     * Extra distance added to the movement vector to prevent the AI
     * from overshooting and oscillating around the target.
     */
    private static final double FOLLOW_OVERSHOOT_BUFFER = 0.5;

    private final JavaPlugin plugin;
    private final ActionExecutor actionExecutor;

    private ArmorStand stand;
    private boolean active = false;
    private BukkitTask followTask;

    public AIPlayer(JavaPlugin plugin, ActionExecutor actionExecutor) {
        this.plugin = plugin;
        this.actionExecutor = actionExecutor;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Spawn the AI player ~3 blocks in front of the caller.
     * If one is already active this is a no-op with an informational message.
     */
    public void join(Player caller) {
        if (isActive()) {
            caller.sendMessage("§b🤖 AI_Игрок уже находится на сервере!");
            return;
        }

        Location loc = spawnLocation(caller);

        stand = caller.getWorld().spawn(loc, ArmorStand.class, as -> {
            as.setCustomName(AI_NAME);
            as.setCustomNameVisible(true);
            as.setArms(true);
            as.setBasePlate(true);
            as.setGravity(true);
            as.setVisible(true);
            as.setInvulnerable(true);

            // Equip with iron gear and pickaxe for a "player" look
            as.getEquipment().setHelmet(new ItemStack(Material.IRON_HELMET));
            as.getEquipment().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
            as.getEquipment().setLeggings(new ItemStack(Material.IRON_LEGGINGS));
            as.getEquipment().setBoots(new ItemStack(Material.IRON_BOOTS));
            as.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_PICKAXE));
        });

        active = true;
        LOGGER.info("AI player spawned at " + loc);
        Bukkit.broadcastMessage("§b§l[AI_Игрок] §fAI игрок присоединился к серверу! 🤖");
        Bukkit.broadcastMessage("§b§l[AI_Игрок] §fПривет! Я AI помощник. Напиши 'ai помощь' для списка команд!");
    }

    /**
     * Remove the AI player entity from the world.
     */
    public void leave() {
        cancelFollow();
        if (stand != null && !stand.isDead()) {
            stand.remove();
        }
        active = false;
        Bukkit.broadcastMessage("§b§l[AI_Игрок] §fAI игрок покинул сервер. До свидания! 👋");
        LOGGER.info("AI player left");
    }

    /**
     * Build a named structure at a location 5 blocks in front of the caller.
     *
     * @param caller        the player who issued the command
     * @param structureType e.g. "house", "tower", "bridge", "mansion"
     */
    public void build(Player caller, String structureType) {
        ensureJoined(caller);
        Location buildOrigin = buildLocation(caller);
        Bukkit.broadcastMessage("§b§l[AI_Игрок] §fНачинаю строить: " + structureType + "! 🏗️");
        actionExecutor.buildStructure(caller, structureType, buildOrigin);
    }

    /**
     * Mine all non-bedrock/air/liquid blocks within {@code radius} blocks of the AI.
     *
     * @param caller who issued the command
     * @param radius 1–8 blocks
     */
    public void mine(Player caller, int radius) {
        ensureJoined(caller);
        int r = Math.max(1, Math.min(radius, 8));
        Bukkit.broadcastMessage("§b§l[AI_Игрок] §fНачинаю добычу в радиусе " + r + " блоков! ⛏️");

        Location centre = aiLocation(caller);
        List<Block> blocks = collectMineable(centre, r);
        equipPickaxe();

        new BukkitRunnable() {
            int idx = 0;

            @Override
            public void run() {
                if (idx >= blocks.size()) {
                    cancel();
                    Bukkit.broadcastMessage(
                            "§b§l[AI_Игрок] §fДобыча завершена! Добыто " + idx + " блоков ✅");
                    equipPickaxe();
                    return;
                }
                Block b = blocks.get(idx);
                if (b.getType() != Material.AIR) {
                    b.breakNaturally();
                    moveStandTo(b.getLocation());
                }
                idx++;
            }
        }.runTaskTimer(plugin, 0L, MINE_TICKS_PER_BLOCK);
    }

    /**
     * Make the AI continuously follow {@code target} until {@link #stopFollow} is called.
     */
    public void follow(Player target) {
        ensureJoined(target);
        cancelFollow();
        Bukkit.broadcastMessage("§b§l[AI_Игрок] §fСледую за " + target.getName() + "! 🏃");

        followTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!isActive() || !target.isOnline()) {
                    cancel();
                    return;
                }
                Location targetLoc = target.getLocation();
                Location aiLoc = stand.getLocation();
                double dist = aiLoc.distance(targetLoc);

                if (dist > FOLLOW_CLOSE_DISTANCE) {
                    Vector dir = targetLoc.toVector().subtract(aiLoc.toVector());
                    if (dir.length() > 0) {
                        dir.normalize().multiply(Math.min(FOLLOW_STEP_SIZE, dist - FOLLOW_CLOSE_DISTANCE + FOLLOW_OVERSHOOT_BUFFER));
                        Location next = aiLoc.clone().add(dir);
                        // Use the player's actual Y to follow them inside buildings,
                        // underground, or on ledges — not the surface height.
                        next.setY(targetLoc.getY());
                        next.setYaw(targetLoc.getYaw());
                        stand.teleport(next);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, FOLLOW_PERIOD_TICKS);
    }

    /**
     * Cancel an active follow task.
     *
     * @param caller player to notify, or {@code null} for broadcast-only
     */
    public void stopFollow(Player caller) {
        cancelFollow();
        Bukkit.broadcastMessage("§b§l[AI_Игрок] §fОстановился! Жду новых команд 🛑");
    }

    /**
     * Search for and mine blocks of a specific type within {@code radius} of the AI.
     *
     * @param caller    who issued the command
     * @param blockType Russian or English name of the block/ore
     * @param radius    1–16 blocks search radius
     */
    public void gather(Player caller, String blockType, int radius) {
        ensureJoined(caller);
        Material mat = resolveMaterial(blockType);
        int r = Math.max(1, Math.min(radius, 16));
        Bukkit.broadcastMessage(
                "§b§l[AI_Игрок] §fИщу " + mat.name().toLowerCase() + " в радиусе " + r + " блоков... 🔍");

        Location centre = aiLocation(caller);
        List<Block> targets = collectByMaterial(centre, mat, r);

        if (targets.isEmpty()) {
            Bukkit.broadcastMessage(
                    "§b§l[AI_Игрок] §fНе нашёл " + mat.name().toLowerCase() + " рядом 😔");
            return;
        }

        equipPickaxe();

        new BukkitRunnable() {
            int idx = 0;

            @Override
            public void run() {
                if (idx >= targets.size()) {
                    cancel();
                    Bukkit.broadcastMessage(
                            "§b§l[AI_Игрок] §fСобрал " + idx + " блоков " + mat.name().toLowerCase() + " ✅");
                    return;
                }
                Block b = targets.get(idx);
                if (b.getType() == mat) {
                    b.breakNaturally();
                    moveStandTo(b.getLocation());
                }
                idx++;
            }
        }.runTaskTimer(plugin, 0L, MINE_TICKS_PER_BLOCK);
    }

    /**
     * @return {@code true} if the AI entity is currently alive and active
     */
    public boolean isActive() {
        return active && stand != null && !stand.isDead();
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    /** Join if not already active. */
    private void ensureJoined(Player player) {
        if (!isActive()) {
            join(player);
        }
    }

    /** Current AI location, or the player's location as fallback. */
    private Location aiLocation(Player player) {
        return isActive() ? stand.getLocation() : player.getLocation();
    }

    /** A location 5 blocks in front of the player – where the AI will build. */
    private Location buildLocation(Player player) {
        Location base = isActive() ? stand.getLocation() : player.getLocation();
        return base.clone().add(
                player.getLocation().getDirection().setY(0).normalize().multiply(5));
    }

    /** A location 3 blocks in front of the player – where the AI spawns. */
    private Location spawnLocation(Player caller) {
        Location loc = caller.getLocation().clone().add(
                caller.getLocation().getDirection().setY(0).normalize().multiply(3));
        loc.setY(caller.getWorld().getHighestBlockYAt(loc));
        return loc;
    }

    private void cancelFollow() {
        if (followTask != null && !followTask.isCancelled()) {
            followTask.cancel();
            followTask = null;
        }
    }

    private void equipPickaxe() {
        if (isActive()) {
            stand.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_PICKAXE));
        }
    }

    private void moveStandTo(Location loc) {
        if (isActive()) {
            stand.teleport(loc.clone().add(0.5, 0, 0.5));
        }
    }

    // ── Block collection ──────────────────────────────────────────────────────

    private List<Block> collectMineable(Location centre, int radius) {
        List<Block> list = new ArrayList<>();
        World world = centre.getWorld();
        int cx = centre.getBlockX(), cy = centre.getBlockY(), cz = centre.getBlockZ();

        outer:
        for (int x = -radius; x <= radius; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Block b = world.getBlockAt(cx + x, cy + y, cz + z);
                    Material m = b.getType();
                    if (m != Material.AIR && m != Material.BEDROCK
                            && m != Material.WATER && m != Material.LAVA) {
                        list.add(b);
                        if (list.size() >= MAX_MINE_BLOCKS) break outer;
                    }
                }
            }
        }
        return list;
    }

    private List<Block> collectByMaterial(Location centre, Material mat, int radius) {
        List<Block> list = new ArrayList<>();
        World world = centre.getWorld();
        int cx = centre.getBlockX(), cy = centre.getBlockY(), cz = centre.getBlockZ();

        outer:
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Block b = world.getBlockAt(cx + x, cy + y, cz + z);
                    if (b.getType() == mat) {
                        list.add(b);
                        if (list.size() >= MAX_MINE_BLOCKS) break outer;
                    }
                }
            }
        }
        return list;
    }

    // ── Material resolver ─────────────────────────────────────────────────────

    private Material resolveMaterial(String blockType) {
        if (blockType == null) return Material.STONE;
        switch (blockType.toLowerCase()) {
            case "камень":   case "stone":   return Material.STONE;
            case "уголь":   case "coal":    return Material.COAL_ORE;
            case "железо":  case "iron":    return Material.IRON_ORE;
            case "золото":  case "gold":    return Material.GOLD_ORE;
            case "алмаз":   case "diamond": return Material.DIAMOND_ORE;
            case "дерево":  case "wood":
            case "дуб":     case "oak":     return Material.OAK_LOG;
            case "земля":   case "dirt":    return Material.DIRT;
            case "песок":   case "sand":    return Material.SAND;
            default:                        return Material.STONE;
        }
    }
}
