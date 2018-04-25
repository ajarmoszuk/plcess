package eu.polandcraft.is;

import org.apache.commons.lang3.tuple.Pair;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.io.InputStreamReader;
import java.io.PrintStream;

public class Package {
    int id; // id
    String name; // name
    String smsIDAccount; // smsid
    String transferIDAccount; // transferid
    String targetServer; // servername
    String servID; // servid
    String price; // cena
    String smsPhoneNumber; // numersms
    String description; // description
    String rank; // rank
    String[] commands; //commands
    int rankDays; // ranknum
    List<Item> items = new ArrayList<>(); // <itemX,numX>
    //List<Pair<String, String>> commands = new ArrayList<>(); // item $user num

    public static Package getPackage(ResultSet rs) throws SQLException {
        if (rs.isAfterLast() || !rs.next()) return null;
        Package pk = new Package();
        pk.id = rs.getInt("ID");
        pk.name = rs.getString("name");
        pk.smsIDAccount = rs.getString("HPAY_SERVICEID");
        pk.transferIDAccount = rs.getString("HPAY_SERVICETID");
        pk.targetServer = rs.getString("serverName");
        pk.servID = rs.getString("serverID");
        pk.price = rs.getString("price");
        pk.smsPhoneNumber = rs.getString("smsNumber");
        pk.description = rs.getString("description");
        pk.rank = rs.getString("rank");
        pk.commands = rs.getString("commands").split("\\r?\\n");
        try {
            pk.rankDays = Integer.parseInt(rs.getString("ranknum"));
        } catch (NumberFormatException e) {
        }

        if (pk.id > 0) return pk;
        return null;
    }

    public static List<Package> getPackages(ResultSet rs) throws SQLException {
        List<Package> p = new ArrayList<>();
        while (!rs.isLast()) {
            Package pk = getPackage(rs);
            if (pk instanceof Package) p.add(pk);
        }
        return p;
    }
}
