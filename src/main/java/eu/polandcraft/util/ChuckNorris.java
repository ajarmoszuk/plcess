package eu.polandcraft.util;

import eu.polandcraft.PLCEss;
import eu.polandcraft.PluginModule;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.Scanner;
import org.bukkit.event.Listener;
import org.bukkit.command.CommandSender;
import org.bukkit.event.player.PlayerJoinEvent;

public class ChuckNorris extends PluginModule {
  private PLCEss plugin;
  private ConfigAccessor confAcc;
  private FileConfiguration config;

  public ChuckNorris(PLCEss plug) {
      plugin = plug;
  }

  @Override
  public String getName() {
      return "ChuckNorris";
  }
  
  @Override
  public boolean onEnable() {
	plugin.getServer().getPluginManager().registerEvents(this, plugin);
	confAcc = new ConfigAccessor(plugin, "chuck.yml");
    config = confAcc.getConfig();
    confAcc.saveDefaultConfig();
    return true;
  }
  
  @Override
  public void onDisable() {
  }
  
  /* inefficient code, todo later
  @EventHandler(priority=EventPriority.LOWEST)
  public void onLogin(PlayerLoginEvent event) {
      try {
        String url = "http://botscout.com/test/?ip=" + event.getAddress().getHostAddress() + "&key=2cAwndCrWuYpH9q";
        Scanner scanner = new Scanner(new URL(url).openStream());
        if (scanner.findInLine("Y") != null) {
          event.disallow(PlayerLoginEvent.Result.KICK_OTHER, "[ChuckNorris] Uzywasz bramki proxy, polaczenie zatrzymane!");
        }
        scanner.close();
      } catch (MalformedURLException exception) {
        exception.printStackTrace();
      } catch (IOException exception) {
        exception.printStackTrace();
      }
  }
  */
  
  @EventHandler(priority = EventPriority.LOW)
  public void onPlayerJoin(PlayerJoinEvent join) {
	  join.getPlayer().sendMessage(ChatColor.AQUA + "------------------------------------------");
	  join.getPlayer().sendMessage(ChatColor.AQUA + "Witaj na serwerze sieci PolandCraft.eu!");
	  join.getPlayer().sendMessage(ChatColor.AQUA + "Nasze forum: http://polandcraft.eu/");
	  join.getPlayer().sendMessage(ChatColor.AQUA + "------------------------------------------");
	  
	  if (censorTime() >= 21 && censorTime() < 8) {
          join.getPlayer().sendMessage(ChatColor.RED + "[ChuckNorris] " + ChatColor.WHITE + "Są godziny wieczorne, przekleństwa są dozwolone!");
      } else {
		  join.getPlayer().sendMessage(ChatColor.RED + "[ChuckNorris] " + ChatColor.WHITE + "Uwaga! Zostaniesz wyrzucony z serwera jeżeli zaczniesz przeklinać!");
	  }
  }
  
  @EventHandler
  public void onPlayerChat(AsyncPlayerChatEvent event) { 
	String m = event.getMessage();
    final Player p = event.getPlayer();
		for (String c : config.getStringList("censored"))
		if (p.isOp()) {
			event.setCancelled(false);
		} else if (m.toLowerCase().contains(c.toLowerCase())) {
			if (censorTime() >= 21 && censorTime() < 8) {
				event.setCancelled(false);
			break;
			} else {
				event.setCancelled(true);
				//banPlayer(p,"5","Przeklenstwa");
				p.sendMessage(ChatColor.RED + "[ChuckNorris] " + ChatColor.WHITE + "Nie przeklinaj!");
				break;
			}
		}
		for (String h : config.getStringList("blockedhosts"))
		if (!p.isOp() && (advert(p, m) || m.toLowerCase().contains(h.toLowerCase()))) {
				event.setCancelled(true);
				if (censorTime() == 20) {
					p.sendMessage(ChatColor.RED + "[ChuckNorris] " + ChatColor.WHITE + "Jeśli jeszcze raz ogłosisz swój gówniany serwer to osobiście przyjde do twojego domu i doczołowo spieprze twoje oczy.");
					break;
				} else {
					p.sendMessage(ChatColor.RED + "[ChuckNorris] " + ChatColor.WHITE + "Nasz system wykrył próbę reklamy serwera. Jest to niedozwolone, akcja została zalogowana do pliku.");
				}
				writeLog(p, m);
				break;
			}
		}
		
	public void banPlayer(Player gp, String time, String reason) {
		final String p = gp.getName();
		final String t = time;
		final String r = reason;
		plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() { 
		    public void run() { 
		        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "tempban " + p +" " + t + " m " + r); 
		        }
	    });
	}
	
	public void writeLog(Player gp, String message) {
		BufferedWriter bufferedWriter = null;
		String p = gp.getName();
        String ip = gp.getAddress().getAddress().getHostAddress();
        try {
            bufferedWriter = new BufferedWriter(new FileWriter("plugins/PLCEss/logs/Adverts.log", true));
            bufferedWriter.write("[" + currentTime() + "] <" + p + " @ " + ip + "> " + message);
            bufferedWriter.newLine();
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (bufferedWriter != null) {
                    bufferedWriter.flush();
                    bufferedWriter.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
	}
	public boolean checkForIp(String str) {
		  String ipPattern = "\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}";
	      Pattern r = Pattern.compile(ipPattern);
	      Matcher m = r.matcher(str);
	      if (m.find( )) {
	    	return true;
	      }
		return false; 
	}
	
	public boolean checkForDomain(String str) {
		  String domainPattern = "([0-9a-z_\\-]{2,}\\.)+[a-z]{2,}(:\\d*)?";
	      Pattern r = Pattern.compile(domainPattern);
	      Matcher m = r.matcher(str);
	      if (m.find( )) {
	    	return true;
	      }
		return false; 
	}
	
	public boolean advert(Player p, String m) {
		for (String w : config.getStringList("whitelist")) {
			if (m.toLowerCase().matches(w.toLowerCase())) {
				return false;
			}
		}
		if (checkForIp(m) || checkForDomain(m)) {
			return true;
		}
		return false;
	}
	
	public String currentTime() {
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Calendar cal = Calendar.getInstance();
		return dateFormat.format(cal.getTime());
	}
	
	public int censorTime() {
		Calendar now = Calendar.getInstance();
		return now.get(Calendar.HOUR_OF_DAY);
	}
}