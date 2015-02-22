/*
 * Copyleft (c) 2015. This code is for learning purposes only.
 * Do whatever you like with it but don't take it as perfect code.
 * //Michel Racic (http://rac.su/+)//
 */

package ch.racic.caps.utils;

/**
 * Created by rac on 22.02.15.
 */
public class FileUtils {

    /**
     * Tries to get the extension of the given file path. It will return the content of the String after the last dot or
     * an empty String if no dot exists at all.
     *
     * @param fullPath
     * @return extension string
     */
    public static String getFileExtension(final String fullPath) {
        if (fullPath.lastIndexOf(".") != -1 && fullPath.lastIndexOf(".") != 0) {
            return fullPath.substring(fullPath.lastIndexOf(".") + 1);
        } else {
            return "";
        }
    }

    /**
     * Replaces the current extension with something else.
     *
     * @param fullPath
     * @param newExtension
     * @return modified String
     */
    public static String replaceExtension(final String fullPath, final String newExtension) {
        final String oldExtension = getFileExtension(fullPath);
        return StringUtils.replaceLast(fullPath, oldExtension, newExtension);
    }

}
