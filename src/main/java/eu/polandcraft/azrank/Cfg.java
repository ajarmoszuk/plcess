package eu.polandcraft.azrank;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.TimeZone;

class Cfg {
    private AZRank plugin;
    private final YamlConfiguration config = new YamlConfiguration();

    private String message = "+player is now a(n) +group for+time";
    private String aWhile = " a while";
    private String ever = "ever";
    private boolean broadcastRankChange = true;
    public boolean allowOpsChanges = true;
    public boolean logEverything = false;
    public int checkInterval = 10 * 20;
    public TimeZone timeZone = TimeZone.getDefault();

    Cfg(AZRank plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("CallToThreadDumpStack")
    boolean loadConfig() {
        try {
            config.load(plugin.yml);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } catch (InvalidConfigurationException e) {
            e.printStackTrace();
            return false;
        }

        message = config.getString("message", message);
        aWhile = config.getString("aWhile", aWhile);
        ever = config.getString("ever", ever);
        broadcastRankChange = config.getBoolean("broadcastRankChange", broadcastRankChange);
        allowOpsChanges = config.getBoolean("allowOpsChanges", allowOpsChanges);
        logEverything = config.getBoolean("logEverything", logEverything);
        checkInterval = 20 * config.getInt("checkInterval", checkInterval / 20);

        return true;
    }

    void defaultConfig() {
        config.set("message", message);
        config.set("aWhile", aWhile);
        config.set("ever", ever);
        config.set("broadcastRankChange", broadcastRankChange);
        config.set("allowOpsChanges", allowOpsChanges);
        config.set("logEverything", logEverything);
        config.set("checkInterval", checkInterval / 20);

        try {
            config.save(plugin.yml);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void checkConfig() {
        try {
            config.load(plugin.yml);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
        boolean hasChanged = false;
        if (config.get("message") == null) {
            config.set("message", message);
            hasChanged = true;
        }
        if (config.get("aWhile") == null) {
            config.set("aWhile", aWhile);
            hasChanged = true;
        }
        if (config.get("ever") == null) {
            config.set("ever", ever);
            hasChanged = true;
        }
        if (config.get("broadcastRankChange") == null) {
            config.set("broadcastRankChange", broadcastRankChange);
            hasChanged = true;
        }
        if (config.get("allowOpsChanges") == null) {
            config.set("allowOpsChanges", allowOpsChanges);
            hasChanged = true;
        }
        if (config.get("logEverything") == null) {
            config.set("logEverything", logEverything);
            hasChanged = true;
        }
        if (config.get("checkInterval") == null) {
            config.set("checkInterval", checkInterval / 20);
            hasChanged = true;
        } else if (config.getInt("checkInterval") < 1) {
            config.set("checkInterval", checkInterval / 20);
            hasChanged = true;
        }

        if (hasChanged) {
            try {
                config.save(plugin.yml);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
