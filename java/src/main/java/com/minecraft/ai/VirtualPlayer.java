package com.minecraft.ai;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Виртуальный игрок — NPC, реализованный поверх сущности Zombie.
 *
 * <p>Использует только Bukkit/Spigot API без внешних плагинов.
 * Движение выполняется телепортацией по шагам через BukkitRunnable,
 * добыча и строительство анимируются частицами и звуками.
 *
 * <p>Все методы ДОЛЖНЫ вызываться из главного потока сервера.
 */
public class VirtualPlayer {

    private static final Logger LOGGER = Logger.getLogger("VirtualPlayer");

    /** Размер шага при ходьбе (блоков за тик). */
    private static final double WALK_STEP = 0.25;
    /** Расстояние от игрока, при котором следование считается «рядом». */
    private static final double FOLLOW_MIN_DIST = 3.0;
    /** Максимальное число тиков для одного moveTo (30 секунд). */
    private static final int MOVE_TIMEOUT_TICKS = 20 * 30;

    private final JavaPlugin plugin;
    private final String npcName;
    private Zombie entity;
    private BukkitTask currentTask;
    private boolean busy = false;

    public VirtualPlayer(JavaPlugin plugin, String npcName) {
        this.plugin = plugin;
        this.npcName = npcName;
    }

    // ── Spawn / Despawn ──────────────────────────────────────────────────────

    /**
     * Spawns the NPC next to the given player.
     * Does nothing if the NPC is already alive.
     */
    public void spawn(Player nearPlayer) {
        if (isSpawned()) return;

        Location loc = nearPlayer.getLocation().clone().add(2, 0, 2);
        loc.setY(nearPlayer.getWorld().getHighestBlockYAt(loc) + 1);

        entity = (Zombie) nearPlayer.getWorld().spawnEntity(loc, EntityType.ZOMBIE);
        entity.setCustomName("§b" + npcName);
        entity.setCustomNameVisible(true);
        entity.setAI(false);
        entity.setSilent(true);
        entity.setInvulnerable(true);
        entity.setBaby(false);

        // Default equipment — iron gear to look like a player
        EntityEquipment eq = entity.getEquipment();
        if (eq != null) {
            eq.setHelmet(new ItemStack(Material.IRON_HELMET));
            eq.setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
            eq.setLeggings(new ItemStack(Material.IRON_LEGGINGS));
            eq.setBoots(new ItemStack(Material.IRON_BOOTS));
            eq.setItemInMainHand(new ItemStack(Material.IRON_PICKAXE));
            // Drop chances set to 0 so equipment is not dropped on (unlikely) death
            eq.setHelmetDropChance(0f);
            eq.setChestplateDropChance(0f);
            eq.setLeggingsDropChance(0f);
            eq.setBootsDropChance(0f);
            eq.setItemInMainHandDropChance(0f);
        }

        LOGGER.info("VirtualPlayer '" + npcName + "' spawned at " + loc);
    }

    /**
     * Removes the NPC from the world and cancels any running task.
     */
    public void despawn() {
        cancelCurrentTask();
        if (entity != null && !entity.isDead()) {
            entity.remove();
        }
        entity = null;
        LOGGER.info("VirtualPlayer '" + npcName + "' despawned");
    }

    public boolean isSpawned() {
        return entity != null && !entity.isDead();
    }

    public boolean isBusy() {
        return busy;
    }

    public String getName() {
        return npcName;
    }

    public Location getLocation() {
        return entity != null ? entity.getLocation().clone() : null;
    }

    // ── Movement ─────────────────────────────────────────────────────────────

    /**
     * Teleports the NPC instantly to the given location.
     */
    public void teleport(Location loc) {
        requireSpawned();
        entity.teleport(loc);
    }

