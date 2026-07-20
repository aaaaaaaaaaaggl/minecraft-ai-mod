package com.minecraft.ai;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.type.Gate;
import org.bukkit.block.data.type.Stairs;
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
        buildStructure(player, structureType, player.getLocation().clone());
    }

    /**
     * Build a structure at a specific origin location.
     *
     * @param player        the requesting player (receives feedback message)
     * @param structureType "house", "tower", "bridge", or "mansion" (case-insensitive)
     * @param origin        where to build
     */
    public void buildStructure(Player player, String structureType, Location origin) {
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
                player.sendMessage("§a🏰 Особняк построен!");
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

    // ── Mansion builder ───────────────────────────────────────────────────────

    /**
     * Build a large two-storey mansion with a pitched dark-oak roof,
     * mixed stone-brick / oak-plank / spruce-plank walls, glass-pane windows,
     * a front porch, and interior torch lighting.
     *
     * Footprint: 15 wide (x: 0..14) × 12 deep (z: 0..11), ~13 blocks high.
     */
    private void buildMansion(Location origin) {
        World world = origin.getWorld();
        int px = origin.getBlockX();
        int py = origin.getBlockY();
        int pz = origin.getBlockZ();

        final int W = 15; // width  (x: 0 .. W-1)
        final int D = 12; // depth  (z: 0 .. D-1)

        // ── Foundation (y = py) ────────────────────────────────────────────
        fillRect(world, px, py, pz, W, D, Material.STONE_BRICKS);

        // ── First-floor interior floor (y = py+1) ─────────────────────────
        fillRect(world, px + 1, py + 1, pz + 1, W - 2, D - 2, Material.OAK_PLANKS);

        // ── First-floor walls  (y = py+1 … py+4, height = 4) ─────────────
        buildMansionWalls(world, px, py + 1, pz, W, 4, D, Material.OAK_PLANKS, Material.OAK_LOG);

        // ── Second-floor base / first-floor ceiling (y = py+5) ───────────
        fillRect(world, px + 1, py + 5, pz + 1, W - 2, D - 2, Material.OAK_PLANKS);

        // ── Second-floor walls (y = py+5 … py+8, height = 4) ─────────────
        buildMansionWalls(world, px, py + 5, pz, W, 4, D, Material.SPRUCE_PLANKS, Material.SPRUCE_LOG);

        // ── Pitched roof (starts at y = py+9) ─────────────────────────────
        buildMansionRoof(world, px, py + 9, pz, W, D);

        // ── Front door (centre of north wall, z = pz) ─────────────────────
        int doorX = px + W / 2; // px+7 for W=15
        world.getBlockAt(doorX, py, pz).setType(Material.AIR, false);
        placeDoor(world, Material.OAK_DOOR, doorX, py + 1, pz, BlockFace.SOUTH);

        // ── Windows on all four walls ──────────────────────────────────────
        placeMansionWindows(world, px, py, pz, W, D, doorX - px);

        // ── Interior torch lighting ────────────────────────────────────────
        world.getBlockAt(px + 2, py + 2, pz + 2).setType(Material.TORCH);
        world.getBlockAt(px + W - 3, py + 2, pz + 2).setType(Material.TORCH);
        world.getBlockAt(px + 2, py + 2, pz + D - 3).setType(Material.TORCH);
        world.getBlockAt(px + W - 3, py + 2, pz + D - 3).setType(Material.TORCH);
        world.getBlockAt(px + 2, py + 7, pz + 2).setType(Material.TORCH);
        world.getBlockAt(px + W - 3, py + 7, pz + 2).setType(Material.TORCH);
        world.getBlockAt(px + 2, py + 7, pz + D - 3).setType(Material.TORCH);
        world.getBlockAt(px + W - 3, py + 7, pz + D - 3).setType(Material.TORCH);

        // ── Front porch columns (outside north wall) ───────────────────────
        int doorRelX = W / 2; // 7
        for (int y = 1; y <= 4; y++) {
            world.getBlockAt(px + doorRelX - 2, py + y, pz - 1).setType(Material.OAK_LOG);
            world.getBlockAt(px + doorRelX + 2, py + y, pz - 1).setType(Material.OAK_LOG);
        }
        // Porch lintel
        for (int x = doorRelX - 2; x <= doorRelX + 2; x++) {
            world.getBlockAt(px + x, py + 5, pz - 1).setType(Material.OAK_PLANKS);
        }
    }

    /**
     * Build the four exterior walls for one storey.
     * Corners and the midpoint column use {@code pillarMat}; everything else uses {@code wallMat}.
     */
    private void buildMansionWalls(World world, int px, int py, int pz,
                                   int W, int H, int D,
                                   Material wallMat, Material pillarMat) {
        // North wall  (z = pz)
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                Material m = (x == 0 || x == W - 1 || x == W / 2) ? pillarMat : wallMat;
                world.getBlockAt(px + x, py + y, pz).setType(m);
            }
        }
        // South wall  (z = pz + D - 1)
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                Material m = (x == 0 || x == W - 1 || x == W / 2) ? pillarMat : wallMat;
                world.getBlockAt(px + x, py + y, pz + D - 1).setType(m);
            }
        }
        // West wall  (x = px)
        for (int y = 0; y < H; y++) {
            for (int z = 1; z < D - 1; z++) {
                Material m = (z == D / 2) ? pillarMat : wallMat;
                world.getBlockAt(px, py + y, pz + z).setType(m);
            }
        }
        // East wall  (x = px + W - 1)
        for (int y = 0; y < H; y++) {
            for (int z = 1; z < D - 1; z++) {
                Material m = (z == D / 2) ? pillarMat : wallMat;
                world.getBlockAt(px + W - 1, py + y, pz + z).setType(m);
            }
        }
    }

    /**
     * Build a symmetrical pitched (gabled) dark-oak roof.
     * Ridge runs along the z-axis at x = W/2.
     * The roof rises 4 blocks above {@code py} at the ridge.
     */
    private void buildMansionRoof(World world, int px, int py, int pz, int W, int D) {
        int ridgeRelX = W / 2; // 7 for W=15

        // Pre-compute roof height for each x column (0-indexed)
        // heights[x] = blocks above py where the topmost roof block sits
        int[] heights = new int[W];
        for (int x = 0; x < W; x++) {
            int dist = Math.abs(x - ridgeRelX);
            heights[x] = Math.max(0, 4 - dist / 2);
        }

        for (int z = 0; z < D; z++) {
            for (int x = 0; x < W; x++) {
                int h = heights[x];

                // Gable-end fill (triangular wall at z=0 and z=D-1)
                if (z == 0 || z == D - 1) {
                    for (int y = 0; y < h; y++) {
                        world.getBlockAt(px + x, py + y, pz + z).setType(Material.DARK_OAK_PLANKS);
                    }
                }

                // Roof surface: ridge → planks; slopes → stairs facing inward
                if (x == ridgeRelX) {
                    world.getBlockAt(px + x, py + h, pz + z).setType(Material.DARK_OAK_PLANKS);
                } else {
                    Block stairBlock = world.getBlockAt(px + x, py + h, pz + z);
                    stairBlock.setType(Material.DARK_OAK_STAIRS, false);
                    Stairs sd = (Stairs) stairBlock.getBlockData();
                    // Left of ridge faces EAST (step rises toward centre)
                    // Right of ridge faces WEST
                    sd.setFacing(x < ridgeRelX ? BlockFace.EAST : BlockFace.WEST);
                    stairBlock.setBlockData(sd);
                }
            }
        }
    }

    /**
     * Add glass-pane windows to all four walls of a two-storey mansion.
     * Skips the door opening on the north wall.
     *
     * @param doorRelX door's x position relative to {@code px}
     */
    private void placeMansionWindows(World world, int px, int py, int pz,
                                     int W, int D, int doorRelX) {
        // First floor: y = py+2 and py+3
        // Second floor: y = py+6 and py+7
        int[] windowYOffsets = {2, 3, 6, 7};

        for (int dy : windowYOffsets) {
            // North wall  (z = pz) – skip 3-block door zone
            for (int x = 1; x < W - 1; x++) {
                if (Math.abs(x - doorRelX) <= 1) continue; // door opening
                if (x % 4 == 1 || x % 4 == 2) {
                    world.getBlockAt(px + x, py + dy, pz).setType(Material.GLASS_PANE);
                }
            }
            // South wall  (z = pz + D - 1)
            for (int x = 1; x < W - 1; x++) {
                if (x % 4 == 1 || x % 4 == 2) {
                    world.getBlockAt(px + x, py + dy, pz + D - 1).setType(Material.GLASS_PANE);
                }
            }
            // West wall  (x = px)
            for (int z = 1; z < D - 1; z++) {
                if (z % 4 == 1 || z % 4 == 2) {
                    world.getBlockAt(px, py + dy, pz + z).setType(Material.GLASS_PANE);
                }
            }
            // East wall  (x = px + W - 1)
            for (int z = 1; z < D - 1; z++) {
                if (z % 4 == 1 || z % 4 == 2) {
                    world.getBlockAt(px + W - 1, py + dy, pz + z).setType(Material.GLASS_PANE);
                }
            }
        }
    }

    /**
     * Fill a 2-D rectangular layer with a single material.
     * (Helper shared by mansion builders.)
     */
    private void fillRect(World world, int px, int py, int pz, int sizeX, int sizeZ, Material mat) {
        for (int x = 0; x < sizeX; x++) {
            for (int z = 0; z < sizeZ; z++) {
                world.getBlockAt(px + x, py, pz + z).setType(mat);
            }
        }
    }
}
