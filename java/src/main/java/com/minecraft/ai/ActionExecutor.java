package com.minecraft.ai;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Random;
import java.util.logging.Logger;

/**
 * Executes in-game AI actions: building structures, spawning mobs,
 * and generating ore veins.
 *
 * All public methods MUST be called from the main server thread.
 */
public class ActionExecutor {

    private static final Logger LOGGER = Logger.getLogger("ActionExecutor");

    private final JavaPlugin plugin;
    private final Random random = new Random();

    public ActionExecutor(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // ── Public API ───────────────────────────────────────────────────────────

    /**
     * Build a structure at the player's current location.
     *
     * @param player        the requesting player
     * @param structureType "house", "tower", or "bridge" (case-insensitive)
     */
    public void buildStructure(Player player, String structureType) {
        Location origin = player.getLocation().clone();
        String type = (structureType != null) ? structureType.toLowerCase() : "house";

        switch (type) {
            case "tower":
                buildTower(origin);
                player.sendMessage("§a🏗️  Башня построена!");
                LOGGER.info(player.getName() + " построил башню");
                break;
            case "bridge":
                buildBridge(origin);
                player.sendMessage("§a🏗️  Мост построен!");
                LOGGER.info(player.getName() + " построил мост");
                break;
            default:
                buildHouse(origin);
                player.sendMessage("§a🏗️  Дом построен!");
                LOGGER.info(player.getName() + " построил дом");
                break;
        }
    }

    /**
     * Spawn mobs near the player.
     *
     * @param player  the requesting player
     * @param mobType "zombie", "skeleton", or "creeper" (case-insensitive)
     * @param count   number of mobs to spawn (clamped 1–10)
     */
    public void spawnMob(Player player, String mobType, int count) {
        Location loc = player.getLocation();
        World world = loc.getWorld();
        EntityType entityType = resolveEntityType(mobType);
        int actualCount = Math.max(1, Math.min(count, 10));

        for (int i = 0; i < actualCount; i++) {
            double ox = (random.nextDouble() - 0.5) * 10;
            double oz = (random.nextDouble() - 0.5) * 10;
            Location spawnLoc = loc.clone().add(ox, 0, oz);
            spawnLoc.setY(world.getHighestBlockYAt(spawnLoc) + 1);
            world.spawnEntity(spawnLoc, entityType);
        }

        player.sendMessage("§a👹 Призвано " + actualCount + " x "
                + entityType.name().toLowerCase() + "!");
        LOGGER.info(player.getName() + " призвал " + actualCount + " x " + entityType);
    }

    /**
     * Generate an ore vein near the player (slightly below ground).
     *
     * @param player   the requesting player
     * @param oreType  "diamond", "gold", or "iron" (case-insensitive)
     * @param veinSize number of ore blocks to place (clamped 1–20)
     */
    public void generateOre(Player player, String oreType, int veinSize) {
        Location loc = player.getLocation();
        World world = loc.getWorld();
        Material oreMaterial = resolveOreMaterial(oreType);
        int actualSize = Math.max(1, Math.min(veinSize, 20));
        int px = loc.getBlockX();
        int py = loc.getBlockY();
        int pz = loc.getBlockZ();

        for (int i = 0; i < actualSize; i++) {
            int ox = random.nextInt(11) - 5;
            int oy = -(random.nextInt(5) + 1); // 1–5 blocks below player
            int oz = random.nextInt(11) - 5;
            world.getBlockAt(px + ox, py + oy, pz + oz).setType(oreMaterial);
        }

        player.sendMessage("§a⛏️  Сгенерировано " + actualSize + " блоков "
                + oreMaterial.name().toLowerCase() + "!");
        LOGGER.info(player.getName() + " сгенерировал " + actualSize + " x " + oreMaterial);
    }

    // ── Structure builders ───────────────────────────────────────────────────

    private void buildHouse(Location origin) {
        World world = origin.getWorld();
        int px = origin.getBlockX();
        int py = origin.getBlockY();
        int pz = origin.getBlockZ();

        // Floor
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                world.getBlockAt(px + x, py, pz + z).setType(Material.OAK_PLANKS);
            }
        }

