package eu.polandcraft.is;

import java.sql.SQLException;
import java.util.List;

interface Database {
    public boolean load() throws SQLException;

    public List<Package> getPackages();

    public Package getPackage(int id);

    public boolean checkConnection();

    public Package getNextPanelPackage(String user);
	
	public boolean sendLog(String user, String name, String code, String packagePrice, String method);

    public boolean removeNextPanelPackage(String user);
}
