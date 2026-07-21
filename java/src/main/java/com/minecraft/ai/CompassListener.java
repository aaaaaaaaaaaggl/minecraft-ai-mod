package com.minecraft.ai;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Manages the AI compass item lifecycle.
 *
 * <ul>
 *   <li>Gives the compass to each player on their <em>first</em> join.
 *   <li>Gives it again whenever the player respawns after death.</li>
 *   <li>Opens the {@link AIBotMenu} on a right-click with the compass.</li>
 * </ul>
 *
 * <p>The set of players who have already received their first compass is
 * persisted in {@code data/compass_players.yml}.
 */
public class CompassListener implements Listener {

    private static final Logger LOGGER = Logger.getLogger("CompassListener");

    /** Display name used to identify our compass. */
    static final String COMPASS_NAME = "§r§fAI";

    private final JavaPlugin   plugin;
    private final MenuListener menuListener;

    private final File              dataFile;
    private final FileConfiguration data;

    public CompassListener(JavaPlugin plugin, MenuListener menuListener) {
        this.plugin       = plugin;
        this.menuListener = menuListener;

        dataFile = new File(plugin.getDataFolder(), "compass_players.yml");
        if (!dataFile.exists()) {
            File parent = dataFile.getParentFile();
            if (!parent.exists() && !parent.mkdirs()) {
                LOGGER.warning("Could not create data directory: " + parent.getAbsolutePath());
            }
            try { dataFile.createNewFile(); } catch (IOException e) {
                LOGGER.warning("Could not create compass_players.yml: " + e.getMessage());
            }
        }
        data = YamlConfiguration.loadConfiguration(dataFile);
    }

    // ── Events ─────────────────────────────────────────────────────────────────

    /**
     * Give the compass the very first time a player joins.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!hasReceivedCompass(player.getUniqueId())) {
            giveCompass(player);
            markReceived(player.getUniqueId());
        }
    }

    /**
     * Give the compass again every time a player respawns after death.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        // Delay by 1 tick so the inventory is fully restored before we add the item
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> giveCompass(player));
    }

    /**
     * Open the menu when the player right-clicks with the AI compass.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        if (!isAICompass(item)) return;

        event.setCancelled(true);
        menuListener.openMenu(event.getPlayer());
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /**
     * Add one AI compass to the player's inventory (first available slot).
     * If the inventory is full the compass is dropped at their feet.
     */
    public static void giveCompass(Player player) {
        ItemStack compass = AIBotMenu.makeCompass();
        java.util.HashMap<Integer, ItemStack> leftovers =
                player.getInventory().addItem(compass);
        if (!leftovers.isEmpty()) {
            // Drop at feet if no space
            player.getWorld().dropItem(player.getLocation(), compass);
        }
        LOGGER.fine("Выдан компас AI для " + player.getName());
    }

    /**
     * Returns {@code true} if {@code item} is the AI compass (compass material
     * with display name matching {@link #COMPASS_NAME}).
     */
    public static boolean isAICompass(ItemStack item) {
        if (item == null || item.getType() != Material.COMPASS) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && COMPASS_NAME.equals(meta.getDisplayName());
    }

    // ── Persistence ────────────────────────────────────────────────────────────

    private boolean hasReceivedCompass(UUID uuid) {
        return data.getBoolean(uuid.toString(), false);
    }

    private void markReceived(UUID uuid) {
        data.set(uuid.toString(), true);
        try {
            data.save(dataFile);
        } catch (IOException e) {
            LOGGER.warning("Could not save compass_players.yml: " + e.getMessage());
        }
    }
}
