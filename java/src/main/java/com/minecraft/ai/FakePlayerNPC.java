package com.minecraft.ai;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.trait.SkinTrait;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collection;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Creates a player NPC via Citizens with Steve's skin and the nick {@code AI_bot}.
 *
 * <p>This class requires Citizens to be installed on the server.
 * If Citizens is absent the spawn / despawn calls are no-ops and a
 * warning is logged.
 */
public class FakePlayerNPC {
    private static final long ATTACK_INTERVAL_TICKS = 10L;
    private static final long SETTINGS_REFRESH_INTERVAL_TICKS = 10L;
    private static final long TARGET_SCAN_INTERVAL_TICKS = 4L;
    private static final double FOLLOW_START_DISTANCE = 3.0;
    private static final double FOLLOW_STOP_DISTANCE = 2.0;
    private static final double MELEE_ATTACK_DISTANCE = 2.5;

    private static final Logger LOGGER = Logger.getLogger("FakePlayerNPC");

    private final JavaPlugin plugin;
    /** Display name shown above the entity. */
    private final String npcName;

    private NPC npc;
    private boolean spawned = false;
    private BukkitTask behaviorTask;
    private UUID controllerUuid;
    private UUID followTargetUuid;
    private long attackCooldownTicks;
    private long settingsReloadCooldownTicks;
    private long targetScanCooldownTicks;
    private LivingEntity cachedAttackTarget;
    private AIBotSettings cachedSettings;

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

        // Apply Steve's default skin via the SkinTrait.
        // setSkinName fetches the skin from Mojang's API using the Minecraft
        // account name — requires internet access on the game server.
        SkinTrait skinTrait = npc.getOrAddTrait(SkinTrait.class);
        skinTrait.setSkinName("Steve");