        // Walls (y = 1–3)
        for (int y = 1; y <= 3; y++) {
            for (int x = -2; x <= 2; x++) {
                world.getBlockAt(px + x, py + y, pz - 2).setType(Material.OAK_PLANKS);
                world.getBlockAt(px + x, py + y, pz + 2).setType(Material.OAK_PLANKS);
            }
            for (int z = -1; z <= 1; z++) {
                world.getBlockAt(px - 2, py + y, pz + z).setType(Material.OAK_PLANKS);
                world.getBlockAt(px + 2, py + y, pz + z).setType(Material.OAK_PLANKS);
            }
        }

        // Roof
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                world.getBlockAt(px + x, py + 4, pz + z).setType(Material.OAK_PLANKS);
            }
        }

        // Door opening (front wall, center)
        world.getBlockAt(px, py + 1, pz - 2).setType(Material.AIR);
        world.getBlockAt(px, py + 2, pz - 2).setType(Material.AIR);

        // Windows
        world.getBlockAt(px - 1, py + 2, pz - 2).setType(Material.GLASS);
        world.getBlockAt(px + 1, py + 2, pz - 2).setType(Material.GLASS);
        world.getBlockAt(px - 1, py + 2, pz + 2).setType(Material.GLASS);
        world.getBlockAt(px + 1, py + 2, pz + 2).setType(Material.GLASS);
    }

    private void buildTower(Location origin) {
        World world = origin.getWorld();
        int px = origin.getBlockX();
        int py = origin.getBlockY();
        int pz = origin.getBlockZ();
        int height = 12;

        // Hollow cylinder walls
        for (int y = 0; y < height; y++) {
            for (int x = -2; x <= 2; x++) {
                world.getBlockAt(px + x, py + y, pz - 2).setType(Material.STONE_BRICKS);
                world.getBlockAt(px + x, py + y, pz + 2).setType(Material.STONE_BRICKS);
            }
            for (int z = -1; z <= 1; z++) {
                world.getBlockAt(px - 2, py + y, pz + z).setType(Material.STONE_BRICKS);
                world.getBlockAt(px + 2, py + y, pz + z).setType(Material.STONE_BRICKS);
            }
        }

        // Battlements at the top (every other block)
        for (int x = -2; x <= 2; x += 2) {
            world.getBlockAt(px + x, py + height, pz - 2).setType(Material.STONE_BRICKS);
            world.getBlockAt(px + x, py + height, pz + 2).setType(Material.STONE_BRICKS);
        }
        for (int z = -2; z <= 2; z += 2) {
            world.getBlockAt(px - 2, py + height, pz + z).setType(Material.STONE_BRICKS);
            world.getBlockAt(px + 2, py + height, pz + z).setType(Material.STONE_BRICKS);
        }
    }

    private void buildBridge(Location origin) {
        World world = origin.getWorld();
        int px = origin.getBlockX();
        int py = origin.getBlockY();
        int pz = origin.getBlockZ();
        int halfLength = 10;

        for (int x = -halfLength; x <= halfLength; x++) {
            // 3-block-wide deck
            for (int z = -1; z <= 1; z++) {
                world.getBlockAt(px + x, py, pz + z).setType(Material.OAK_PLANKS);
            }
            // Fence railings on each side
            world.getBlockAt(px + x, py + 1, pz - 1).setType(Material.OAK_FENCE);
            world.getBlockAt(px + x, py + 1, pz + 1).setType(Material.OAK_FENCE);
        }
    }

    // ── Resolvers ────────────────────────────────────────────────────────────

    private EntityType resolveEntityType(String mobType) {
        if (mobType == null) return EntityType.ZOMBIE;
        switch (mobType.toLowerCase()) {
            case "skeleton": return EntityType.SKELETON;
            case "creeper":  return EntityType.CREEPER;
            default:         return EntityType.ZOMBIE;
        }
    }

    private Material resolveOreMaterial(String oreType) {
        if (oreType == null) return Material.IRON_ORE;
        switch (oreType.toLowerCase()) {
            case "diamond": return Material.DIAMOND_ORE;
            case "gold":    return Material.GOLD_ORE;
            default:        return Material.IRON_ORE;
        }
    }
}
