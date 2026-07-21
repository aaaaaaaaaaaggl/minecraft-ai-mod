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
 * Manages the AI wooden sword item lifecycle.
 *
 * <ul>
 *   <li>Gives the wooden sword to each player on their <em>first</em> join.</li>
 *   <li>Gives it again whenever the player respawns after death.</li>
 *   <li>Opens the {@link AIBotMenu} on a right-click with the wooden sword.</li>
 * </ul>
 *
 * <p>The set of players who have already received their first wooden sword is
 * persisted in {@code data/ai_item_tracking.yml}.
 */
public class WoodenSwordListener implements Listener {

    private static final Logger LOGGER = Logger.getLogger("WoodenSwordListener");
    private static final String CFG_GIVE_ON_FIRST_JOIN = "ai-bot.give-wooden-sword-on-first-join";
    private static final String CFG_GIVE_ON_RESPAWN = "ai-bot.give-wooden-sword-on-respawn";

    /** Display name used to identify our wooden sword. */
    static final String WOODEN_SWORD_NAME = "§r§fAI";

    private final JavaPlugin   plugin;
    private final MenuListener menuListener;

    private final File              dataFile;
    private final FileConfiguration data;

    public WoodenSwordListener(JavaPlugin plugin, MenuListener menuListener) {
        this.plugin       = plugin;
        this.menuListener = menuListener;

        dataFile = new File(plugin.getDataFolder(), "ai_item_tracking.yml");
        if (!dataFile.exists()) {
            File parent = dataFile.getParentFile();
            if (!parent.exists() && !parent.mkdirs()) {
                LOGGER.warning("Could not create data directory: " + parent.getAbsolutePath());
            }
            try { dataFile.createNewFile(); } catch (IOException e) {
                LOGGER.warning("Could not create ai_item_tracking.yml: " + e.getMessage());
            }
        }
        data = YamlConfiguration.loadConfiguration(dataFile);
    }

    // ── Events ─────────────────────────────────────────────────────────────────

    /**
     * Give the wooden sword the very first time a player joins.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!plugin.getConfig().getBoolean(CFG_GIVE_ON_FIRST_JOIN, true)) return;

        Player player = event.getPlayer();
        if (!hasReceivedWoodenSword(player.getUniqueId())) {
            giveWoodenSword(player);
            markReceived(player.getUniqueId());
        }
    }

    /**
     * Give the wooden sword again every time a player respawns after death.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (!plugin.getConfig().getBoolean(CFG_GIVE_ON_RESPAWN, true)) return;

        Player player = event.getPlayer();
        // Delay by 1 tick so the inventory is fully restored before we add the item
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> giveWoodenSword(player));
    }

    /**
     * Open the menu when the player right-clicks with the AI wooden sword.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        if (!isAIWoodenSword(item)) return;

        event.setCancelled(true);
        menuListener.openMenu(event.getPlayer());
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /**
     * Add one AI wooden sword to the player's inventory (first available slot).
     * If the inventory is full the wooden sword is dropped at their feet.
     */
    public static void giveWoodenSword(Player player) {
        ItemStack woodenSword = AIBotMenu.makeAIWoodenSword();
        java.util.HashMap<Integer, ItemStack> leftovers =
                player.getInventory().addItem(woodenSword);
        if (!leftovers.isEmpty()) {
            // Drop at feet if no space
            player.getWorld().dropItem(player.getLocation(), woodenSword);
        }
        LOGGER.fine("Выдан деревянный меч AI для " + player.getName());
    }

    /**
     * Returns {@code true} if {@code item} is the AI wooden sword
     * (wooden sword material with display name matching {@link #WOODEN_SWORD_NAME}).
     */
    public static boolean isAIWoodenSword(ItemStack item) {
        if (item == null || item.getType() != Material.WOODEN_SWORD) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && WOODEN_SWORD_NAME.equals(meta.getDisplayName());
    }

    // ── Persistence ────────────────────────────────────────────────────────────

    private boolean hasReceivedWoodenSword(UUID uuid) {
        return data.getBoolean(uuid.toString(), false);
    }

    private void markReceived(UUID uuid) {
        data.set(uuid.toString(), true);
        try {
            data.save(dataFile);
        } catch (IOException e) {
            LOGGER.warning("Could not save ai_item_tracking.yml: " + e.getMessage());
        }
    }
}
