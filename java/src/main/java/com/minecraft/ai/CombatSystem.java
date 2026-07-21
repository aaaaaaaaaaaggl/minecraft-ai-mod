package com.minecraft.ai;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Система боя AI Bot: автоматически обнаруживает агрессивных мобов
 * в радиусе 20 блоков и атакует их каждые 10 тиков (0.5 сек).
 *
 * Все Bukkit-операции выполняются на главном потоке сервера.
 */
public class CombatSystem {

    /** Радиус обнаружения мобов (блоков). */
    private static final double DETECTION_RADIUS = 20.0;

    /** Урон за удар: 4.0 = 2 сердца. */
    private static final double DAMAGE_PER_HIT = 4.0;

    /** Интервал атак в тиках (10 тиков = 0.5 сек). */
    private static final long ATTACK_INTERVAL_TICKS = 10L;

    private static final Logger LOGGER = Logger.getLogger("CombatSystem");

    private final JavaPlugin plugin;

    /** Активные боевые задачи: UUID игрока → BukkitTask. */
    private final Map<UUID, BukkitTask> activeTasks = new ConcurrentHashMap<>();

    public CombatSystem(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Начать боевой режим для игрока.
     * Если режим уже активен — ничего не делает.
     *
     * @param player игрок, от имени которого наносится урон
     */
    public void startCombat(Player player) {
        UUID uuid = player.getUniqueId();
        if (activeTasks.containsKey(uuid)) {
            player.sendMessage("§e⚔️  Боевой режим уже активен!");
            return;
        }

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    stopCombat(player);
                    return;
                }
                attackNearbyMonsters(player);
            }
        }.runTaskTimer(plugin, 0L, ATTACK_INTERVAL_TICKS);

        activeTasks.put(uuid, task);
        player.sendMessage("§a⚔️  Боевой режим включён! Атакую агрессивных мобов в радиусе "
                + (int) DETECTION_RADIUS + " блоков.");
        LOGGER.info("Боевой режим включён для " + player.getName());
    }

    /**
     * Остановить боевой режим для игрока.
     *
     * @param player игрок
     */
    public void stopCombat(Player player) {
        UUID uuid = player.getUniqueId();
        BukkitTask task = activeTasks.remove(uuid);
        if (task != null) {
            task.cancel();
            if (player.isOnline()) {
                player.sendMessage("§c⚔️  Боевой режим отключён.");
            }
            LOGGER.info("Боевой режим отключён для " + player.getName());
        }
    }

    /**
     * Проверить, активен ли боевой режим у игрока.
     *
     * @param player игрок
     * @return true, если боевой режим активен
     */
    public boolean isInCombat(Player player) {
        return activeTasks.containsKey(player.getUniqueId());
    }

    /**
     * Найти ближайших агрессивных мобов и атаковать их.
     * Должен вызываться на главном потоке сервера.
     */
    private void attackNearbyMonsters(Player player) {
        Collection<Entity> nearby = player.getNearbyEntities(
                DETECTION_RADIUS, DETECTION_RADIUS, DETECTION_RADIUS);

        int attacked = 0;
        for (Entity entity : nearby) {
            if (entity instanceof Monster) {
                Monster monster = (Monster) entity;
                monster.damage(DAMAGE_PER_HIT, player);
                double healthLeft = monster.isDead() ? 0.0 : monster.getHealth();
                LOGGER.info("⚔️  " + player.getName() + " атаковал " + monster.getType().name()
                        + " (" + healthLeft + " HP осталось)");
                attacked++;
            }
        }

        if (attacked > 0) {
            player.sendMessage("§c⚔️  Атаковано мобов: " + attacked);
        }
    }
}
