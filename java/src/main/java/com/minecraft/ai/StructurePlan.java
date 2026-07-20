package com.minecraft.ai;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Defines a step-by-step block-placement plan for the VirtualPlayer.
 *
 * Each {@link BlockEntry} records the world location and target material
 * for one block.  Build methods on VirtualPlayer iterate through the list,
 * placing one block every few ticks so the construction looks animated.
 */
public class StructurePlan {

    /** One block in the plan: where to place it and what material to use. */
    public static class BlockEntry {
        public final Location location;
        public final Material material;

        public BlockEntry(Location location, Material material) {
            this.location = location;
            this.material = material;
        }
    }

    private final String name;
    private final List<BlockEntry> blocks = new ArrayList<>();

    public StructurePlan(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void addBlock(Location location, Material material) {
        blocks.add(new BlockEntry(location, material));
    }

    /** Returns an immutable view of the block list. */
    public List<BlockEntry> getBlocks() {
        return Collections.unmodifiableList(blocks);
    }

    // ── Predefined structure plans ───────────────────────────────────────────

    /**
     * A simple 5×5 oak-plank house (floor + 3-high walls + flat roof + windows).
     * The front wall has an opening left in place of a door so the build stays
     * stateless (no bisected-block data required).
     *
     * @param origin player / AI location used as centre of the structure
     */
    public static StructurePlan simpleHouse(Location origin) {
        StructurePlan plan = new StructurePlan("Домик");
        World w = origin.getWorld();
        int px = origin.getBlockX();
        int py = origin.getBlockY();
        int pz = origin.getBlockZ();

        // ── Floor ────────────────────────────────────────────────────────────
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                plan.addBlock(w.getBlockAt(px + x, py, pz + z).getLocation(),
                        Material.OAK_PLANKS);
            }
        }

        // ── Walls (y 1–3) ────────────────────────────────────────────────────
        for (int y = 1; y <= 3; y++) {
            for (int x = -2; x <= 2; x++) {
                // Front wall — leave center bottom two rows open (doorway)
                if (!(x == 0 && y <= 2)) {
                    plan.addBlock(w.getBlockAt(px + x, py + y, pz - 2).getLocation(),
                            Material.OAK_PLANKS);
                }
                plan.addBlock(w.getBlockAt(px + x, py + y, pz + 2).getLocation(),
                        Material.OAK_PLANKS);
            }
            for (int z = -1; z <= 1; z++) {
                plan.addBlock(w.getBlockAt(px - 2, py + y, pz + z).getLocation(),
                        Material.OAK_PLANKS);
                plan.addBlock(w.getBlockAt(px + 2, py + y, pz + z).getLocation(),
                        Material.OAK_PLANKS);
            }
        }

        // ── Windows ───────────────────────────────────────────────────────────
        plan.addBlock(w.getBlockAt(px - 1, py + 2, pz - 2).getLocation(), Material.GLASS);
        plan.addBlock(w.getBlockAt(px + 1, py + 2, pz - 2).getLocation(), Material.GLASS);
        plan.addBlock(w.getBlockAt(px - 1, py + 2, pz + 2).getLocation(), Material.GLASS);
        plan.addBlock(w.getBlockAt(px + 1, py + 2, pz + 2).getLocation(), Material.GLASS);

        // ── Flat roof ─────────────────────────────────────────────────────────
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                plan.addBlock(w.getBlockAt(px + x, py + 4, pz + z).getLocation(),
                        Material.OAK_PLANKS);
            }
        }

        return plan;
    }
}
