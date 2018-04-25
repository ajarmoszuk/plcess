package eu.polandcraft.azrank;

import eu.polandcraft.PLCEss;
import eu.polandcraft.PluginModule;
import eu.polandcraft.azrank.permissions.AZPermissionsHandler;
import eu.polandcraft.azrank.permissions.AZVaultAdapter;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class AZRank extends PluginModule implements CommandExecutor {
    YamlConfiguration database = new YamlConfiguration();
    private Cfg cfg = new Cfg(this);

    public AZPermissionsHandler permBridge = null;

    public File yml;
    private File yamlDataBaseFile;

    private int taskID;
    private TimeRankChecker checker;
    private int checkDelay = 10 * 20;
    private SimpleDateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public int tempranks = 0;
    private static final String INFO_NODE = "azrank.info";
    private static final String LIST_NODE = "azrank.list";
    private static final String ALL_NODE = "azrank.*";

    private PLCEss plugin;

    public AZRank(PLCEss plcess) {
        plugin = plcess;
        dateformat.setTimeZone(cfg.timeZone);
    }

    public Server getServer() {
        return plugin.getServer();
    }

    private boolean checkVault() {
        try {
            Class.forName("net.milkbowl.vault.permission.Permission");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean onEnable() {
        yamlDataBaseFile = new File(plugin.getDataFolder(), "azrank_db.yml");
        yml = new File(plugin.getDataFolder(), "azrank.yml");

        if (!setupPermissions()) return false;

        if (!yml.exists() || !yamlDataBaseFile.exists()) {
            firstRunSettings();
        }
        dLoad();
        checker = new TimeRankChecker(this);
        checker.run();
        taskID = getServer().getScheduler().scheduleAsyncRepeatingTask(this.plugin, checker, checkDelay, cfg.checkInterval);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        registerCmds();
        return true;
    }

    @Override
    public void onDisable() {
        getServer().getScheduler().cancelTask(taskID);
    }

    private boolean setupPermissions() {
        if (!checkVault()) {
            logSevere("AZRank nie znalazł Vault i zostanie wyłączony.");
            return false;
        }
        RegisteredServiceProvider<Permission> permissionProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
        permBridge = new AZVaultAdapter(this, permissionProvider.getProvider());
        return true;
    }

    private void firstRunSettings() {
        try {
            if (!yml.exists()) {
                cfg.defaultConfig();
            }
            if (!yamlDataBaseFile.exists()) {
                FileWriter fstream = new FileWriter(yamlDataBaseFile);
                BufferedWriter out = new BufferedWriter(fstream);
                out.write("{}");
                out.close();
            }
        } catch (Exception e) {
            logSevere("Failed to create config file!");
        }
    }

    private void registerCmds() {
        List<String> cmds = new ArrayList<>();
        cmds.add("azplayer");
        cmds.add("azrankreload");
        cmds.add("azsetgroup");
        cmds.add("azaddgroup");
        cmds.add("azremovegroup");
        cmds.add("azranks");
        cmds.add("azextend");

        for (String cmd : cmds) {
            PluginCommand a = plugin.getCommand(cmd);
            if (a != null) a.setExecutor(this);
        }
    }

    @Override
    public boolean onCommand(CommandSender cs, Command cmd, String alias, String[] args) {
        if (cmd.getName().equalsIgnoreCase("azplayer")) {
            if (args.length > 0) {
                if (args.length > 1)
                    sayTooManyArgs(cs, 1);
                if (hasPerm(cs, INFO_NODE))
                    return infoCMD(cs, args[0]);
                else {
                    sayNoPerm(cs);
                    return true;
                }
            } else
                sayTooFewArgs(cs, 1);
        } else if (cmd.getName().equalsIgnoreCase("azrankreload")) {
            if (args.length > 0) {
                sayTooManyArgs(cs, 0);
            }
            if (cs instanceof Player) {
                Player player = (Player) cs;
                if (hasReload(player) || (cfg.allowOpsChanges && player.isOp())) {
                    if (dLoad()) {  // jeżeli dobrze przeładowano
                        cs.sendMessage(ChatColor.GREEN + "[AZRank] AZRank was succesfully reloaded");
                        getServer().getScheduler().cancelTask(taskID);
                        taskID = getServer().getScheduler().scheduleAsyncRepeatingTask(this.plugin, checker, checkDelay, cfg.checkInterval);
                        logInfo("AZRank was succesfully reloaded");
                    } else {  //jeżeli błąd podczas przeładowywania
                        cs.sendMessage(ChatColor.GREEN + "[AZRank] AZRank - Error when reloading");
                    }
                } else {
                    cs.sendMessage(ChatColor.GREEN + "[AZRank] " + ChatColor.RED + "You do not have permission to do this!");
                }
            } else {
                if (dLoad()) {  // jeżeli dobrze przeładowano
                    getServer().getScheduler().cancelTask(taskID);
                    taskID = getServer().getScheduler().scheduleAsyncRepeatingTask(this.plugin, checker, checkDelay, cfg.checkInterval);
                    logInfo("AZRank was succesfully reloaded");
                } else {  //jeżeli błąd podczas przeładowywania
                    cs.sendMessage(ChatColor.GREEN + "[AZRank] AZRank - Error when reloading");
                }
            }
            return true;
        } else if (cmd.getName().equalsIgnoreCase("azsetgroup")) {
            if (args.length < 2) {
                sayTooFewArgs(cs, 2);
                return false;
            }
            long czas = -1;
            if (args.length > 2) {
                if (args.length > 4)
                    sayTooManyArgs(cs, 4);
                try {
                    czas = Util.parseTimeDiffInMillis(args[args.length - 1]);
                } catch (Exception e) {
                    cs.sendMessage(ChatColor.RED + "[AZRank] Error - " + e.getMessage());
                    e.printStackTrace();
                    return false;
                }
            }
            if (canSetRank(cs, args[0], args[1])) {
                if (!SetRank(cs, args[0], new String[]{args[1]}, czas, args[args.length - 2].equalsIgnoreCase("-s"))) {
                    cs.sendMessage(ChatColor.GREEN + "[AZRank]" + ChatColor.RED + "An error occurred when tried to set group!");
                } else
                    return true;
            }
        } else if (cmd.getName().equalsIgnoreCase("azaddgroup")) {
            if (args.length < 2) {
                sayTooFewArgs(cs, 2);
                return false;
            }
            long czas = -1;
            if (args.length > 2) {
                if (args.length > 4)
                    sayTooManyArgs(cs, 4);
                try {
                    czas = Util.parseTimeDiffInMillis(args[args.length - 1]);
                } catch (Exception e) {
                    cs.sendMessage(ChatColor.RED + "[AZRank] Error - " + e.getMessage());
                    e.printStackTrace();
                    return false;
                }
            }
            if (canSetRank(cs, args[0], args[1])) {
                if (!playerAddTmpGroup(cs, args[0], new String[]{args[1]}, czas, args[args.length - 2].equalsIgnoreCase("-s"))) {
                    cs.sendMessage(ChatColor.GREEN + "[AZRank]" + ChatColor.RED + "An error occurred when tried to add group!");
                } else
                    return true;
            }
        } else if (cmd.getName().equalsIgnoreCase("azremovegroup")) {
            if (args.length == 2) {
                if (canSetRank(cs, args[0], args[1])) {
                    if (permBridge.playerRemoveGroups(args[0], new String[]{args[1]})) {
                        if (database.getConfigurationSection("users." + args[0] + "." + args[1]) != null) {
                            database.set("users." + args[0] + "." + args[1], null);
                        }
                        cs.sendMessage(ChatColor.GREEN + "[AZRank]" + ChatColor.AQUA + " Successful removed " + args[0] + " from " + args[1] + "!");
                        return true;
                    } else {
                        cs.sendMessage(ChatColor.GREEN + "[AZRank]" + ChatColor.RED + "An error occurred when tried to remove group!");
                        return false;
                    }

                } else {
                    sayNoPerm(cs);
                    return false;
                }
            } else if (args.length < 2) {
                sayTooFewArgs(cs, 2);
                return false;
            } else if (args.length > 2) {
                sayTooManyArgs(cs, 2);
                return false;
            }
        } else if (cmd.getName().equalsIgnoreCase("azranks")) {
            if (cs instanceof Player) {
                if (!hasPerm(cs, LIST_NODE)) {
                    sayNoPerm(cs);
                    return false;
                }
                //wypisywanie jezeli gracz
                if (args.length > 0) {
                    try {
                        int page = Integer.parseInt(args[0]);
                        if (page < 1) throw new NumberFormatException("Number must be positive!");
                        wypiszGraczy(cs, 10, page);
                        return true;
                    } catch (NumberFormatException e) {
                        cs.sendMessage(ChatColor.GREEN + "[AZRank] " + ChatColor.RED + "Error! Invalid page number!" + e.getMessage());
                        return false;
                    }
                } else {
                    wypiszGraczy(cs, 10, 1);
                    return true;
                }
            } else { //wypisywanie jeżeli konsola
                wypiszGraczy(cs, 0, 0);
                return true;
            }
        } else if (cmd.getName().equalsIgnoreCase("azextend")) {
            //TODO: extension all temp groups time
            if (args.length != 3) {
                if (args.length < 3)
                    sayTooFewArgs(cs, 3);
                else
                    sayTooManyArgs(cs, 3);
                return false;
            }
            long interval;
            try {
                interval = Util.parseTimeDiffInMillis(args[args.length - 1]);
            } catch (Exception e) {
                cs.sendMessage(ChatColor.RED + "[AZRank] Invalid time format - " + e.getMessage());
                e.printStackTrace();
                return false;
            }
            if (canSetRank(cs, args[0], args[1])) {
                ConfigurationSection groupSection = database.getConfigurationSection("users." + args[0] + "." + args[1]);
                if (groupSection == null) {
                    cs.sendMessage(ChatColor.GREEN + "[AZRank] " + ChatColor.RED + "Player isnt in that group temporary");
                    return false;
                }
                long to = groupSection.getLong("to");
                if (to > 0) {
                    groupSection.set("to", to + interval);
                    save(cs);
                    cs.sendMessage(ChatColor.GREEN + "[AZRank] " + ChatColor.AQUA + "Successful prolonged duration " + args[0] + " in " + args[1]);
                    return true;
                } else {
                    cs.sendMessage(ChatColor.GREEN + "[AZRank] " + ChatColor.RED + "Invalid end time for " + args[0] + " in " + args[1] + "! Deleting!");
                    database.set("users." + args[0] + "." + args[1], null);
                    save(cs);
                    return false;
                }
            }
        }

        return false;
    }

    /**
     * Say "You do not have permission to do this!"
     *
     * @param cs CommandSender to which is send message
     */
    void sayNoPerm(CommandSender cs) {
        cs.sendMessage(ChatColor.GREEN + "[AZRank] " + ChatColor.RED + "You do not have permission to do this!");
    }

    /**
     * Say "Too many arguments!"
     *
     * @param cs CommandSender to which is send message
     * @param a  maximum amount of parameters thad expected
     */
    void sayTooManyArgs(CommandSender cs, int a) {
        cs.sendMessage(ChatColor.GREEN + "[AZRank] " + ChatColor.RED + "Too many arguments! Expected maximum " + a);
    }

    void sayTooFewArgs(CommandSender cs, int a) {
        cs.sendMessage(ChatColor.GREEN + "[AZRank] " + ChatColor.RED + "Too few arguments! Expected minimum " + a);
    }

    //TODO: przenieść do API / move to API

    /**
     * Set rank. If player already is in only given groups and set is false, time will be prolonged.
     *
     * @param cs       Messages output
     * @param Player   player to change his groups
     * @param groups   new groups to set to the player
     * @param timeDiff the duration of give groups
     * @param set      if true, time will be set, else if already player is in only give groups, time will be prolonged
     */
    //@Deprecated
    @SuppressWarnings("unchecked")
    boolean SetRank(CommandSender cs, String Player, String[] groups, long timeDiff, boolean set) {
        ConfigurationSection usersSection = database.getConfigurationSection("users." + Player);
        if (timeDiff <= 0L) {
            if (usersSection != null) {
                database.set("users." + Player, null);
                save(cs);

            }
            if (permBridge.setPlayersGroups(Player, groups)) {
                cs.sendMessage(ChatColor.AQUA + "[AZRank]" + ChatColor.AQUA + " Successful moved " + Player + " to " + Util.tableToString(groups) + " forever!");
                return true;
            }
            return false;
        } else //tymczasowa
        {
            String[] oldGroups = permBridge.getPlayersGroups(Player);
            if (Arrays.equals(groups, oldGroups)) //jeżeli jest już w tych grupach
            {
                //TODO: multi check
                // debugmsg
                int i = 0;
                java.util.Date now = new java.util.Date();
                ConfigurationSection groupSection = database.getConfigurationSection("users." + Player + "." + groups[i]);
                long timeTo = groupSection.getLong("to");
                if (!set && timeTo > 0) { //has temporiary and without -s flag (adding time)
                    //TODO: add adding time
                    timeTo += timeDiff;
                    cs.sendMessage(ChatColor.GREEN + "[AZRank]" + ChatColor.AQUA + " Adding some time for " + Player + " to be in " + Util.tableToString(groups));
                } else {
                    cs.sendMessage(ChatColor.GREEN + "[AZRank]" + ChatColor.AQUA + " Setting new time for " + Player + " to be in " + Util.tableToString(groups));
                    timeTo = now.getTime() + timeDiff;
                }

                {//updateing database
                    database.set("users." + Player + "." + groups[i] + ".to", timeTo);
                    save(cs);
                }
                return true;
            } else //inne grupy niż ma
            {
                //TODO: debug msgs to this:
                List<String> restoreGroups = new LinkedList<>();
                for (String oldGroup : oldGroups) {
                    if (database.getConfigurationSection("users." + Player + "." + oldGroup) != null)//jeżeli są dane grupy(to tymczasowa)
                    {
                        List<String> rg = (List<String>) database.getList("users." + Player + "." + oldGroup + ".restoreGroups");
                        if (rg != null) //jeżeli jakieś są to dodaj je do nowej listy
                        {
                            Iterator<String> it = rg.iterator();
                            String next;
                            while (it.hasNext()) {
                                next = it.next();
                                if (!restoreGroups.contains(next))
                                    restoreGroups.add(next);
                            }
                        }
                    } else
                        restoreGroups.add(oldGroup);
                }
                if (permBridge.setPlayersGroups(Player, groups)) { //jeżeli pomyślnie ustawiono nowe grupy
                    //TODO: obsługe wyjątków
                    database.set("users." + Player, null);
                    java.util.Date now = new java.util.Date();
                    for (String group : groups) {
                        database.set("users." + Player + "." + group + ".restoreGroups", restoreGroups);
                        database.set("users." + Player + "." + group + ".to", now.getTime() + timeDiff);
                    }
                    cs.sendMessage(ChatColor.GREEN + "[AZRank]" + ChatColor.AQUA + " Moved " + Player + " to " + Util.tableToString(groups) + " to " + now.getTime() + timeDiff);
                    save(cs);
                    return true;
                } else { //błąd
                    cs.sendMessage(ChatColor.RED + "Błąd! Nie udało się zmienić grupy!");
                    return false;
                }
            }
        }
    }

    public void givePlayerTmpGroup(String player, String group, long timeDiff) {
        String path = String.format("users.%s.%s", player, group);
        if (isInGroups(new String[]{group}, permBridge.getPlayersGroups(player)) && database.isConfigurationSection(path)) {
            ConfigurationSection groupSection = database.getConfigurationSection(path);
            Date toDate = new Date();
            long timeTo = groupSection.getLong("to");
            toDate.setTime(timeTo + timeDiff);
            groupSection.set("to", toDate.getTime());
        } else {
            Date now = new Date();
            Date toDate = new Date();
            toDate.setTime(now.getTime() + timeDiff);
            database.set(path + ".to", toDate.getTime());
            permBridge.setPlayersGroups(player, new String[]{group});
        }
        save();
    }

    boolean playerAddTmpGroup(CommandSender cs, String Player, String[] groups, long timeDiff, boolean set) {
        ConfigurationSection usersSection = database.getConfigurationSection("users." + Player);
        if (timeDiff <= 0L) {
            for (String group : groups) {
                database.set("users." + Player + "." + group, null);
            }
            save(cs);
            if (permBridge.playerAddGroups(Player, groups)) {
                cs.sendMessage(ChatColor.AQUA + "[AZRank]" + ChatColor.AQUA + " Successful add " + Player + " to " + Util.tableToString(groups) + " forever!");
                return true;
            }
            return false;
        } else //tymczasowa
        {
            String[] oldGroups = permBridge.getPlayersGroups(Player);
            boolean czy = isInGroups(groups, oldGroups);
            if (czy) //jeżeli jest już w tych grupach
            {
                //TODO: multi check
                // debugmsg
                int i = 0;
                ConfigurationSection groupSection = database.getConfigurationSection("users." + Player + "." + groups[i]);
                Date now = new Date();
                long timeTo = groupSection.getLong("to");

                if (!set && timeTo > 0) { //has temporiary and without -s flag (adding time)
                    //TODO: add adding time
                    timeTo += timeDiff;
                    //time = Util parse time + database.get
                    cs.sendMessage(ChatColor.GREEN + "[AZRank]" + ChatColor.AQUA + " Adding some time for " + Player + " to be in " + Util.tableToString(groups));
                } else {
                    timeTo = timeDiff + now.getTime();
                    cs.sendMessage(ChatColor.GREEN + "[AZRank]" + ChatColor.AQUA + " Setting new time for " + Player + " to be in " + Util.tableToString(groups));
                }

                {//updateing database
                    database.set("users." + Player + "." + groups[i] + ".to", timeTo);
                    save(cs);
                }
                return true;
            } else //inne grupy niż ma
            {
                //TODO: dla każdej grupy osobno sprawdzanie czy w niej już jest!
                if (permBridge.playerAddGroups(Player, groups)) { //jeżeli pomyślnie ustawiono nowe grupy
                    //TODO: obsługe wyjątków
                    Date now = new Date();
                    Date toDate = new Date();
                    toDate.setTime(now.getTime() + timeDiff);
                    for (String group : groups) {
                        database.set("users." + Player + "." + group + ".to", toDate.getTime());
                    }
                    cs.sendMessage(ChatColor.GREEN + "[AZRank]" + ChatColor.AQUA + " Added " + Player + " to " + Util.tableToString(groups) + " to " + dateformat.format(toDate));
                    save(cs);
                    return true;
                } else { //błąd
                    cs.sendMessage(ChatColor.RED + "Błąd! Nie udało się zmienić grupy!");
                    return false;
                }
            }
        }
    }

    private boolean isInGroups(String[] groups, String[] oldGroups) {
        boolean znaleziono = false;
        boolean czy = false;
        if (groups.length > 0)
            czy = true;
        for (String group : groups) {
            znaleziono = false;
            for (String cg : oldGroups) {
                if (cg.equalsIgnoreCase(group)) {
                    znaleziono = true;
                    break;
                }
            }
            if (!znaleziono) {
                czy = false;
                break;
            }
        }
        return czy;
    }

    public void save() {
        try {
            database.save(yamlDataBaseFile);
            database.load(yamlDataBaseFile);
        } catch (IOException e) {
            logInfo(ChatColor.RED + "I/O ERROR - unable to save database");
            e.printStackTrace();
        } catch (Exception e) {
            logInfo(ChatColor.RED + "OTHER ERROR - unable to save database");
            e.printStackTrace();
        }
    }

    void save(CommandSender cs) {
        try {
            database.save(yamlDataBaseFile);
            database.load(yamlDataBaseFile);
        } catch (IOException e) {
            logInfo(ChatColor.RED + "I/O ERROR - unable to save database");
            cs.sendMessage(ChatColor.RED + "I/O ERROR - unable to save database");
            e.printStackTrace();
        } catch (Exception e) {
            logInfo(ChatColor.RED + "OTHER ERROR - unable to save database");
            e.printStackTrace();
        }
    }

    boolean canSetRank(CommandSender cs, String player, String group) {
        //TODO: this, powiadomienia że nie ma permissions do komendy lub do grupy lub do gracza danego!
        return hasSetRank(cs, group);
    }

    //@Deprecated
    boolean hasSetRank(CommandSender cs, String group) {
        try {
            if (cs instanceof Player) {
                Player player = (Player) cs;
                if (cfg.allowOpsChanges && player.isOp()) {
                    return true;
                } else if (player.hasPermission("azrank.setrank.*") || player.hasPermission("azrank.*")) {
                    return true;
                } else {
                    String node = "azrank.setrank." + group.toLowerCase();
                    return player.hasPermission(node);
                }

            } else return true;
        } catch (Exception e) {
            logSevere(e.getMessage());
            return false;
        }
    }

    boolean hasReload(Player player) {
        try {
            String node = "azrank.reload";
            return player.hasPermission(node);

        } catch (Exception e) {
            logSevere(e.getMessage());
            return false;
        }

    }

    //@Deprecated
    public boolean setGroups(String name, String[] oldGroups) {
        try {
            return permBridge.setPlayersGroups(name, oldGroups);
        } catch (Exception e) {
            logSevere("Exception when setting group|" + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    boolean dLoad() {
        try {
            cfg.checkConfig();
            cfg.loadConfig();
            database.load(yamlDataBaseFile);
            return true;
        } catch (Exception e) {
            logInfo("Error|" + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    void wypiszGraczy(CommandSender cs, int count, int page) {
        try {
            List<AZPlayersGroup> tempGroups = new LinkedList();
            ConfigurationSection usersSection = database.getConfigurationSection("users");
            ConfigurationSection userSection;
            Set<String> players = usersSection.getKeys(false);
            AZPlayersGroup group;
            List<String> restoreGroups;
            for (String playerName : players) {
                userSection = usersSection.getConfigurationSection(playerName);
                for (String groupName : userSection.getKeys(false)) {
                    restoreGroups = userSection.getStringList(groupName + ".restoreGroups");
                    if (restoreGroups.size() > 0)
                        group = new AZPlayersGroup(playerName, groupName, userSection.getLong(groupName + ".to"), restoreGroups.toArray(new String[restoreGroups.size()]));
                    else
                        group = new AZPlayersGroup(playerName, groupName, userSection.getLong(groupName + ".to"));

                    tempGroups.add(group);
                }
            }
            Collections.sort(tempGroups, new Comparator<AZPlayersGroup>() {
                @Override
                public int compare(AZPlayersGroup o1, AZPlayersGroup o2) {
                    int wynik = o1.playerName.compareTo(o2.playerName);
                    if (wynik == 0)
                        return o1.to.compareTo(o2.to);
                    else return wynik;
                }
            });

            int pages;
            int min, max;
            java.util.Date toDate = new java.util.Date();

            if (count > 0) {
                if (tempGroups.size() > 0)
                    pages = 1 + tempGroups.size() / count;
                else
                    pages = 0;
                min = (page - 1) * 10;
                max = page * 10;
                if (max >= tempGroups.size())
                    max = tempGroups.size() - 1;
                cs.sendMessage(ChatColor.RED + "===Temporiary ranks: == PAGE: " + page + "/" + pages + "====");
            } else {
                min = 0;
                max = tempGroups.size();
                cs.sendMessage(ChatColor.RED + "===Temporiary ranks: == " + max + " ====");
            }


            for (int i = min; i < max; i++) {
                group = tempGroups.get(i);
                toDate.setTime(group.to);
                String rg = "";
                if (group.restoreGroups.length > 0)
                    rg = ", later in " + Util.tableToString(group.restoreGroups);
                cs.sendMessage("" + (i + 1) + ". " + group.playerName + " in: " + group.groupName + " to: " + dateformat.format(toDate) + rg);
            }
        } catch (OutOfMemoryError e) {
            cs.sendMessage(ChatColor.RED + "Out of memory error! send ticket on dev.bukkit.org/server-mods/azrank");
        }
    }

    private boolean hasPerm(CommandSender cs, String node) {
        if (cs instanceof Player) {
            Player player = (Player) cs;
            return player.hasPermission(node) || player.hasPermission(ALL_NODE) || (player.isOp() && cfg.allowOpsChanges);
        } else
            return true;
    }

    boolean infoCMD(CommandSender cs, String playername) {
        ConfigurationSection userSection = database.getConfigurationSection("users." + playername);
        String[] groups = permBridge.getPlayersGroups(playername);
        if (userSection == null) {
            cs.sendMessage(ChatColor.GREEN + "[AZRank] " + ChatColor.AQUA + "User " + playername + " is in " + Util.tableToString(groups) + " forever");
        } else {
            String msg = "";
            long to;
            SimpleDateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            java.util.Date toDate;
            List<String> crGroups = new LinkedList<>(Arrays.asList(groups));
            Set<String> keys = userSection.getKeys(false);
            if (keys.size() > 0) {
                msg += ChatColor.YELLOW + " temporiary" + ChatColor.AQUA + ":\n";
            }
            String dgroup;
            for (String group : keys)//dla każdej grupy w bazie danych
            {
                dgroup = group;
                for (String crGroup : crGroups)
                    if (crGroup.equalsIgnoreCase(group)) {
                        dgroup = crGroup;
                        crGroups.remove(crGroup);
                        break;
                    }
                to = database.getLong("users." + playername + "." + group + ".to");
                toDate = new java.util.Date(to);
                List<String> oldGroups = database.getStringList("users." + playername + "." + group + ".restoreGroups");
                String rg = "";
                if (oldGroups != null && oldGroups.size() > 0)
                    rg = ChatColor.AQUA + " later in: " + ChatColor.DARK_AQUA + oldGroups;
                msg += ChatColor.DARK_AQUA + dgroup + ChatColor.AQUA + " to " + ChatColor.DARK_AQUA + dateformat.format(toDate) + rg + "\n";

            }
            String pg = "";
            if (crGroups.size() > 0) {
                pg = ChatColor.YELLOW + " permanently" + ChatColor.AQUA + ": " + ChatColor.DARK_AQUA + crGroups.get(0);
                crGroups.remove(0);
                for (String cg : crGroups) {
                    pg += ChatColor.AQUA + ", " + ChatColor.DARK_AQUA + cg;
                }
                pg += ChatColor.AQUA + ";";
            }
            msg = ChatColor.GREEN + "[AZRank] " + ChatColor.AQUA + "User " + playername + " is in: " + pg + msg;
            cs.sendMessage(msg);
        }
        return true;
    }

    @Override
    public String getName() {
        return "AZRank";
    }
}


