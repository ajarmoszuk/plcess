package eu.polandcraft.azrank.permissions;

import eu.polandcraft.azrank.AZRank;
import net.milkbowl.vault.permission.Permission;

public class AZVaultAdapter extends AZPermissionsHandler {
    private Permission pp;

    public AZVaultAdapter(AZRank origin, Permission pp) {
        this.plugin = origin;
        this.pp = pp;
    }

    @Override
    public String getName() {
        return (pp.getName());
    }

    @Override
    public String[] getPlayersGroups(String player) {
        String[] groups;
        String defaultWorld = plugin.getServer().getWorlds().get(0).getName();
        try {
            groups = pp.getPlayerGroups((String) null, player);
        } catch (NullPointerException e) {
            groups = pp.getPlayerGroups(defaultWorld, player);
        }
        if (groups == null) {
            return pp.getPlayerGroups(defaultWorld, player);
        } else
            return groups;
    }

    @Override
    public boolean setPlayersGroups(String player, String[] groups) {
        String[] oldGroups;
        String defaultWorld = plugin.getServer().getWorlds().get(0).getName();
        int i = 0;

        try {
            oldGroups = pp.getPlayerGroups((String) null, player);
        } catch (NullPointerException e) {
            oldGroups = pp.getPlayerGroups(defaultWorld, player);
        }

        if (oldGroups == null) { //to GroupManager compatybility
            oldGroups = pp.getPlayerGroups(defaultWorld, player);
        }
        for (String group : oldGroups) {
            if (pp.playerRemoveGroup((String) null, player, group)) i++;
            else if (pp.playerRemoveGroup(defaultWorld, player, group)) i++;
        }

        int j = 0;
        for (String group : groups) {
            if (pp.playerAddGroup((String) null, player, group)) j++;
            else if (pp.playerAddGroup(defaultWorld, player, group)) j++;
        }
        return !(i == 0 || j == 0);
    }
}