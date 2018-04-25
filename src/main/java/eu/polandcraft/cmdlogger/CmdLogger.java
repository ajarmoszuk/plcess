package eu.polandcraft.cmdlogger;

import eu.polandcraft.PLCEss;
import eu.polandcraft.PluginModule;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class CmdLogger extends PluginModule {
    private SimpleDateFormat fileFormat = new SimpleDateFormat("yyyy-MM-dd");
    private SimpleDateFormat timeFormat = new SimpleDateFormat("[dd.MM.yyyy HH:mm:ss]");
    private File logDir;
    private BufferedWriter stream;
    private PLCEss plugin;

    public CmdLogger(PLCEss plug) {
        this.plugin = plug;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChatCommand(PlayerCommandPreprocessEvent e) {
        writeToFile(e.getPlayer(), e.getMessage());
    }

    @Override
    public boolean onEnable() {
        logDir = new File(plugin.getDataFolder(), "logs");
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        return true;
    }

    @Override
    public void onDisable() {
    }

    private void writeToFile(Player p, String cmd) {
        Location loc = p.getLocation();
        String nick = p.getName();
        String ip = p.getAddress().getAddress().getHostAddress();
        String map = loc.getWorld().getName();
        int x = (int) loc.getX();
        int y = (int) loc.getY();
        int z = (int) loc.getZ();

        String time = timeFormat.format(Calendar.getInstance().getTime());
        String data = String.format("<%s @ %s> %s (%s @ X:%d,Y:%d,Z:%d)", nick, ip, cmd, map, x, y, z);
        String logstr = time + " " + data;
        boolean dontLog = (cmd.startsWith("/login") || cmd.startsWith("/register") || cmd.startsWith("/changepassword") || cmd.startsWith("/r") || cmd.startsWith("/l"));
        if (!dontLog)
            logInfo(data);

        try {
            setupFile();
            stream.append(logstr);
            stream.newLine();
            stream.close();
        } catch (IOException e) {
            logWarning("Blad przy zapisie do pliku");
        }
    }

    private void setupFile() {
        try {
            File logFile = new File(logDir, fileFormat.format(Calendar.getInstance().getTime()) + ".log");
            if (!logFile.exists()) {
                logDir.mkdirs();
                logFile.createNewFile();
            }
            stream = new BufferedWriter(new FileWriter(logFile, true));
        } catch (IOException e) {
            logWarning("Blad przy utworzeniu strumienia.");
        }
    }

    @Override
    public String getName() {
        return "CmdLogger";
    }
}
