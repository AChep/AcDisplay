package com.achep.acdisplay.utils;

/**
 * Created by Artem Chepurnoy on 15.09.2014.
 */
public class StringUtils {

    /**
     * Removes all kinds of multiple spaces from given string.
     */
    public static String removeSpaces(CharSequence cs) {
        if (cs == null) return null;
        String string = cs instanceof String
                ? (String) cs : cs.toString();
        return string
                .replaceAll("(\\s+$|^\\s+)", "")
                .replaceAll("\n+", "\n");
    }

}
