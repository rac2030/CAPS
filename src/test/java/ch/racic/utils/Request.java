package ch.racic.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;

/**
 * @Author Michel Racic (m@rac.su) Date: 14.08.12
 */
public class Request {
    public static String readUrl(URL url, Proxy proxy) throws Exception {
        String ret = "";
        BufferedReader in = null;
        URLConnection con = null;
        try {
            con = url.openConnection(proxy);
            in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null)
                ret += inputLine;
        } catch (Exception e) {
            //ret = e.getLocalizedMessage();
            throw e;
        } finally {
            if (in != null)
                in.close();
            if (con != null)
                con = null;
        }
        return ret;
    }
}
