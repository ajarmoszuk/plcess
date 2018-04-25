package eu.polandcraft.is;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class MysqlDB implements Database {
    String itemshopTable;
    String serverName;
    String packsTable;
    String dbUser;
    String dbPass;
    String dbName;
    String dbHost;
    String dbPort = "3306";
    protected Connection conn;

    public MysqlDB(String user, String pass, String name, String host, String table, String server, String packs) {
        dbUser = user;
        dbPass = pass;
        dbName = name;
        dbHost = host;
        itemshopTable = table;
        serverName = server;
        packsTable = packs;
    }

    @Override
    public boolean load() throws SQLException {
        try {
            String jdbc = "jdbc:mysql://%s:%s/%s?autoReconnect=true";
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(String.format(jdbc, dbHost, dbPort, dbName), dbUser, dbPass);
            return true;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public List<Package> getPackages() {
        if (conn == null) return new ArrayList<>();
        try {
            String query = String.format("SELECT * FROM %s WHERE servername = ? ORDER BY id", itemshopTable);
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setString(1, serverName);

            ResultSet rs = ps.executeQuery();
            List<Package> res = Package.getPackages(rs);
            return res;
        } catch (SQLException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    @Override
    public Package getPackage(int id) {
        if (conn == null) return null;
        try {
            String query = String.format("SELECT * FROM %s WHERE servername = ? AND id = ?", itemshopTable);
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setString(1, serverName);
            ps.setInt(2, id);

            ResultSet rs = ps.executeQuery();
            Package res = Package.getPackage(rs);
            return res;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public boolean checkConnection() {
        return conn != null;
    }

    private int getNextPanelPackageId(String user) {
        try {
            String query = String.format("SELECT pakietId FROM %s WHERE servername = ? AND username = ? ORDER BY pakietId LIMIT 1", "panelpacks");
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setString(1, serverName);
            ps.setString(2, user);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int pakietId = rs.getInt("pakietId");
                return pakietId;
            } else
                return 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }
	
  @Override
  public boolean sendLog(String user, String name, String code, String packagePrice, String method) {
    try {
      String query = String.format("INSERT INTO transactions (playerName,packageName,transactionID,date,type,server,price,yearAndMonth,method,payerEmail) VALUES (?,?,?,?,?,?,?,?,?,?)");
      PreparedStatement ps = conn.prepareStatement(query);
	  DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      DateFormat dateAndMonth = new SimpleDateFormat("yyyy-MM");
      Date date = new Date();
      ps.setString(1, user);
      ps.setString(2, name);
      ps.setString(3, code);
      ps.setString(4, dateFormat.format(date));
	  ps.setString(5, "Serwer");
	  ps.setString(6, serverName);
	  ps.setString(7, packagePrice);
      ps.setString(8, dateAndMonth.format(date));
      ps.setString(9, method);
      ps.setString(10, "Brak");
      ps.execute();
	  return true;
      } catch (SQLException e) {
          e.printStackTrace();
          return false;
        }
  }

    @Override
    public Package getNextPanelPackage(String user) {
        int id = getNextPanelPackageId(user);
        if (id != 0) {
            return getPackage(id);
        }
        return null;
    }

    @Override
    public boolean removeNextPanelPackage(String user) {
        try {
            String query = String.format("DELETE FROM %s WHERE servername = ? AND username = ? ORDER BY pakietId LIMIT 1", "panelpacks");
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setString(1, serverName);
            ps.setString(2, user);
            ps.execute();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
