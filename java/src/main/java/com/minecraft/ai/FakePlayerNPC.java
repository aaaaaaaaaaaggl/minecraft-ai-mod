package com.minecraft.ai;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.WrappedSignedProperty;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Creates a fake player NPC via ProtocolLib with Steve's skin and the nick
 * {@code AI_bot}.
 *
 * <p>Packets sent (per observer player):
 * <ol>
 *   <li>{@code PLAYER_INFO_UPDATE} — adds the player to the tab list
 *       (required before the entity can be spawned).</li>
 *   <li>{@code NAMED_ENTITY_SPAWN} — spawns the fake entity at the given
 *       location.</li>
 *   <li>{@code ENTITY_HEAD_ROTATION} — sets the correct head rotation.</li>
 *   <li>After 1 second: {@code PLAYER_INFO_REMOVE} — removes the entry from
 *       the tab list while the visual entity remains.</li>
 * </ol>
 *
 * <p>This class requires ProtocolLib to be installed on the server.
 * If ProtocolLib is absent the spawn / despawn calls are no-ops and a
 * warning is logged.
 */
public class FakePlayerNPC {

    private static final Logger LOGGER = Logger.getLogger("FakePlayerNPC");

    /** Mojang UUID for the default Steve skin. */
    private static final UUID STEVE_UUID = UUID.fromString("8667ba71-b85a-4004-af54-457a9734eed7");

    /**
     * Base64-encoded texture JSON for Steve's default skin.
     * This is the unsigned variant — works on servers that do not enforce
     * secure profiles.
     */
    private static final String STEVE_TEXTURE =
            "ewogICJ0aW1lc3RhbXAiOiAxNjU5MDY0NDAxODcxLAogICJwcm9maWxlSWQiOiAi"
          + "ODY2N2JhNzFiODVhNDAwNGFmNTQ0NTdhOTczNGVlZDciLAogICJwcm9maWxlTmFt"
          + "ZSI6ICJTdGV2ZSIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIjogZmFsc2UsCiAgInRl"
          + "eHR1cmVzIjogewogICAgIlNLSU4iOiB7CiAgICAgICJ1cmwiOiAiaHR0cDovL3Rl"
          + "eHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9jMDZmODkwNjA3MjI0MjFlYWI0"
          + "MzhkNTEwZDRiZjhhNTc3YjQ1N2ZlZDMyMjY5ZTNhYjc1YTAyZjI0ZTBmMjciCiAg"
          + "ICB9CiAgfQp9";

    // Entity IDs must be unique per server session; start high to avoid clashes
    private static final AtomicInteger ENTITY_ID_COUNTER = new AtomicInteger(900_000);

    private final JavaPlugin plugin;

    /** Unique ID of the NPC as a Minecraft entity. */
    private final int    entityId;
    /** UUID used for this fake player profile. */
    private final UUID   npcUuid;
    /** Display name shown above the entity. */
    private final String npcName;

    private boolean spawned = false;

    public FakePlayerNPC(JavaPlugin plugin, String npcName) {
        this.plugin   = plugin;
        this.npcName  = npcName;
        this.entityId = ENTITY_ID_COUNTER.getAndIncrement();
        this.npcUuid  = UUID.randomUUID();
    }

    // ── Public API ─────────────────────────────────────────────────────────────

    public boolean isSpawned() { return spawned; }

    /**
     * Spawns the fake player next to {@code player}, sending packets to all
     * online players.
     */
    public void spawn(Player nearPlayer) {
        if (spawned) return;
        if (!isProtocolLibAvailable()) {
            LOGGER.warning("ProtocolLib не установлен — FakePlayerNPC недоступен.");
            return;
        }

        Location loc = nearPlayer.getLocation().clone().add(2, 0, 2);
        loc.setY(nearPlayer.getWorld().getHighestBlockYAt(loc) + 1);

        for (Player observer : Bukkit.getOnlinePlayers()) {
            sendSpawnPackets(observer, loc);
        }
        spawned = true;
        LOGGER.info("FakePlayerNPC '" + npcName + "' заспавнен рядом с " + nearPlayer.getName());
    }

    /**
     * Removes the fake player from all online players' views.
     */
    public void despawn() {
        if (!spawned) return;
        if (!isProtocolLibAvailable()) return;

        for (Player observer : Bukkit.getOnlinePlayers()) {
            sendDespawnPackets(observer);
        }
        spawned = false;
        LOGGER.info("FakePlayerNPC '" + npcName + "' удалён.");
    }

    // ── Packet senders ─────────────────────────────────────────────────────────