    /**
     * Rotates the NPC to face {@code target}.
     */
    public void lookAt(Location target) {
        requireSpawned();
        Location current = entity.getLocation().clone();
        double dx = target.getX() - current.getX();
        double dz = target.getZ() - current.getZ();
        double dy = target.getY() - (current.getY() + entity.getEyeHeight());
        double horizDist = Math.sqrt(dx * dx + dz * dz);

        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, horizDist));
        current.setYaw(yaw);
        current.setPitch(pitch);
        entity.teleport(current);
    }

    /**
     * Walks towards {@code target} step-by-step (non-blocking).
     *
     * @param target     destination
     * @param onFinished callback executed on the main thread when done (may be null)
     */
    public void moveTo(Location target, Runnable onFinished) {
        requireSpawned();
        cancelCurrentTask();
        busy = true;

        currentTask = new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                ticks++;
                if (!isSpawned() || ticks > MOVE_TIMEOUT_TICKS) {
                    finish();
                    return;
                }

                Location current = entity.getLocation();
                double dx = target.getX() - current.getX();
                double dz = target.getZ() - current.getZ();
                double dist = Math.sqrt(dx * dx + dz * dz);

                if (dist < 0.5) {
                    entity.teleport(target);
                    finish();
                    return;
                }

                double stepX = (dx / dist) * WALK_STEP;
                double stepZ = (dz / dist) * WALK_STEP;
                Location next = current.clone().add(stepX, 0, stepZ);
                float yaw = (float) Math.toDegrees(Math.atan2(-stepX, stepZ));
                next.setYaw(yaw);
                next = applyGroundAdjustment(next);
                entity.teleport(next);
            }

            private void finish() {
                busy = false;
                cancel();
                if (onFinished != null) {
                    Bukkit.getScheduler().runTask(plugin, onFinished);
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    // ── Block Interaction ────────────────────────────────────────────────────

    /**
     * Mines {@code block} with an animated progress effect.
     * The block is broken naturally (drops items) after the simulated mining time.
     *
     * @param block      the block to break
     * @param onFinished callback on the main thread when done (may be null)
     */
    public void breakBlock(Block block, Runnable onFinished) {
        requireSpawned();
        if (busy) return;
        busy = true;

        lookAt(block.getLocation().clone().add(0.5, 0.5, 0.5));
        ItemStack tool = selectTool(block.getType());
        setMainHandItem(tool);

        int miningTicks = getMiningTicks(block.getType());

        currentTask = new BukkitRunnable() {
            int elapsed = 0;

            @Override
            public void run() {
                if (!isSpawned()) {
                    busy = false;
                    cancel();
                    return;
                }

                elapsed++;

                // Particle crack animation
                Location center = block.getLocation().clone().add(0.5, 0.5, 0.5);
                block.getWorld().spawnParticle(
                        Particle.BLOCK_CRACK, center, 4,
                        0.3, 0.3, 0.3, 0.1, block.getBlockData());

                if (elapsed >= miningTicks) {
                    block.breakNaturally(tool);
                    block.getWorld().playSound(block.getLocation(),
                            Sound.BLOCK_STONE_BREAK, 1f, 1f);
                    busy = false;
                    cancel();
                    if (onFinished != null) {
                        Bukkit.getScheduler().runTask(plugin, onFinished);
                    }
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    /**
     * Places a block of the given material (placed with a short delay).
     *
     * @param block      the block position to place at
     * @param material   material to place
     * @param onFinished callback on the main thread when done (may be null)
     */
    public void placeBlock(Block block, Material material, Runnable onFinished) {
        requireSpawned();
        if (busy) return;
        busy = true;

        lookAt(block.getLocation().clone().add(0.5, 0.5, 0.5));
        setMainHandItem(new ItemStack(material));

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!isSpawned()) {
                    busy = false;
                    return;
                }
                block.setType(material);
                block.getWorld().playSound(block.getLocation(),
                        Sound.BLOCK_STONE_PLACE, 1f, 1f);
                busy = false;
                if (onFinished != null) onFinished.run();
            }
        }.runTaskLater(plugin, 10L);
    }

    // ── Building System ──────────────────────────────────────────────────────

    /**
     * Executes a {@link StructurePlan} step by step, placing one block every
     * 5 ticks (0.25 s).  Sends progress updates to {@code reporter}.
     *
     * @param plan     the structure to build
     * @param reporter player to receive progress messages (may be null)
     */
    public void buildStructure(StructurePlan plan, Player reporter) {
        requireSpawned();
        if (busy) return;
        busy = true;

        List<StructurePlan.BlockEntry> blocks = plan.getBlocks();
        if (blocks.isEmpty()) {
            busy = false;
            return;
        }

        if (reporter != null) {
            reporter.sendMessage("§a🤖 " + npcName + " начинает строительство: §e"
                    + plan.getName() + " §7(" + blocks.size() + " блоков)");
        }

        final int[] index = {0};
        final int total = blocks.size();

        currentTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!isSpawned() || index[0] >= total) {
                    busy = false;
                    cancel();
                    setMainHandItem(new ItemStack(Material.IRON_PICKAXE));
                    if (reporter != null) {
                        reporter.sendMessage("§a🤖 " + npcName
                                + " завершил строительство §e" + plan.getName() + "§a!");
                    }
                    return;
                }

                StructurePlan.BlockEntry entry = blocks.get(index[0]++);
                lookAt(entry.location.clone().add(0.5, 0.5, 0.5));
                setMainHandItem(new ItemStack(entry.material));
                entry.location.getBlock().setType(entry.material);
                entry.location.getWorld().playSound(
                        entry.location, Sound.BLOCK_STONE_PLACE, 0.5f, 1f);

                // Progress notification every 10 blocks
                if (reporter != null && index[0] % 10 == 0) {
                    reporter.sendMessage("§7🔨 " + index[0] + " / " + total);
                }
            }
        }.runTaskTimer(plugin, 5L, 5L);
    }

    // ── AI Behaviours ─────────────────────────────────────────────────────────

    /**
     * Continuously follows {@code target} until another command cancels the task.
     */
    public void follow(Player target) {
        requireSpawned();
        cancelCurrentTask();
        busy = true;

        currentTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!isSpawned() || !target.isOnline()) {
                    busy = false;
                    cancel();
                    return;
                }

                Location targetLoc = target.getLocation();
                Location myLoc = entity.getLocation();
                double dist = myLoc.distance(targetLoc);

                if (dist > FOLLOW_MIN_DIST) {
                    double dx = targetLoc.getX() - myLoc.getX();
                    double dz = targetLoc.getZ() - myLoc.getZ();
                    double step = Math.min(WALK_STEP * 2, dist - FOLLOW_MIN_DIST + 0.1);
                    double nx = myLoc.getX() + (dx / dist) * step;
                    double nz = myLoc.getZ() + (dz / dist) * step;
                    float yaw = (float) Math.toDegrees(Math.atan2(-(nx - myLoc.getX()),
                            nz - myLoc.getZ()));
                    Location next = new Location(myLoc.getWorld(), nx, myLoc.getY(), nz,
                            yaw, myLoc.getPitch());
                    next = applyGroundAdjustment(next);
                    entity.teleport(next);
                } else {
                    lookAt(targetLoc);
                }
            }
        }.runTaskTimer(plugin, 5L, 2L);
    }

    /**
     * Searches within {@code radius} blocks for blocks of {@code material} and
     * breaks them one by one.
     *
     * @param material material to mine
     * @param radius   search radius in blocks
     * @param reporter player to receive progress messages (may be null)
     */
    public void mineBlocks(Material material, int radius, Player reporter) {
        requireSpawned();
        if (busy) return;
        busy = true;

        Location center = entity.getLocation();
        List<Block> targets = new ArrayList<>();
        int clampedRadius = Math.min(radius, 8); // safety cap

        for (int x = -clampedRadius; x <= clampedRadius; x++) {
            for (int y = -clampedRadius; y <= clampedRadius; y++) {
                for (int z = -clampedRadius; z <= clampedRadius; z++) {
                    Block b = center.getWorld().getBlockAt(
                            center.getBlockX() + x,
                            center.getBlockY() + y,
                            center.getBlockZ() + z);
                    if (b.getType() == material) targets.add(b);
                }
            }
        }

        if (targets.isEmpty()) {
            busy = false;
            if (reporter != null) {
                reporter.sendMessage("§c🤖 " + npcName + " не нашёл §e"
                        + material.name().toLowerCase() + " §cпоблизости!");
            }
            return;
        }

        if (reporter != null) {
            reporter.sendMessage("§a🤖 " + npcName + " начинает добывать §e"
                    + targets.size() + " §aблоков §e" + material.name().toLowerCase());
        }

        ItemStack tool = selectTool(material);
        setMainHandItem(tool);
        final int ticksPerBlock = getMiningTicks(material);
        final int[] idx = {0};
        final int[] miningElapsed = {0};
        final int total = targets.size();

        currentTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!isSpawned() || idx[0] >= total) {
                    busy = false;
                    cancel();
                    setMainHandItem(new ItemStack(Material.IRON_PICKAXE));
                    if (reporter != null) {
                        reporter.sendMessage("§a🤖 " + npcName + " завершил добычу!");
                    }
                    return;
                }

                Block block = targets.get(idx[0]);
                if (block.getType() == Material.AIR) {
                    idx[0]++;
                    miningElapsed[0] = 0;
                    return;
                }

                lookAt(block.getLocation().clone().add(0.5, 0.5, 0.5));
                miningElapsed[0]++;

                // Particle crack animation
                block.getWorld().spawnParticle(
                        Particle.BLOCK_CRACK,
                        block.getLocation().clone().add(0.5, 0.5, 0.5),
                        3, 0.3, 0.3, 0.3, 0.1, block.getBlockData());

                if (miningElapsed[0] >= ticksPerBlock) {
                    block.breakNaturally(tool);
                    block.getWorld().playSound(block.getLocation(),
                            Sound.BLOCK_STONE_BREAK, 1f, 1f);
                    idx[0]++;
                    miningElapsed[0] = 0;
                }
            }
        }.runTaskTimer(plugin, 5L, 2L);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void requireSpawned() {
        if (!isSpawned()) {
            throw new IllegalStateException("VirtualPlayer '" + npcName + "' is not spawned");
        }
    }

    private void cancelCurrentTask() {
        if (currentTask != null && !currentTask.isCancelled()) {
            currentTask.cancel();
        }
        currentTask = null;
        busy = false;
    }

    private void setMainHandItem(ItemStack item) {
        if (entity != null && entity.getEquipment() != null) {
            entity.getEquipment().setItemInMainHand(item);
        }
    }

    /**
     * Adjusts the Y coordinate of {@code loc} so the NPC stands on solid ground
     * or falls into a gap rather than walking through blocks.
     */
    private Location applyGroundAdjustment(Location loc) {
        Block ground = loc.getWorld().getBlockAt(
                loc.getBlockX(), (int) loc.getY() - 1, loc.getBlockZ());
        Block feet = loc.getWorld().getBlockAt(
                loc.getBlockX(), (int) loc.getY(), loc.getBlockZ());

        if (!ground.getType().isSolid()) {
            loc.setY(loc.getY() - 1); // step down
        } else if (feet.getType().isSolid()) {
            loc.setY(loc.getY() + 1); // step up
        }
        return loc;
    }

    /**
     * Picks the most appropriate tool for the given material.
     */
    private ItemStack selectTool(Material material) {
        String n = material.name();
        if (n.contains("LOG") || n.contains("WOOD") || n.contains("PLANK")
                || n.contains("BAMBOO")) {
            return new ItemStack(Material.IRON_AXE);
        }
        if (n.contains("DIRT") || n.contains("SAND")
                || n.contains("GRAVEL") || n.contains("SOUL")) {
            return new ItemStack(Material.IRON_SHOVEL);
        }
        // Default: pickaxe for stone, ore, etc.
        return new ItemStack(Material.IRON_PICKAXE);
    }

    /**
     * Returns the simulated number of ticks needed to mine a block of the given type.
     * Uses a coarse hardness approximation.
     */
    private int getMiningTicks(Material material) {
        String n = material.name();
        if (n.contains("OBSIDIAN"))                               return 50;
        if (n.contains("DEEPSLATE"))                              return 25;
        if (n.contains("ORE") || n.contains("STONE")
                || n.contains("BRICK") || n.contains("COBBLE"))  return 15;
        if (n.contains("LOG")  || n.contains("WOOD")
                || n.contains("PLANK"))                           return 10;
        if (n.contains("DIRT") || n.contains("SAND")
                || n.contains("GRAVEL"))                          return  5;
        return 10;
    }
}
