package eu.polandcraft.azrank;

public class AZPlayersGroup {
    public String playerName;
    public String groupName;
    public Long to;
    public String[] restoreGroups = new String[]{};

    public AZPlayersGroup(String playerName, String groupName, long to) {
        this.playerName = playerName;
        this.groupName = groupName;
        this.to = to;
    }

    public AZPlayersGroup(String playerName, String groupName, long to, String[] restoreGroups) {
        this.playerName = playerName;
        this.groupName = groupName;
        this.to = to;
        if (restoreGroups != null)
            this.restoreGroups = restoreGroups;
    }
}