    private void sendSpawnPackets(Player observer, Location loc) {
        ProtocolManager manager = ProtocolLibrary.getProtocolManager();

        // 1. PLAYER_INFO_UPDATE — add player to tab list so the client can load skin
        sendPlayerInfoAdd(manager, observer);

        // 2. NAMED_ENTITY_SPAWN — display the entity
        sendNamedEntitySpawn(manager, observer, loc);

        // 3. ENTITY_HEAD_ROTATION
        sendHeadRotation(manager, observer, loc.getYaw());

        // 4. After 20 ticks (1 s) remove from tab list to keep tab clean
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (observer.isOnline()) {
                sendPlayerInfoRemove(manager, observer);
            }
        }, 20L);
    }

    private void sendDespawnPackets(Player observer) {
        ProtocolManager manager = ProtocolLibrary.getProtocolManager();

        // Remove entity via ENTITY_DESTROY
        try {
            PacketContainer packet = manager.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
            packet.getIntLists().write(0, Collections.singletonList(entityId));
            manager.sendServerPacket(observer, packet);
        } catch (InvocationTargetException e) {
            LOGGER.warning("Не удалось отправить ENTITY_DESTROY: " + e.getMessage());
        }

        // Also remove from tab list if still listed
        sendPlayerInfoRemove(manager, observer);
    }

    // ── Individual packet helpers ─────────────────────────────────────────────

    private void sendPlayerInfoAdd(ProtocolManager manager, Player observer) {
        try {
            PacketContainer packet =
                    manager.createPacket(PacketType.Play.Server.PLAYER_INFO_UPDATE);

            // Actions: ADD_PLAYER + SET_LISTED + UPDATE_GAME_MODE
            EnumSet<EnumWrappers.PlayerInfoAction> actions = EnumSet.of(
                    EnumWrappers.PlayerInfoAction.ADD_PLAYER,
                    EnumWrappers.PlayerInfoAction.SET_LISTED,
                    EnumWrappers.PlayerInfoAction.UPDATE_GAME_MODE);
            packet.getEnumSets(EnumWrappers.getPlayerInfoActionConverter()).write(0, actions);

            // Build the game profile with Steve's skin
            WrappedGameProfile profile = new WrappedGameProfile(npcUuid, npcName);
            profile.getProperties().put(
                    "textures",
                    WrappedSignedProperty.fromValues("textures", STEVE_TEXTURE, null));

            PlayerInfoData infoData = new PlayerInfoData(
                    npcUuid,
                    0,      // latency ms
                    true,   // listed in tab
                    EnumWrappers.NativeGameMode.SURVIVAL,
                    profile,
                    WrappedChatComponent.fromText(npcName),
                    null);  // no profile public key

            packet.getPlayerInfoDataLists().write(1,
                    Collections.singletonList(infoData));

            manager.sendServerPacket(observer, packet);
        } catch (Exception e) {
            LOGGER.warning("Не удалось отправить PLAYER_INFO_UPDATE (add): " + e.getMessage());
        }
    }

    private void sendPlayerInfoRemove(ProtocolManager manager, Player observer) {
        try {
            PacketContainer packet =
                    manager.createPacket(PacketType.Play.Server.PLAYER_INFO_REMOVE);
            // ProtocolLib 5.x exposes the UUID list for PLAYER_INFO_REMOVE
            packet.getUUIDLists().write(0, Collections.singletonList(npcUuid));
            manager.sendServerPacket(observer, packet);
        } catch (Exception e) {
            LOGGER.warning("Не удалось отправить PLAYER_INFO_REMOVE: " + e.getMessage());
        }
    }

    private void sendNamedEntitySpawn(ProtocolManager manager, Player observer, Location loc) {
        try {
            PacketContainer packet =
                    manager.createPacket(PacketType.Play.Server.NAMED_ENTITY_SPAWN);
            packet.getIntegers().write(0, entityId);
            packet.getUUIDs().write(0, npcUuid);
            packet.getDoubles().write(0, loc.getX());
            packet.getDoubles().write(1, loc.getY());
            packet.getDoubles().write(2, loc.getZ());
            // Encode yaw / pitch as a signed byte (256 units = 360°)
            packet.getBytes()
                  .write(0, (byte) Math.round(loc.getYaw()   / 360f * 256f));
            packet.getBytes()
                  .write(1, (byte) Math.round(loc.getPitch() / 360f * 256f));
            manager.sendServerPacket(observer, packet);
        } catch (InvocationTargetException e) {
            LOGGER.warning("Не удалось отправить NAMED_ENTITY_SPAWN: " + e.getMessage());
        }
    }

    private void sendHeadRotation(ProtocolManager manager, Player observer, float yaw) {
        try {
            PacketContainer packet =
                    manager.createPacket(PacketType.Play.Server.ENTITY_HEAD_ROTATION);
            packet.getIntegers().write(0, entityId);
            packet.getBytes().write(0, (byte) Math.round(yaw / 360f * 256f));
            manager.sendServerPacket(observer, packet);
        } catch (InvocationTargetException e) {
            LOGGER.warning("Не удалось отправить ENTITY_HEAD_ROTATION: " + e.getMessage());
        }
    }

    // ── Utility ────────────────────────────────────────────────────────────────

    private static boolean isProtocolLibAvailable() {
        return Bukkit.getPluginManager().getPlugin("ProtocolLib") != null;
    }
}
