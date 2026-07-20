package com.minecraft.ai;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.type.Gate;
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
     * @param structureType "house", "tower", "bridge", or "mansion" (case-insensitive)
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
            case "mansion":
                buildMansion(origin);
                player.sendMessage("§a🏰  Особняк построен!");
                LOGGER.info(player.getName() + " построил особняк");
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

        // Door (front wall, center) - OAK_DOOR with lower and upper halves
        placeDoor(world, Material.OAK_DOOR, px, py + 1, pz - 2, BlockFace.SOUTH);

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

        // Entrance door (front wall, center) - clear opening and place IRON_DOOR
        world.getBlockAt(px, py, pz - 2).setType(Material.AIR, false);
        placeDoor(world, Material.IRON_DOOR, px, py + 1, pz - 2, BlockFace.SOUTH);
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

        // Entrance gates at both ends of the bridge
        placeGate(world, px - halfLength, py + 1, pz, BlockFace.SOUTH);
        placeGate(world, px + halfLength, py + 1, pz, BlockFace.SOUTH);
    }

    /**
     * Build a large 2-story mansion (12×15 base) with a pitched roof, double door
     * entrance, multi-story windows, interior room divider, corner pillars, and a
     * decorative front porch.
     *
     * Layout (relative to origin):
     *   X: -5 .. +6 (12 wide)   Z: -7 .. +7 (15 deep)
     *   Story 1: y 0–4 (stone brick)
     *   Story 2: y 5–8 (spruce plank)
     *   Roof:    y 9–12 (dark oak plank, stepped pitch)
     */
    private void buildMansion(Location origin) {
        World world = origin.getWorld();
        int px = origin.getBlockX();
        int py = origin.getBlockY();
        int pz = origin.getBlockZ();

        // ── Floor ────────────────────────────────────────────────────────────
        for (int x = -5; x <= 6; x++) {
            for (int z = -7; z <= 7; z++) {
                world.getBlockAt(px + x, py, pz + z).setType(Material.STONE_BRICKS);
            }
        }

        // ── Story 1 walls (y 1–4): stone bricks ──────────────────────────────
        for (int y = 1; y <= 4; y++) {
            for (int x = -5; x <= 6; x++) {
                world.getBlockAt(px + x, py + y, pz - 7).setType(Material.STONE_BRICKS);
                world.getBlockAt(px + x, py + y, pz + 7).setType(Material.STONE_BRICKS);
            }
            for (int z = -6; z <= 6; z++) {
                world.getBlockAt(px - 5, py + y, pz + z).setType(Material.STONE_BRICKS);
                world.getBlockAt(px + 6, py + y, pz + z).setType(Material.STONE_BRICKS);
            }
        }

        // Corner pillars – dark oak log (story 1)
        for (int y = 1; y <= 4; y++) {
            world.getBlockAt(px - 5, py + y, pz - 7).setType(Material.DARK_OAK_LOG);
            world.getBlockAt(px + 6, py + y, pz - 7).setType(Material.DARK_OAK_LOG);
            world.getBlockAt(px - 5, py + y, pz + 7).setType(Material.DARK_OAK_LOG);
            world.getBlockAt(px + 6, py + y, pz + 7).setType(Material.DARK_OAK_LOG);
        }

        // ── Main entrance: double oak door + decorative arch ──────────────────
        // placeDoor replaces the wall blocks at y+1 and y+2 with door halves
        placeDoor(world, Material.OAK_DOOR, px,     py + 1, pz - 7, BlockFace.SOUTH);
        placeDoor(world, Material.OAK_DOOR, px + 1, py + 1, pz - 7, BlockFace.SOUTH);
        // Dark oak arch spanning the double door (replaces wall at y+3)
        world.getBlockAt(px - 1, py + 3, pz - 7).setType(Material.DARK_OAK_PLANKS);
        world.getBlockAt(px,     py + 3, pz - 7).setType(Material.DARK_OAK_PLANKS);
        world.getBlockAt(px + 1, py + 3, pz - 7).setType(Material.DARK_OAK_PLANKS);
        world.getBlockAt(px + 2, py + 3, pz - 7).setType(Material.DARK_OAK_PLANKS);

        // ── Story 1 windows (glass pane, 2 per wall) ─────────────────────────
        // Front (flanking entrance)
        world.getBlockAt(px - 3, py + 2, pz - 7).setType(Material.GLASS_PANE);
        world.getBlockAt(px - 3, py + 3, pz - 7).setType(Material.GLASS_PANE);
        world.getBlockAt(px + 4, py + 2, pz - 7).setType(Material.GLASS_PANE);
        world.getBlockAt(px + 4, py + 3, pz - 7).setType(Material.GLASS_PANE);
        // Back
        world.getBlockAt(px - 2, py + 2, pz + 7).setType(Material.GLASS_PANE);
        world.getBlockAt(px - 2, py + 3, pz + 7).setType(Material.GLASS_PANE);
        world.getBlockAt(px + 3, py + 2, pz + 7).setType(Material.GLASS_PANE);
        world.getBlockAt(px + 3, py + 3, pz + 7).setType(Material.GLASS_PANE);
        // Left side
        world.getBlockAt(px - 5, py + 2, pz - 3).setType(Material.GLASS_PANE);
        world.getBlockAt(px - 5, py + 3, pz - 3).setType(Material.GLASS_PANE);
        world.getBlockAt(px - 5, py + 2, pz + 3).setType(Material.GLASS_PANE);
        world.getBlockAt(px - 5, py + 3, pz + 3).setType(Material.GLASS_PANE);
        // Right side
        world.getBlockAt(px + 6, py + 2, pz - 3).setType(Material.GLASS_PANE);
        world.getBlockAt(px + 6, py + 3, pz - 3).setType(Material.GLASS_PANE);
        world.getBlockAt(px + 6, py + 2, pz + 3).setType(Material.GLASS_PANE);
        world.getBlockAt(px + 6, py + 3, pz + 3).setType(Material.GLASS_PANE);

        // ── Interior ground-floor room divider (entrance hall vs. rear rooms) ─
        for (int x = -4; x <= 5; x++) {
            for (int y = 1; y <= 3; y++) {
                world.getBlockAt(px + x, py + y, pz - 2).setType(Material.OAK_PLANKS);
            }
        }
        // Door in the divider wall (replaces plank blocks at the chosen opening)
        placeDoor(world, Material.OAK_DOOR, px + 1, py + 1, pz - 2, BlockFace.SOUTH);

        // ── Inter-floor platform at y=5 (oak planks ceiling/upper floor) ──────
        for (int x = -4; x <= 5; x++) {
            for (int z = -6; z <= 6; z++) {
                world.getBlockAt(px + x, py + 5, pz + z).setType(Material.OAK_PLANKS);
            }
        }

        // ── Story 2 walls (y 6–8): spruce planks ─────────────────────────────
        for (int y = 6; y <= 8; y++) {
            for (int x = -5; x <= 6; x++) {
                world.getBlockAt(px + x, py + y, pz - 7).setType(Material.SPRUCE_PLANKS);
                world.getBlockAt(px + x, py + y, pz + 7).setType(Material.SPRUCE_PLANKS);
            }
            for (int z = -6; z <= 6; z++) {
                world.getBlockAt(px - 5, py + y, pz + z).setType(Material.SPRUCE_PLANKS);
                world.getBlockAt(px + 6, py + y, pz + z).setType(Material.SPRUCE_PLANKS);
            }
        }

        // Corner pillars – dark oak log (story 2)
        for (int y = 6; y <= 8; y++) {
            world.getBlockAt(px - 5, py + y, pz - 7).setType(Material.DARK_OAK_LOG);
            world.getBlockAt(px + 6, py + y, pz - 7).setType(Material.DARK_OAK_LOG);
            world.getBlockAt(px - 5, py + y, pz + 7).setType(Material.DARK_OAK_LOG);
            world.getBlockAt(px + 6, py + y, pz + 7).setType(Material.DARK_OAK_LOG);
        }

        // ── Story 2 windows ───────────────────────────────────────────────────
        // Front (8 window blocks in 4 double-height pairs: 2 flanking + 2 centre)
        world.getBlockAt(px - 3, py + 7, pz - 7).setType(Material.GLASS_PANE);
        world.getBlockAt(px - 3, py + 8, pz - 7).setType(Material.GLASS_PANE);
        world.getBlockAt(px + 4, py + 7, pz - 7).setType(Material.GLASS_PANE);
        world.getBlockAt(px + 4, py + 8, pz - 7).setType(Material.GLASS_PANE);
        world.getBlockAt(px,     py + 7, pz - 7).setType(Material.GLASS_PANE);
        world.getBlockAt(px,     py + 8, pz - 7).setType(Material.GLASS_PANE);
        world.getBlockAt(px + 1, py + 7, pz - 7).setType(Material.GLASS_PANE);
        world.getBlockAt(px + 1, py + 8, pz - 7).setType(Material.GLASS_PANE);
        // Back
        world.getBlockAt(px - 2, py + 7, pz + 7).setType(Material.GLASS_PANE);
        world.getBlockAt(px - 2, py + 8, pz + 7).setType(Material.GLASS_PANE);
        world.getBlockAt(px + 3, py + 7, pz + 7).setType(Material.GLASS_PANE);
        world.getBlockAt(px + 3, py + 8, pz + 7).setType(Material.GLASS_PANE);
        // Left side
        world.getBlockAt(px - 5, py + 7, pz - 2).setType(Material.GLASS_PANE);
        world.getBlockAt(px - 5, py + 8, pz - 2).setType(Material.GLASS_PANE);
        world.getBlockAt(px - 5, py + 7, pz + 2).setType(Material.GLASS_PANE);
        world.getBlockAt(px - 5, py + 8, pz + 2).setType(Material.GLASS_PANE);
        // Right side
        world.getBlockAt(px + 6, py + 7, pz - 2).setType(Material.GLASS_PANE);
        world.getBlockAt(px + 6, py + 8, pz - 2).setType(Material.GLASS_PANE);
        world.getBlockAt(px + 6, py + 7, pz + 2).setType(Material.GLASS_PANE);
        world.getBlockAt(px + 6, py + 8, pz + 2).setType(Material.GLASS_PANE);

        // ── Pitched roof: 4 stepped layers of dark oak planks ────────────────
        // y=9: full footprint (12×15)
        for (int x = -5; x <= 6; x++) {
            for (int z = -7; z <= 7; z++) {
                world.getBlockAt(px + x, py + 9, pz + z).setType(Material.DARK_OAK_PLANKS);
            }
        }
        // y=10: inset 1 block on each X side
        for (int x = -4; x <= 5; x++) {
            for (int z = -7; z <= 7; z++) {
                world.getBlockAt(px + x, py + 10, pz + z).setType(Material.DARK_OAK_PLANKS);
            }
        }
        // y=11: inset 2 blocks on each X side
        for (int x = -3; x <= 4; x++) {
            for (int z = -7; z <= 7; z++) {
                world.getBlockAt(px + x, py + 11, pz + z).setType(Material.DARK_OAK_PLANKS);
            }
        }
        // y=12: ridge (6 blocks wide)
        for (int x = -2; x <= 3; x++) {
            for (int z = -7; z <= 7; z++) {
                world.getBlockAt(px + x, py + 12, pz + z).setType(Material.DARK_OAK_PLANKS);
            }
        }

        // ── Front porch ───────────────────────────────────────────────────────
        // Porch platform (stone bricks, one block in front of the building)
        for (int x = -1; x <= 2; x++) {
            world.getBlockAt(px + x, py, pz - 8).setType(Material.STONE_BRICKS);
        }
        // Porch pillars – dark oak log (flanking the entrance)
        for (int y = 1; y <= 4; y++) {
            world.getBlockAt(px - 2, py + y, pz - 8).setType(Material.DARK_OAK_LOG);
            world.getBlockAt(px + 3, py + y, pz - 8).setType(Material.DARK_OAK_LOG);
        }
        // Porch canopy connecting pillars to the building wall
        for (int x = -2; x <= 3; x++) {
            world.getBlockAt(px + x, py + 5, pz - 8).setType(Material.DARK_OAK_PLANKS);
            world.getBlockAt(px + x, py + 5, pz - 7).setType(Material.DARK_OAK_PLANKS);
        }
        // Decorative fence railings on porch sides
        world.getBlockAt(px - 1, py + 1, pz - 8).setType(Material.OAK_FENCE);
        world.getBlockAt(px + 2, py + 1, pz - 8).setType(Material.OAK_FENCE);
    }

    // ── Resolvers ────────────────────────────────────────────────────────────

    private void placeDoor(World world, Material doorType, int x, int y, int z, BlockFace facing) {
        Block lower = world.getBlockAt(x, y, z);
        lower.setType(doorType, false);
        Door lowerData = (Door) lower.getBlockData();
        lowerData.setFacing(facing);
        lowerData.setHalf(Bisected.Half.LOWER);
        lower.setBlockData(lowerData);

        Block upper = world.getBlockAt(x, y + 1, z);
        upper.setType(doorType, false);
        Door upperData = (Door) upper.getBlockData();
        upperData.setFacing(facing);
        upperData.setHalf(Bisected.Half.UPPER);
        upper.setBlockData(upperData);
    }

    private void placeGate(World world, int x, int y, int z, BlockFace facing) {
        Block gate = world.getBlockAt(x, y, z);
        gate.setType(Material.OAK_FENCE_GATE, false);
        Gate gateData = (Gate) gate.getBlockData();
        gateData.setFacing(facing);
        gate.setBlockData(gateData);
    }

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
