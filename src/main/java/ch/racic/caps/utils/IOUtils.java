/*
 * Copyleft (c) 2015. This code is for learning purposes only.
 * Do whatever you like with it but don't take it as perfect code.
 * //Michel Racic (http://rac.su/+)//
 */

package ch.racic.caps.utils;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by rac on 21.02.15.
 */
public class IOUtils {
    private static final int BUFFER = 4096;
    private static final String DEFAULT_ENCODING = "UTF-8";

    /**
     * Simple looper which takes an InputStream fully and returns it as String.
     *
     * @param in       filled InputStream
     * @param encoding target encoding for the string
     * @return full content as String with target encoding
     * @throws IOException
     */
    public static String toString(InputStream in, String encoding) throws IOException {
        StringBuilder sb = new StringBuilder();
        byte[] buffer = new byte[BUFFER];
        int length;
        while ((length = in.read(buffer)) != -1) {
            sb.append(new String(buffer, 0, length, encoding));
        }

        in.close();

        return sb.toString();
    }

    /**
     * Simple looper which takes an InputStream fully and returns it as UTF-8 String.
     *
     * @param in filled InputStream
     * @return full content as UTF-8 String
     * @throws IOException
     */
    public static String toString(InputStream in) throws IOException {
        // Taking a default value for encoding
        return toString(in, DEFAULT_ENCODING);
    }

    /**
     * Use this classes ClassLoader to get the resource InputStream and convert it to a String.
     *
     * @param path     relative path in the ClassLoader, e.g. in resource directories
     * @param encoding target encoding for the string
     * @return full content as String with target encoding
     * @throws IOException
     */
    public static String resourceAsString(String path, String encoding) throws IOException {
        return toString(IOUtils.class.getClassLoader().getResourceAsStream(path), encoding);
    }

    /**
     * Use this classes ClassLoader to get the resource InputStream and convert it to a UTF-8 String.
     *
     * @param path relative path in the ClassLoader, e.g. in resource directories
     * @return full content as String with target encoding
     * @throws IOException
     */
    public static String resourceAsString(String path) throws IOException {
        return resourceAsString(path, DEFAULT_ENCODING);
    }
}
