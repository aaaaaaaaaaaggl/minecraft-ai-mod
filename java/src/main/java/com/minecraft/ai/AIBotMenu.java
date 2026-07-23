package com.minecraft.ai;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Chest GUI for controlling AI Bot parameters.
 *
 * <p>Layout (27 slots — 3 rows × 9 columns):
 * <pre>
 * Row 0: [RED]  [MOB]  [PLYR] [RANGE][DMG  ][SEP  ][ORG  ][ABLD ][BTYPE]
 * Row 1: [BARO ][SEP  ][BLUE ][SPWN ][DSPW ][HLTH ][SEP  ][SEP  ][SEP  ]
 * Row 2: [SEP  ][SEP  ][SEP  ][GRN  ][SPBT ][DSBT ][RST  ][SEP  ][SEP  ]
 * </pre>
 */
public class AIBotMenu implements InventoryHolder {

    public static final String TITLE = "§8⚙ §bAI Bot §8— §7Управление";

    // Slot indices
    public static final int SLOT_HEADER_COMBAT   = 0;
    public static final int SLOT_ATTACK_MOBS     = 1;
    public static final int SLOT_ATTACK_PLAYERS  = 2;
    public static final int SLOT_ATTACK_RANGE    = 3;
    public static final int SLOT_ATTACK_DAMAGE   = 4;
    public static final int SLOT_SEP_1           = 5;
    public static final int SLOT_HEADER_BUILD    = 6;
    public static final int SLOT_AUTO_BUILD      = 7;
    public static final int SLOT_BUILD_TYPE      = 8;

    public static final int SLOT_BUILD_AROUND    = 9;
    public static final int SLOT_SEP_2           = 10;
    public static final int SLOT_HEADER_BEHAVIOR = 11;
    public static final int SLOT_SPAWN_CMD       = 12;
    public static final int SLOT_DESPAWN_CMD     = 13;
    public static final int SLOT_HEALTH_INFO     = 14;
    public static final int SLOT_MOVEMENT_SPEED  = 15;
    public static final int SLOT_SEP_3           = 16;
    public static final int SLOT_SEP_4           = 17;

    public static final int SLOT_SEP_6           = 18;
    public static final int SLOT_SEP_7           = 19;
    public static final int SLOT_SEP_8           = 20;
    public static final int SLOT_HEADER_CONTROL  = 21;
    public static final int SLOT_SPAWN_BOT       = 22;
    public static final int SLOT_DESPAWN_BOT     = 23;
    public static final int SLOT_RESET_SETTINGS  = 24;
    public static final int SLOT_SEP_9           = 25;
    public static final int SLOT_SEP_10          = 26;

    private final Inventory inventory;
    private final UUID      playerUuid;
    private AIBotSettings   settings;

    public AIBotMenu(Player player) {
        this.playerUuid = player.getUniqueId();
        this.settings   = AIBotSettings.load(playerUuid);
        this.inventory  = Bukkit.createInventory(this, 27, TITLE);
        populate();
    }

    // ── InventoryHolder ───────────────────────────────────────────────────────

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public UUID getPlayerUuid() { return playerUuid; }

    public AIBotSettings getSettings() { return settings; }

    /**
     * Refresh the whole menu (re-populate every slot from current settings).
     */
    public void refresh() {
        settings = AIBotSettings.load(playerUuid);
        populate();
    }

