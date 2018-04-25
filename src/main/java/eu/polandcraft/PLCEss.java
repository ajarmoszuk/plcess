package eu.polandcraft;

import eu.polandcraft.azrank.AZRank;
import eu.polandcraft.cmdlogger.CmdLogger;
import eu.polandcraft.is.ItemShop;
//import eu.polandcraft.nis.NewItemShop;
import eu.polandcraft.util.UtilModule;
import eu.polandcraft.util.ChuckNorris;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class PLCEss extends JavaPlugin {
    private AZRank azrank;
    List<PluginModule> modules = new ArrayList<>();

    public PLCEss() {
        modules.add(new ChuckNorris(this));
        modules.add(new UtilModule(this));
        modules.add(new CmdLogger(this));
        azrank = new AZRank(this);
        modules.add(azrank);
        //modules.add(new NewItemShop(this));
        modules.add(new ItemShop(this));
    }

    @Override
    public void onEnable() {
        for (PluginModule m : modules) {
            long time = System.currentTimeMillis();
            try {
                if (m.setEnabled(true))
                    getLogger().info(String.format("%s module is now enabled (%dms)", m.getName(), System.currentTimeMillis() - time));
                else
                    getLogger().info(String.format("Error when loading module %s (%dms)", m.getName(), System.currentTimeMillis() - time));
            } catch (Exception e) {
                getLogger().info(String.format("Critial error when loading module %s (%dms)", m.getName(), System.currentTimeMillis() - time));
            }
        }
    }

    @Override
    public void onDisable() {
        for (PluginModule m : modules) {
            long time = System.currentTimeMillis();
            m.setEnabled(false);
            getLogger().info(String.format("Disabled module %s (%dms)", m.getName(), System.currentTimeMillis() - time));
        }
    }

    public AZRank getAZRank() {
        return azrank;
    }
}
