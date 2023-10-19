package de.yugata.easy.edits.util;

public class StringUtil {


    public static String substringUntil(final String s, final String delimiter, final int startPos) {
        return s.substring(startPos, s.indexOf(delimiter, startPos));
    }

    public static String substringUntil(final String s, final String delimiter) {
        return s.substring(0, s.indexOf(delimiter));
    }
}
