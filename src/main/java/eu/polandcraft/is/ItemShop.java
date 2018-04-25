package eu.polandcraft.is;

import eu.polandcraft.PLCEss;
import eu.polandcraft.PluginModule;
import eu.polandcraft.util.ConfigAccessor;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.sql.SQLException;
import java.util.*;

public class ItemShop extends PluginModule implements CommandExecutor {
    private Database db;
    private PLCEss plugin;
    private ConfigAccessor confAcc;
    private FileConfiguration config;
    private final String prefix = ChatColor.GREEN + "[ItemShop] ";
    private String smsID;

    public ItemShop(PLCEss plug) {
        this.plugin = plug;
    }

    @Override
    public boolean onEnable() {
        confAcc = new ConfigAccessor(plugin, "itemshop.yml");
        config = confAcc.getConfig();
        confAcc.saveDefaultConfig();

        String user = config.getString("MySQL.User", "root");
        String pass = config.getString("MySQL.Password", "");
        String dbName = config.getString("MySQL.Database", "minecraft");
        String host = config.getString("MySQL.Host", "localhost");
        String serverName = config.getString("MySQL.Category", "metropolic");
        String table = config.getString("MySQL.TableIS", "itemshop");
        String packs = config.getString("MySQL.TablePacks", "panelpacks");
        db = new MysqlDB(user, pass, dbName, host, table, serverName, packs);

        try {
            db.load();
        } catch (SQLException e) {
            logInfo("Nie działa baza danych :( - " + e.getMessage());
        }

        plugin.getCommand("is").setExecutor(this);
        return true;
    }

    @Override
    public void onDisable() {
    }

    @Override
    public String getName() {
        return "ItemShop";
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
        if (!db.checkConnection()) {
            error(commandSender, "Nie działa baza danych...a mogło być tak pięknie :(");
            return true;
        }

        switch (args.length) {
            case 0:
                pokazPakiety(commandSender);
                return true;
            case 1:
                if (args[0].equals("kup")) pokazPakiety(commandSender);
                if (args[0].equals("odbierz")) odbierzPakiety(commandSender);
                return true;
            case 2:
                if (args[0].equals("kup")) {
                    pokazPakiet(commandSender, args[1]);
                    return true;
                }
                break;
            case 3:
                kupPakiet(commandSender, args[1], args[2]);
                return true;
        }
        error(commandSender, "Nie ma takiej komendy.");
        return true;
    }

    private void odbierzPakiety(CommandSender sender) {
        Package pk = db.getNextPanelPackage(sender.getName());
        if (pk != null) {
            if (db.removeNextPanelPackage(sender.getName())) {
                dajPakiet(sender, pk);
            } else
                error(sender, "Emm...coś poszło nie tak >_<");
        } else {
            error(sender, "Nie masz żadnych pakietów do odebrania!");
        }
    }

    private void pokazPakiety(CommandSender sender) {
        List<Package> a = db.getPackages();
        Iterator i = a.iterator();
        int j = 1;
        if (a.isEmpty()) {
            error(sender, "Brak pakietów. WTF?");
            return;
        }
        while (i.hasNext()) {
            Package p = (Package) i.next();
            sender.sendMessage((++j % 2 > 0 ? ChatColor.GREEN : ChatColor.AQUA) + String.format("Pakiet nr. %s - %s za %szł", p.id, p.name, p.price));
        }
        sender.sendMessage(ChatColor.RED + "Aby zakupić pakiet użyj komendy: /is kup numerpakietu");
    }

    private void pokazPakiet(CommandSender sender, String packetID) {
        try {
            int id = Integer.parseInt(packetID.trim());
            Package p = db.getPackage(id);
            if (p instanceof Package) {
                sender.sendMessage(ChatColor.GREEN + "-- " + p.name + " --");
                sender.sendMessage("");
				sender.sendMessage(p.description);
				sender.sendMessage("");
                sender.sendMessage(ChatColor.AQUA + String.format("Aby zakupić pakiet %s możesz użyć cztery opcje płatności:\n-----SMS-----\nWyślij SMS o treści MSMS.PLC na numer %s.\n\n-----PRZELEW-----\nWyślij przelew poprzez:\nhttps://ssl.homepay.pl/wplata/%s-PLC%s\n\n-----PAYPAL-----\nZapłać przez PayPal:\nhttp://panel.polandcraft.eu/shop/paypal/%s\n\n-----VOUCHER-----\nMożesz także użyć vouchera promocyjnego.\n\n\nSuma płatności to %szł brutto.", p.name, p.smsPhoneNumber, p.transferIDAccount, p.price.replaceAll("\\W", ""), p.id, p.price));
                sender.sendMessage("");
                sender.sendMessage("");
                sender.sendMessage(ChatColor.YELLOW + String.format("Aby otrzymać pakiet wpisz /is kup %d <otrzymanykod>", id));
                sender.sendMessage(ChatColor.GREEN + "---------------------------------------");
            } else {
                error(sender, "Pakiet o takim ID nie istnieje.");
            }
        } catch (NumberFormatException e) {
            error(sender, "Nieprawidłowy numer pakietu.");
        }
    }

