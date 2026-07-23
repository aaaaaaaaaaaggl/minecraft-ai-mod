package com.minecraft.ai;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Per-player AI Bot settings stored in data/ai_bot_settings.yml.
 *
 * <p>Holds all configurable parameters that the GUI menu exposes:
 * combat flags, build options, and behaviour hooks.
 */
public class AIBotSettings {

    private static final Logger LOGGER = Logger.getLogger("AIBotSettings");

    // ── Combat ───────────────────────────────────────────────────────────────
    private boolean attackMobs    = false;
    private boolean attackPlayers = false;
    private double  attackRange   = 10.0;
    private double  attackDamage  = 4.0;

    // ── Build ─────────────────────────────────────────────────────────────────
    private boolean autoBuild   = false;
    private String  buildType   = "house"; // house | tower | bridge | mansion
    private boolean buildAround = false;

    // ── Behaviour ────────────────────────────────────────────────────────────
    private String spawnCommand  = "";
    private String despawnCommand = "";
    private double movementSpeed = 2.0;

    // ── Range / damage limits ─────────────────────────────────────────────────

    /** Minimum and maximum allowed attack range in blocks. */
    private static final double MIN_ATTACK_RANGE   = 1.0;
    private static final double MAX_ATTACK_RANGE   = 50.0;

    /** Minimum and maximum allowed attack damage per hit. */
    private static final double MIN_ATTACK_DAMAGE  = 1.0;
    private static final double MAX_ATTACK_DAMAGE  = 20.0;
    private static final double MIN_MOVEMENT_SPEED = 0.5;
    private static final double MAX_MOVEMENT_SPEED = 5.0;
    private static File     dataFile;
    private static FileConfiguration data;

    /**
     * Initialise the backing YAML file. Call once from {@code onEnable}.
     */
    public static void init(JavaPlugin plugin) {
        dataFile = new File(plugin.getDataFolder(), "ai_bot_settings.yml");
        if (!dataFile.exists()) {
            File parent = dataFile.getParentFile();
            if (!parent.exists() && !parent.mkdirs()) {
                LOGGER.warning("Could not create data directory: " + parent.getAbsolutePath());
            }
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                LOGGER.warning("Could not create ai_bot_settings.yml: " + e.getMessage());
            }
        }
        data = YamlConfiguration.loadConfiguration(dataFile);
    }

    /**
     * Load settings for the given player UUID, or return defaults if none saved.
     */
    public static AIBotSettings load(UUID uuid) {
        AIBotSettings s = new AIBotSettings();
        if (data == null || !data.contains(uuid.toString())) {
            return s;
        }
        String path = uuid.toString() + ".";
        s.attackMobs     = data.getBoolean(path + "attackMobs",     false);
        s.attackPlayers  = data.getBoolean(path + "attackPlayers",  false);
        s.attackRange    = data.getDouble( path + "attackRange",    10.0);
        s.attackDamage   = data.getDouble( path + "attackDamage",   4.0);
        s.autoBuild      = data.getBoolean(path + "autoBuild",      false);
        s.buildType      = data.getString( path + "buildType",      "house");
        s.buildAround    = data.getBoolean(path + "buildAround",    false);
        s.spawnCommand   = data.getString( path + "spawnCommand",   "");
        s.despawnCommand = data.getString( path + "despawnCommand", "");
        s.setMovementSpeed(data.getDouble(path + "movementSpeed", 2.0));
        return s;
    }

    /**
     * Persist the current settings for {@code uuid}.
     */
    public void save(UUID uuid) {
        if (data == null) return;
        String path = uuid.toString() + ".";
        data.set(path + "attackMobs",     attackMobs);
        data.set(path + "attackPlayers",  attackPlayers);
        data.set(path + "attackRange",    attackRange);
        data.set(path + "attackDamage",   attackDamage);
        data.set(path + "autoBuild",      autoBuild);
        data.set(path + "buildType",      buildType);
        data.set(path + "buildAround",    buildAround);
        data.set(path + "spawnCommand",   spawnCommand);
        data.set(path + "despawnCommand", despawnCommand);
        data.set(path + "movementSpeed",  movementSpeed);
        try {
            data.save(dataFile);
        } catch (IOException e) {
            LOGGER.warning("Could not save ai_bot_settings.yml: " + e.getMessage());
        }
    }

    /** Remove all saved settings for {@code uuid}. */
    public static void reset(UUID uuid) {
        if (data == null) return;
        data.set(uuid.toString(), null);
        try {
            data.save(dataFile);
        } catch (IOException e) {
            LOGGER.warning("Could not save ai_bot_settings.yml: " + e.getMessage());
        }
    }

    // ── Getters / Setters ────────────────────────────────────────────────────

    public boolean isAttackMobs()        { return attackMobs; }
    public void setAttackMobs(boolean v) { this.attackMobs = v; }

    public boolean isAttackPlayers()        { return attackPlayers; }
    public void setAttackPlayers(boolean v) { this.attackPlayers = v; }

    public double getAttackRange()        { return attackRange; }
    public void setAttackRange(double v)  { this.attackRange  = Math.max(MIN_ATTACK_RANGE,  Math.min(MAX_ATTACK_RANGE,  v)); }

    public double getAttackDamage()       { return attackDamage; }
    public void setAttackDamage(double v) { this.attackDamage = Math.max(MIN_ATTACK_DAMAGE, Math.min(MAX_ATTACK_DAMAGE, v)); }

    public boolean isAutoBuild()        { return autoBuild; }
    public void setAutoBuild(boolean v) { this.autoBuild = v; }

    public String getBuildType()          { return buildType; }
    public void setBuildType(String type) { this.buildType = type; }

    /**
     * Cycle through the available structure types.
     */
    public void cycleBuildType() {
        switch (buildType) {
            case "house":   buildType = "tower";   break;
            case "tower":   buildType = "bridge";  break;
            case "bridge":  buildType = "mansion"; break;
            default:        buildType = "house";   break;
        }
    }

    public boolean isBuildAround()        { return buildAround; }
    public void setBuildAround(boolean v) { this.buildAround = v; }

    public String getSpawnCommand()           { return spawnCommand; }
    public void setSpawnCommand(String cmd)   { this.spawnCommand = cmd; }

    public String getDespawnCommand()           { return despawnCommand; }
    public void setDespawnCommand(String cmd)   { this.despawnCommand = cmd; }

    public double getMovementSpeed() {
        return movementSpeed;
    }

    public void setMovementSpeed(double speed) {
        this.movementSpeed = Math.max(MIN_MOVEMENT_SPEED, Math.min(MAX_MOVEMENT_SPEED, speed));
    }
}
