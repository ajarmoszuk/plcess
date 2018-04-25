package eu.polandcraft.azrank;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

abstract class Util {
    //This is code from essentials plugin.
    static long parseTimeDiffInMillis(String time) throws Exception {
        Pattern timePattern = Pattern.compile(
                "(?:([0-9]+)\\s*y[a-z]*[,\\s]*)?"
                        + "(?:([0-9]+)\\s*mo[a-z]*[,\\s]*)?"
                        + "(?:([0-9]+)\\s*w[a-z]*[,\\s]*)?"
                        + "(?:([0-9]+)\\s*d[a-z]*[,\\s]*)?"
                        + "(?:([0-9]+)\\s*h[a-z]*[,\\s]*)?"
                        + "(?:([0-9]+)\\s*m[a-z]*[,\\s]*)?"
                        + "(?:([0-9]+)\\s*(?:s[a-z]*)?)?", Pattern.CASE_INSENSITIVE);
        Matcher m = timePattern.matcher(time);
        long seconds = 0;
        boolean found = false;
        while (m.find()) {
            if (m.group() == null || m.group().isEmpty()) {
                continue;
            }
            for (int i = 0; i < m.groupCount(); i++) {
                if (m.group(i) != null && !m.group(i).isEmpty()) {
                    found = true;
                    break;
                }
            }
            if (found) {
                if (m.group(1) != null && !m.group(1).isEmpty()) { //years
                    seconds += 31556926 * Integer.parseInt(m.group(1));
                }
                if (m.group(2) != null && !m.group(2).isEmpty()) { //months
                    seconds += 2629743 * Integer.parseInt(m.group(2));
                }
                if (m.group(3) != null && !m.group(3).isEmpty()) { //weeks
                    seconds += 604800 * Integer.parseInt(m.group(3));
                }
                if (m.group(4) != null && !m.group(4).isEmpty()) { //days
                    seconds += 86400 * Integer.parseInt(m.group(4));
                }
                if (m.group(5) != null && !m.group(5).isEmpty()) { //hours
                    seconds += 3600 * Integer.parseInt(m.group(5));
                }
                if (m.group(6) != null && !m.group(6).isEmpty()) { //minutes
                    seconds += 60 * Integer.parseInt(m.group(6));
                }
                if (m.group(7) != null && !m.group(7).isEmpty()) { //seconds
                    seconds += Integer.parseInt(m.group(7));
                }
                break;
            }
        }
        if (!found) {
            throw new Exception("illegalDate");
        }
        return seconds * 1000;

    }

    static String tableToString(String[] tbl) {
        String wynik = "[";
        if (tbl.length > 0) {
            wynik += tbl[0];
            for (int i = 1; i < tbl.length; i++) {
                wynik += "," + tbl[i];
            }
        }
        wynik += "]";
        return wynik;
    }
}
