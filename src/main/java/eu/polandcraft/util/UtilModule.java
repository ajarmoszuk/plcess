package eu.polandcraft.util;

import eu.polandcraft.PLCEss;
import eu.polandcraft.PluginModule;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerLoginEvent;

public class UtilModule extends PluginModule {
    private PLCEss plugin;
    private ConfigAccessor confAcc;
    private FileConfiguration config;

    public UtilModule(PLCEss plug) {
        plugin = plug;
    }

    @Override
    public String getName() {
        return "PLCEss Util";
    }

    @Override
    public boolean onEnable() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        confAcc = new ConfigAccessor(plugin, "blockedcmds.yml");
        config = confAcc.getConfig();
        confAcc.saveDefaultConfig();
        return true;
    }

    @Override
    public void onDisable() {
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerLogin(PlayerLoginEvent e) {
        Player p = e.getPlayer();
        String name = p.getName();
        String ip = e.getAddress().getHostAddress();
        if (name.startsWith("\\") || name.startsWith("/")) {
            String msg = "Uwaga! Script Kiddie o adresie IP: %s, probowal wejsc z nickiem %s";
            logWarning(String.format(msg, ip, name));
            e.disallow(PlayerLoginEvent.Result.KICK_OTHER, "Taaa, ciekawe...");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerCmd(PlayerCommandPreprocessEvent e) {
        for (String i : config.getStringList("blocked")) {
            if (e.getMessage().toLowerCase().startsWith(i.toLowerCase())) {
                if (isWhitelisted(e.getPlayer().getName(), e.getMessage())) return;
                String msg = "Zablokowano komendę %s, użytą przez %s";
                logInfo(String.format(msg, e.getMessage(), e.getPlayer().getName()));
                e.setMessage("/cancelledcmd " + e.getMessage());
                e.setCancelled(true);
                e.getPlayer().sendMessage(ChatColor.DARK_GRAY + "[" + ChatColor.WHITE + "Poland" + ChatColor.DARK_RED + "Craft" + ChatColor.DARK_GRAY + "] " + ChatColor.RED + "Ta komenda jest dostępna tylko z konsoli!");
            }
        }
    }

    private boolean isWhitelisted(String nick, String cmd) {
        String wat = cmd.split(" ")[0];
        for (String i : config.getStringList("whitelist." + wat)) {
            if (i.equalsIgnoreCase(nick)) {
                logInfo(String.format("%s był na whiteliscie komendy '%s'", nick, wat));
                return true;
            }
        }
        return false;
    }
}
