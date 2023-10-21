package de.yugata.easy.edits.util;

public class StringUtil {
    /**
     * Checks whether a given char sequence is numeric, this includes decimal points,
     * thereby, the method is very basic and a string such as 1....0 would be accepted.
     * We could solve this by either using regex or just allowing for one dot.
     *
     * @param cs the char-sequence to test
     * @return whether that string only contains numbers or dots
     */
    public static boolean isDecimalNumeric(CharSequence cs) {
        if (cs == null || cs.length() == 0) {
            return false;
        } else {
            final int length = cs.length();
            boolean dot = false;

            for (int i = 0; i < length; ++i) {
                if (cs.charAt(i) == '.')
                    if (dot)
                        return false;
                    else
                        dot = true;
                else if (!Character.isDigit(cs.charAt(i))) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Checks whether a given char sequence is an integer
     *
     * @param cs the char-sequence to test
     * @return whether that string only contains numbers only
     */
    public static boolean isInteger(CharSequence cs) {
        if (cs == null || cs.length() == 0) {
            return false;
        } else {
            final int length = cs.length();

            for (int i = 0; i < length; ++i) {
                if (!Character.isDigit(cs.charAt(i))) {
                    return false;
                }
            }
            return true;
        }
    }
}
