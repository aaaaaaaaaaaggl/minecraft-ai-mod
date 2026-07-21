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

    // ── Persistence ──────────────────────────────────────────────────────────
    private static File     dataFile;
    private static FileConfiguration data;

    /**
     * Initialise the backing YAML file. Call once from {@code onEnable}.
     */
    public static void init(JavaPlugin plugin) {
        dataFile = new File(plugin.getDataFolder(), "ai_bot_settings.yml");
        if (!dataFile.exists()) {
            dataFile.getParentFile().mkdirs();
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
    public void setAttackRange(double v)  { this.attackRange = Math.max(1.0, Math.min(50.0, v)); }

    public double getAttackDamage()       { return attackDamage; }
    public void setAttackDamage(double v) { this.attackDamage = Math.max(1.0, Math.min(20.0, v)); }

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
}
