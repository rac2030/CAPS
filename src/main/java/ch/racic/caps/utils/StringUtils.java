/*
 * Copyleft (c) 2015. This code is for learning purposes only.
 * Do whatever you like with it but don't take it as perfect code.
 * //Michel Racic (http://rac.su/+)//
 */

package ch.racic.caps.utils;

/**
 * Created by rac on 22.02.15.
 */
public class StringUtils {

    /**
     * Replaces the last occurrence of the given regex (can be a simple String) in a String.
     *
     * @param text
     * @param regex
     * @param replacement
     * @return modified text
     */
    public static String replaceLast(final String text, final String regex, final String replacement) {
        return text.replaceFirst("(?s)(.*)" + regex, "$1" + replacement);
    }
}
