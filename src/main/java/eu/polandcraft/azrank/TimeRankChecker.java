package eu.polandcraft.azrank;

import org.bukkit.configuration.ConfigurationSection;

import java.util.Calendar;
import java.util.List;
import java.util.Set;

class TimeRankChecker implements Runnable {
    private AZRank plugin;

    TimeRankChecker(AZRank origin) {
        this.plugin = origin;
    }

    @Override
    public void run() {
        try {
            long now = Calendar.getInstance().getTimeInMillis();
            ConfigurationSection usersSection = plugin.database.getConfigurationSection("users");

            if (usersSection != null) {
                Set<String> usersNames = usersSection.getKeys(false);
                int tempranks = 0;
                for (String userName : usersNames) {
                    long to = plugin.database.getLong("users." + userName + ".to");
                    if (to == 0) { //version >= 1.3.0
                        //TODO: usuwanie jak nie ma .to
                        ConfigurationSection userSection = plugin.database.getConfigurationSection("users." + userName);
                        Set<String> groups = userSection.getKeys(false);

                        if (groups.size() > 0) {
                            for (String group : groups) {
                                tempranks++;
                                to = plugin.database.getLong("users." + userName + "." + group + ".to");
                                if (to < now) {
                                    List<String> restoreGroups = userSection.getStringList(group + ".restoreGroups");
                                    if (restoreGroups != null) {
                                        plugin.permBridge.playerAddGroups(userName, restoreGroups.toArray(new String[restoreGroups.size()]));
                                        for (String rGroup : restoreGroups)//usuwanie danych o grupach które mają zostać przywrócone.
                                        {
                                            userSection.set(rGroup, null);
                                        }
                                    }
                                    plugin.permBridge.playerRemoveGroups(userName, new String[]{group});
                                    plugin.database.set("users." + userName + "." + group, null);
                                    plugin.save();
                                }
                            }
                        } else {
                            usersSection.set(userName, null);
                        }

                    } else {//capability with 1.2.5 and older versions
                        if (to < now) {
                            List<String> oldGroups = plugin.database.getStringList("users." + userName + ".oldRanks");

                            if (oldGroups.size() > 0) {
                                String[] groups = new String[oldGroups.size()];
                                for (int i = 0; i < oldGroups.size(); i++) {
                                    groups[i] = oldGroups.get(i);
                                }
                                try {
                                    if (plugin.setGroups(userName, groups)) {
                                        plugin.logInfo("unranked user " + userName + " to group(s) " + oldGroups);
                                        plugin.database.set("users." + userName, null);
                                        plugin.save();
                                    } else {
                                        String oldGroupsS = "[";
                                        if (oldGroups.size() > 0) {
                                            oldGroupsS += oldGroups.get(0);    // start with the first element
                                            for (int i = 1; i < oldGroups.size(); i++) {
                                                oldGroupsS += ", " + oldGroups.get(i);
                                            }
                                        }
                                        oldGroupsS += "]";
                                        plugin.logSevere("Failed to restore group for " + userName + " to " + oldGroupsS + ".\nYou should manualy retore player groups in permissions manager, and later in database.yml");
                                    }
                                } catch (Exception e) {
                                    plugin.logSevere(e.getMessage());
                                    e.printStackTrace();
                                }

                            } else {
                                plugin.logSevere("Failed to unrank user " + userName + "! He haven't 'oldGroups'");
                                plugin.database.set("users." + userName, null);
                                plugin.save();
                            }
                        } else {
                            String[] crGroups = plugin.permBridge.getPlayersGroups(userName);
                            ConfigurationSection userSection = plugin.database.getConfigurationSection("users." + userName);
                            for (String crGroup : crGroups) {
                                userSection.set(crGroup + ".to", to);
                                userSection.set(crGroup + ".restoreGroups", userSection.getStringList("oldRanks"));
                                userSection.set("to", null);
                                userSection.set("oldRanks", null);
                            }
                            plugin.save();
                        }
                    }
                }
                plugin.tempranks = tempranks;
            }

        } catch (Exception e) {
            plugin.logSevere(e.getMessage());
            e.printStackTrace();
        }
    }
}
