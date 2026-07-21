package com.minecraft.ai;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.trait.SkinTrait;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

/**
 * Creates a player NPC via Citizens with Steve's skin and the nick {@code AI_bot}.
 *
 * <p>This class requires Citizens to be installed on the server.
 * If Citizens is absent the spawn / despawn calls are no-ops and a
 * warning is logged.
 */
public class FakePlayerNPC {

    private static final Logger LOGGER = Logger.getLogger("FakePlayerNPC");

    private final JavaPlugin plugin;
    /** Display name shown above the entity. */
    private final String npcName;

    private NPC npc;
    private boolean spawned = false;

    public FakePlayerNPC(JavaPlugin plugin, String npcName) {
        this.plugin  = plugin;
        this.npcName = npcName;
    }

    // ── Public API ─────────────────────────────────────────────────────────────

    public boolean isSpawned() { return spawned; }

    /**
     * Spawns the NPC next to {@code player}.
     */
    public void spawn(Player nearPlayer) {
        if (spawned) return;
        if (!isCitizensAvailable()) {
            LOGGER.warning("Citizens не установлен — FakePlayerNPC недоступен.");
            return;
        }

        Location loc = nearPlayer.getLocation().clone().add(2, 0, 2);
        loc.setY(nearPlayer.getWorld().getHighestBlockYAt(loc) + 1);

        NPCRegistry registry = CitizensAPI.getNPCRegistry();
        npc = registry.createNPC(EntityType.PLAYER, npcName);

        // Apply Steve's default skin via the SkinTrait
        SkinTrait skinTrait = npc.getOrAddTrait(SkinTrait.class);
        skinTrait.setSkinName("Steve");

        npc.spawn(loc);
        spawned = true;
        LOGGER.info("FakePlayerNPC '" + npcName + "' заспавнен рядом с " + nearPlayer.getName());
    }

    /**
     * Despawns and destroys the NPC.
     */
    public void despawn() {
        if (!spawned || npc == null) return;
        npc.destroy();
        npc = null;
        spawned = false;
        LOGGER.info("FakePlayerNPC '" + npcName + "' удалён.");
    }

    // ── Utility ────────────────────────────────────────────────────────────────

    private static boolean isCitizensAvailable() {
        return Bukkit.getPluginManager().getPlugin("Citizens") != null;
    }
}
