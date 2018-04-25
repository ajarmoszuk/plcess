package eu.polandcraft;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;

public abstract class PluginModule implements Listener {
    private boolean isEnabled;

    public abstract boolean onEnable();

    public abstract void onDisable();

    public boolean setEnabled(boolean enabled) {
        boolean ok = false;
        if (enabled)
            ok = onEnable();
        else
            onDisable();
        this.isEnabled = ok;
        return ok;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public String getName() {
        return "PLCEss module";
    }

    public void logInfo(String message) {
        Bukkit.getLogger().info(String.format("[%s] %s", getName(), message));
    }

    public void logWarning(String message) {
        Bukkit.getLogger().warning(String.format("[%s] %s", getName(), message));
    }

    public void logSevere(String message) {
        Bukkit.getLogger().warning(String.format("[%s] %s", getName(), message));
    }
}