        npc.spawn(loc);
        spawned = true;
        resetBehaviorState();
        controllerUuid = nearPlayer.getUniqueId();
        startBehaviorTask();
        LOGGER.info("FakePlayerNPC '" + npcName + "' заспавнен рядом с " + nearPlayer.getName());
    }

    /**
     * Despawns and destroys the NPC.
     */
    public void despawn() {
        if (!spawned || npc == null) return;
        if (npc.isSpawned()) {
            npc.despawn();
        }
        if (behaviorTask != null) {
            behaviorTask.cancel();
            behaviorTask = null;
        }
        npc.destroy();
        npc = null;
        spawned = false;
        resetBehaviorState();
        LOGGER.info("FakePlayerNPC '" + npcName + "' удалён.");
    }

    public void startFollowing(Player player) {
        if (!spawned || npc == null) return;
        controllerUuid = player.getUniqueId();
        followTargetUuid = player.getUniqueId();
        cachedSettings = null;
        settingsReloadCooldownTicks = 0L;
    }

    public void stopFollowing() {
        followTargetUuid = null;
        if (spawned && npc != null && npc.getNavigator().isNavigating()) {
            npc.getNavigator().cancelNavigation();
        }
    }

    // ── Utility ────────────────────────────────────────────────────────────────

    private void startBehaviorTask() {
        if (behaviorTask != null) {
            behaviorTask.cancel();
        }
        behaviorTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickBehavior, 1L, 1L);
    }

    private void tickBehavior() {
        if (!spawned || npc == null || !npc.isSpawned() || !(npc.getEntity() instanceof LivingEntity)) {
            return;
        }

        LivingEntity npcEntity = (LivingEntity) npc.getEntity();
        if (!npcEntity.isValid() || npcEntity.isDead()) {
            return;
        }

        if (attackCooldownTicks > 0L) {
            attackCooldownTicks--;
        }
        if (settingsReloadCooldownTicks > 0L) {
            settingsReloadCooldownTicks--;
        }
        if (targetScanCooldownTicks > 0L) {
            targetScanCooldownTicks--;
        }

        AIBotSettings settings = getCurrentSettings();
        LivingEntity target = resolveAttackTarget(npcEntity, settings);
        if (target != null) {
            handleAttackTarget(npcEntity, target, settings);
            return;
        }

        handleFollowTarget(npcEntity);
    }

    private LivingEntity resolveAttackTarget(LivingEntity npcEntity, AIBotSettings settings) {
        if (!isAttackTargetValid(npcEntity, settings, cachedAttackTarget)) {
            cachedAttackTarget = null;
        }
        if (targetScanCooldownTicks <= 0L) {
            cachedAttackTarget = findAttackTarget(npcEntity, settings);
            targetScanCooldownTicks = TARGET_SCAN_INTERVAL_TICKS;
        }
        return cachedAttackTarget;
    }

    private LivingEntity findAttackTarget(LivingEntity npcEntity, AIBotSettings settings) {
        if (settings == null || (!settings.isAttackMobs() && !settings.isAttackPlayers())) {
            return null;
        }

        double range = settings.getAttackRange();
        double maxDistanceSquared = range * range;
        Collection<Entity> nearby = npcEntity.getNearbyEntities(range, range, range);

        LivingEntity nearest = null;
        double nearestDistanceSquared = Double.MAX_VALUE;

        for (Entity entity : nearby) {
            if (!(entity instanceof LivingEntity) || entity.equals(npcEntity)) {
                continue;
            }

            LivingEntity candidate = (LivingEntity) entity;
            if (!candidate.isValid() || candidate.isDead() || !npcEntity.hasLineOfSight(candidate)) {
                continue;
            }

            if (candidate instanceof Player) {
                if (!settings.isAttackPlayers()) {
                    continue;
                }

                Player targetPlayer = (Player) candidate;
                if ((controllerUuid != null && controllerUuid.equals(targetPlayer.getUniqueId()))
                        || (followTargetUuid != null && followTargetUuid.equals(targetPlayer.getUniqueId()))) {
                    continue;
                }
            } else if (candidate instanceof Mob) {
                if (!settings.isAttackMobs()) {
                    continue;
                }
            } else {
                continue;
            }

            double distanceSquared = npcEntity.getLocation().distanceSquared(candidate.getLocation());
            if (distanceSquared <= maxDistanceSquared && distanceSquared < nearestDistanceSquared) {
                nearest = candidate;
                nearestDistanceSquared = distanceSquared;
            }
        }

        return nearest;
    }

    private boolean isAttackTargetValid(LivingEntity npcEntity, AIBotSettings settings, LivingEntity candidate) {
        if (candidate == null || settings == null || candidate.isDead() || !candidate.isValid()) {
            return false;
        }
        if (!npcEntity.getWorld().equals(candidate.getWorld()) || !npcEntity.hasLineOfSight(candidate)) {
            return false;
        }

        double maxDistanceSquared = settings.getAttackRange() * settings.getAttackRange();
        if (npcEntity.getLocation().distanceSquared(candidate.getLocation()) > maxDistanceSquared) {
            return false;
        }

        if (candidate instanceof Player) {
            if (!settings.isAttackPlayers()) {
                return false;
            }
            UUID targetUuid = ((Player) candidate).getUniqueId();
            return !targetUuid.equals(controllerUuid) && !targetUuid.equals(followTargetUuid);
        }

        return candidate instanceof Mob && settings.isAttackMobs();
    }

    private void handleAttackTarget(LivingEntity npcEntity, LivingEntity target, AIBotSettings settings) {
        double distanceSquared = npcEntity.getLocation().distanceSquared(target.getLocation());
        double meleeDistanceSquared = MELEE_ATTACK_DISTANCE * MELEE_ATTACK_DISTANCE;

        if (distanceSquared > meleeDistanceSquared) {
            npc.getNavigator().setTarget(target, false);
            return;
        }

        if (attackCooldownTicks > 0L) {
            return;
        }

        if (npc.getNavigator().isNavigating()) {
            npc.getNavigator().cancelNavigation();
        }
        target.damage(settings.getAttackDamage(), npcEntity);
        attackCooldownTicks = ATTACK_INTERVAL_TICKS;
    }

    private void handleFollowTarget(LivingEntity npcEntity) {
        if (followTargetUuid == null) {
            if (npc.getNavigator().isNavigating()) {
                npc.getNavigator().cancelNavigation();
            }
            return;
        }

        Player followTarget = Bukkit.getPlayer(followTargetUuid);
        if (followTarget == null || !followTarget.isOnline() || !followTarget.getWorld().equals(npcEntity.getWorld())) {
            stopFollowing();
            return;
        }

        double distanceSquared = npcEntity.getLocation().distanceSquared(followTarget.getLocation());
        if (distanceSquared > FOLLOW_START_DISTANCE * FOLLOW_START_DISTANCE) {
            npc.getNavigator().setTarget(followTarget, false);
        } else if (distanceSquared <= FOLLOW_STOP_DISTANCE * FOLLOW_STOP_DISTANCE
                && npc.getNavigator().isNavigating()) {
            npc.getNavigator().cancelNavigation();
        }
    }

    private AIBotSettings getCurrentSettings() {
        if (controllerUuid == null) {
            return null;
        }
        if (cachedSettings == null || settingsReloadCooldownTicks <= 0L) {
            cachedSettings = AIBotSettings.load(controllerUuid);
            settingsReloadCooldownTicks = SETTINGS_REFRESH_INTERVAL_TICKS;
        }
        return cachedSettings;
    }

    private void resetBehaviorState() {
        controllerUuid = null;
        followTargetUuid = null;
        attackCooldownTicks = 0L;
        settingsReloadCooldownTicks = 0L;
        targetScanCooldownTicks = 0L;
        cachedAttackTarget = null;
        cachedSettings = null;
    }

    private static boolean isCitizensAvailable() {
        return Bukkit.getPluginManager().getPlugin("Citizens") != null;
    }
}
