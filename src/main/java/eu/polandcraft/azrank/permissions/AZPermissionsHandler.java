package eu.polandcraft.azrank.permissions;

import eu.polandcraft.azrank.AZRank;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public abstract class AZPermissionsHandler {
    AZRank plugin;

    public abstract String getName();

    public abstract String[] getPlayersGroups(String playerName);

    public abstract boolean setPlayersGroups(String playerName, String[] groups);

    @SuppressWarnings("unchecked")
    public synchronized boolean playerAddGroups(String userName, String[] groups) {
        List<String> crGroups = new ArrayList(Arrays.asList(getPlayersGroups(userName)));
        List<String> addGroups = Arrays.asList(groups);
        crGroups.addAll(addGroups);
        return setPlayersGroups(userName, crGroups.toArray(new String[crGroups.size()]));
    }

    @SuppressWarnings("unchecked")
    public synchronized boolean playerRemoveGroups(String userName, String[] groups) {
        List<String> crGroups = new ArrayList(Arrays.asList(getPlayersGroups(userName)));
        List<String> remGroups = Arrays.asList(groups);
        Iterator<String> iter = crGroups.iterator();
        String crGroup;
        while (iter.hasNext()) {
            crGroup = iter.next();
            for (String remGroup : remGroups) {
                if (crGroup.equalsIgnoreCase(remGroup)) {
                    iter.remove();
                    break;
                }
            }
        }
        return setPlayersGroups(userName, crGroups.toArray(new String[crGroups.size()]));
    }
}