    private void kupPakiet(CommandSender sender, String packetID, String kodSMS) {
        try {
            int id = Integer.parseInt(packetID.trim());
            Package pk = db.getPackage(id);
            String method = new String("");
            String player = sender.getName();
            if (pk instanceof Package) {
                if (sender instanceof Player) {
                    Player p = (Player) sender;
                    ItemStack[] inv = p.getInventory().getContents();
                    ArrayList<ItemStack> ar = new ArrayList<>(Arrays.asList(inv));

                    int free = Collections.frequency(ar, null);
                    int missing = pk.items.size() - free;
                    if (pk.items.size() > free) {
                        String msg = "Masz za mało miejsca w EQ żeby kupić ten pakiet! Brakuje Ci %d wolnych slotów!";
                        error(p, String.format(msg, missing));
                        return;
                    }
                }
                String smsID = "1141";
                String smsAcc = "1500";
                if (sprawdzSMS(sender, smsID, smsAcc, kodSMS, pk.smsPhoneNumber)) {
                    dajPakiet(sender, pk);
                    method = "SMS";
					db.sendLog(sender.getName(), pk.name, kodSMS, pk.price, method);
					
                } /*else if (sprawdzTransfer(sender, smsID, pk.transferIDAccount, kodSMS)) {
                    dajPakiet(sender, pk);
                    method = "Przelew";
					db.sendLog(sender.getName(), pk.name, kodSMS, pk.price, method);
					
                }*/ else if (sprawdzVoucher(sender, player, pk.smsPhoneNumber, pk.targetServer, kodSMS)) {
                    dajPakiet(sender, pk);
                    method = "Voucher";
					db.sendLog(sender.getName(), pk.name, kodSMS, pk.price, method);
					
                } else {
                    error(sender, "Twój kod jest niepoprawny lub nie istnieje.");
                }
            } else {
                error(sender, "Pakiet o takim ID nie istnieje.");
            }
        } catch (NumberFormatException e) {
            error(sender, "Nieprawidłowy numer pakietu.");
        }
    }

    private void dajPakiet(CommandSender cs, Package p) {
        dajRangeOrazKomendy(cs, p);
        kupilPakiet(cs, p);
    }

    private void dajRangeOrazKomendy(CommandSender cs, Package p) {
        if (cs instanceof ConsoleCommandSender) {
            if (!p.rank.isEmpty())
                cs.sendMessage(prefix + ChatColor.AQUA + String.format("Ranga w pakiecie: '%s' na %d dni", p.rank, p.rankDays));
        } else {
            if (!p.rank.isEmpty()) {
                if (plugin.getAZRank().isEnabled()) {
                    plugin.getAZRank().givePlayerTmpGroup(cs.getName(), p.rank, 60 * 60 * 24 * 1000L * p.rankDays);
                    //cs.sendMessage(prefix + ChatColor.BLUE + "Dostałeś rangę " + p.rank + " na " + p.rankDays + " dni");
                } else {
                    error(cs, "AZRank nie działa :(");
                }
            }
             for (String com : p.commands) {
               com = com.replace("[p]", cs.getName());
               Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), com);
            }
        }
    }

    private void error(CommandSender cs, String msg) {
        cs.sendMessage(prefix + ChatColor.RED + msg);
    }

    private void kupilPakiet(CommandSender cs, Package pk) {
        cs.sendMessage(prefix + ChatColor.BLUE + "Dostałeś pakiet " + pk.name);
        Bukkit.getServer().broadcastMessage(ChatColor.BLUE + "[ItemShop] " + ChatColor.YELLOW + String.format("Gracz %s kupił pakiet %s!", cs.getName(), pk.name));
    }

    private boolean sprawdzSMS(CommandSender sender, String smsID, String smsAcc, String code, String smsNumber) {
        try {
            String www = String.format("http://microsms.pl/api/check.php?userid=%s&number=%s&code=%s&serviceid=%s", smsID, smsNumber, code, smsAcc);
            //String www = String.format("http://homepay.pl/API/check_code.php?usr_id=%s&acc_id=%s&code=%s", smsID, smsAcc, code); //nope
            URLConnection yc = new URL(www).openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream()));
            String inputLine = in.readLine();
            //int kod = Integer.parseInt(inputLine.trim());
            String kodString = inputLine.substring(0, 1);
            int kod = Integer.parseInt(kodString);
            //sender.sendMessage(inputLine + www + kodString);
			return (code.equals("testowy") && sender.isOp()) || kod == 1;
		} catch (Exception e) {
            return false;
        }
    }

    private boolean sprawdzTransfer(CommandSender sender, String smsID, String smsAcc, String code) {
        try {
            String www = String.format("http://homepay.pl/API/check_tcode.php?usr_id=%s&acc_id=%s&code=%s", smsID, smsAcc, code);
            URLConnection yc = new URL(www).openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream()));
            String inputLine = in.readLine();
            int kod = Integer.parseInt(inputLine.trim());
			return (code.equals("testowy") && sender.isOp()) || kod == 1;
		} catch (Exception e) {
            return false;
        }
    }

    private boolean sprawdzVoucher(CommandSender sender, String user, String smsNumber, String serverName, String code) {
        try {
            String www = String.format("https://panel.polandcraft.eu/utils/functions/voucher.php?name=%s&smsNumber=%s&server=%s&code=%s", user, smsNumber, serverName, code);
            URLConnection yc = new URL(www).openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream()));
            String inputLine = in.readLine();
            int kod = Integer.parseInt(inputLine.trim());
            return (code.equals("testowy") && sender.isOp()) || kod == 1;
        } catch (Exception e) {
            return false;
        }
    }
}