    /**
     * Update just the slots that depend on {@code settings} (skips static items).
     */
    public void updateDynamic() {
        inventory.setItem(SLOT_ATTACK_MOBS,    makeAttackMobs());
        inventory.setItem(SLOT_ATTACK_PLAYERS, makeAttackPlayers());
        inventory.setItem(SLOT_ATTACK_RANGE,   makeAttackRange());
        inventory.setItem(SLOT_ATTACK_DAMAGE,  makeAttackDamage());
        inventory.setItem(SLOT_AUTO_BUILD,     makeAutoBuild());
        inventory.setItem(SLOT_BUILD_TYPE,     makeBuildType());
        inventory.setItem(SLOT_BUILD_AROUND,   makeBuildAround());
        inventory.setItem(SLOT_SPAWN_CMD,      makeSpawnCmd());
        inventory.setItem(SLOT_DESPAWN_CMD,    makeDespawnCmd());
        inventory.setItem(SLOT_HEALTH_INFO,    makeHealthInfo());
        inventory.setItem(SLOT_MOVEMENT_SPEED, makeMovementSpeed());
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void populate() {
        // ── Static headers ───────────────────────────────────────────────────
        inventory.setItem(SLOT_HEADER_COMBAT,   makeHeader(Material.RED_WOOL,
                "§c⚔ Боевые параметры",
                "§7Настройте параметры боя AI Bot"));

        inventory.setItem(SLOT_HEADER_BUILD,    makeHeader(Material.ORANGE_WOOL,
                "§6🏗 Построение",
                "§7Параметры автоматического строительства"));

        inventory.setItem(SLOT_HEADER_BEHAVIOR, makeHeader(Material.BLUE_WOOL,
                "§9🎭 Поведение",
                "§7События и состояние AI Bot"));

        inventory.setItem(SLOT_HEADER_CONTROL,  makeHeader(Material.GREEN_WOOL,
                "§a🎮 Управление",
                "§7Спавн, деспавн и сброс настроек"));

        // ── Separators ───────────────────────────────────────────────────────
        ItemStack sep = makeSeparator();
        for (int s : new int[]{SLOT_SEP_1, SLOT_SEP_2, SLOT_SEP_3, SLOT_SEP_4,
                               SLOT_SEP_6, SLOT_SEP_7, SLOT_SEP_8,
                               SLOT_SEP_9, SLOT_SEP_10}) {
            inventory.setItem(s, sep);
        }

        // ── Control buttons (static labels) ───────────────────────────────────
        inventory.setItem(SLOT_SPAWN_BOT,      makeSpawnBotButton());
        inventory.setItem(SLOT_DESPAWN_BOT,    makeDespawnBotButton());
        inventory.setItem(SLOT_RESET_SETTINGS, makeResetButton());

        // ── Dynamic items ─────────────────────────────────────────────────────
        updateDynamic();
    }

    // ── Item factories ────────────────────────────────────────────────────────

    private static ItemStack makeHeader(Material mat, String name, String loreText) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Collections.singletonList(loreText));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack makeSeparator() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§7");
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack makeAttackMobs() {
        boolean on = settings.isAttackMobs();
        ItemStack item = new ItemStack(Material.STONE_SWORD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§fАтака мобов: " + statusLabel(on));
            meta.setLore(Arrays.asList(
                    "§7Бот атакует агрессивных мобов",
                    "§7поблизости.",
                    "",
                    "§eЛевый клик §7— переключить"));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack makeAttackPlayers() {
        boolean on = settings.isAttackPlayers();
        ItemStack item = new ItemStack(Material.BONE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§fАтака игроков: " + statusLabel(on));
            meta.setLore(Arrays.asList(
                    "§7Бот атакует других игроков.",
                    "",
                    "§eЛевый клик §7— переключить"));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack makeAttackRange() {
        ItemStack item = new ItemStack(Material.BOW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§fДальность атаки: §e" + (int) settings.getAttackRange() + " блоков");
            meta.setLore(Arrays.asList(
                    "§7Радиус обнаружения целей.",
                    "§7Диапазон: 1–50 блоков.",
                    "",
                    "§aЛевый клик §7— +1",
                    "§cПравый клик §7— −1"));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack makeAttackDamage() {
        ItemStack item = new ItemStack(Material.IRON_AXE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§fСила удара: §e" + settings.getAttackDamage());
            meta.setLore(Arrays.asList(
                    "§7Урон за один удар.",
                    "§7Диапазон: 1.0–20.0.",
                    "",
                    "§aЛевый клик §7— +1",
                    "§cПравый клик §7— −1"));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack makeAutoBuild() {
        boolean on = settings.isAutoBuild();
        ItemStack item = new ItemStack(Material.OAK_PLANKS);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§fАвтостроительство: " + statusLabel(on));
            meta.setLore(Arrays.asList(
                    "§7Бот автоматически строит",
                    "§7выбранную структуру.",
                    "",
                    "§eЛевый клик §7— переключить"));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack makeBuildType() {
        String type = settings.getBuildType();
        ItemStack item = new ItemStack(Material.BRICKS);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§fТип структуры: §e" + translateBuildType(type));
            meta.setLore(Arrays.asList(
                    "§7Выберите, что строить:",
                    "§7Дом / Башня / Мост / Особняк",
                    "",
                    "§eЛевый клик §7— следующий тип"));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack makeBuildAround() {
        boolean on = settings.isBuildAround();
        ItemStack item = new ItemStack(Material.GRASS_BLOCK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§fСтройка вокруг: " + statusLabel(on));
            meta.setLore(Arrays.asList(
                    "§7Бот строит вокруг игрока.",
                    "",
                    "§eЛевый клик §7— переключить"));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack makeSpawnCmd() {
        String cmd = settings.getSpawnCommand();
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§fКоманда при появлении");
            meta.setLore(Arrays.asList(
                    "§7Выполняется при спавне AI Bot.",
                    "§7Текущее значение:",
                    cmd.isEmpty() ? "§8(не задана)" : "§a" + cmd,
                    "",
                    "§eЛевый клик §7— изменить",
                    "§cПравый клик §7— очистить"));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack makeDespawnCmd() {
        String cmd = settings.getDespawnCommand();
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§fКоманда при исчезновении");
            meta.setLore(Arrays.asList(
                    "§7Выполняется при деспавне AI Bot.",
                    "§7Текущее значение:",
                    cmd.isEmpty() ? "§8(не задана)" : "§a" + cmd,
                    "",
                    "§eЛевый клик §7— изменить",
                    "§cПравый клик §7— очистить"));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack makeHealthInfo() {
        ItemStack item = new ItemStack(Material.APPLE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§fСостояние AI Bot");
            meta.setLore(Arrays.asList(
                    "§7Используйте кнопки управления",
                    "§7для проверки активности бота.",
                    "",
                    "§aСпавн §f— кнопка «Спавн AI_Bot»",
                    "§cДеспавн §f— кнопка «Деспавн AI_Bot»"));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack makeMovementSpeed() {
        ItemStack item = new ItemStack(Material.SUGAR);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(String.format("§fСкорость движения: §e%.1f", settings.getMovementSpeed()));
            meta.setLore(Arrays.asList(
                    "§7Скорость перемещения AI Bot.",
                    "§7Диапазон: 0.5–5.0.",
                    "",
                    "§aЛевый клик §7— +0.5",
                    "§cПравый клик §7— −0.5"));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack makeSpawnBotButton() {
        ItemStack item = new ItemStack(Material.ZOMBIE_SPAWN_EGG);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§a✦ Спавн AI_Bot");
            meta.setLore(Arrays.asList(
                    "§7Призвать AI Bot рядом с вами.",
                    "",
                    "§eЛевый клик §7— заспавнить"));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack makeDespawnBotButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§c✦ Деспавн AI_Bot");
            meta.setLore(Arrays.asList(
                    "§7Убрать AI Bot с сервера.",
                    "",
                    "§eЛевый клик §7— убрать"));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack makeResetButton() {
        ItemStack item = new ItemStack(Material.TNT);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§c⚠ Очистить настройки");
            meta.setLore(Arrays.asList(
                    "§7Сбросить все параметры",
                    "§7к значениям по умолчанию.",
                    "",
                    "§cЛевый клик §7— сбросить"));
            item.setItemMeta(meta);
        }
        return item;
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private static String statusLabel(boolean on) {
        return on ? "§a[ВКЛ]" : "§c[ВЫКЛ]";
    }

    private static String translateBuildType(String type) {
        switch (type) {
            case "tower":   return "Башня";
            case "bridge":  return "Мост";
            case "mansion": return "Особняк";
            default:        return "Дом";
        }
    }

    // ── Wooden sword factory (static) ─────────────────────────────────────────

    /**
     * Build the wooden sword item that opens the AI Bot menu.
     */
    public static ItemStack makeAIWoodenSword() {
        ItemStack item = new ItemStack(Material.WOODEN_SWORD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§r§fAI");
            meta.setLore(Arrays.asList(
                    "§7Нажмите правой кнопкой мыши",
                    "§7для открытия меню AI Bot."));
            item.setItemMeta(meta);
        }
        return item;
    }
}
