package ch.racic.caps;

/**
 * @Author Michel Racic (m@rac.su)
 * Date: 12.08.12
 */

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import javax.net.SocketFactory;
import javax.net.ssl.*;
import java.io.*;
import java.net.Socket;
import java.nio.charset.Charset;
import java.security.KeyStore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProxyThread extends Thread {
    /**
     * Logging helper
     */
    private final Logger logger;

    private Socket clientSocket = null;
    private static final int BUFFER_SIZE = 32768;
    private boolean debug = false, debugSSL = false;
    private boolean silent = true;

    /**
     * SSL statics *
     */
    private static TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(
                        java.security.cert.X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(
                        java.security.cert.X509Certificate[] certs, String authType) {
                }
            }
    };

    /**
     * SSL config *
     */
    private File trustStoreFile = null;
    private String trustStorePassword = null;
    private File keyStoreFile = null;
    private String keyStorePassword = null;
    private String keyStoreType = null;

    /**
     * precompile regex patterns *
     */
    private static Pattern patternHost = Pattern.compile("(?<=Host: )([^:\t\r\n]+)(?::?)(([0-9]+)?)");
    private static Pattern patternContentLength = Pattern.compile("(?<=Content-Length: )(([0-9]+))");
    //private static Pattern patternChunckSize = Pattern.compile("([0-9a-fA-F]+)(?<=\0x8d\0xa\0x8d\0xa)");
    private static Pattern patternChunckSize = Pattern.compile("([0-9a-fA-F]+)(\r\n\r\n)");
    private static Pattern pattern304 = Pattern.compile(" 304 Not Modified\r\n");
    private static Pattern patternLocation = Pattern.compile("(?<=Location: )([^:\t\r\n]+)");


    /**
     * run variables *
     */
    private String targetHost;
    private int targetPort;
    private boolean ssl;
    private Socket hostSocket;

    public ProxyThread(Socket socket) {
        super("ProxyThread");
        this.clientSocket = socket;
        logger = Logger.getLogger("ProxyThread_" + this.getId());
    }

    public void run() {
        /** Generell proxy description:
         * 1) Create a socket and wait for user input (request)
         * 2) Create a SSL socket to the target host (beeing a MITM and SSL end point)
         * 3) Pipe the user request to the target server
         * 4) Receive the server response and pipe it back to the user
         */

            messagePrint("ProxyThread started");
        CAPServer.getInstance().ptStartNotification();

        try {
            /** Create a temporary buffer **/
            byte[] buffer = new byte[BUFFER_SIZE];
            /** get the input stream of the user socket **/
            InputStream bis = clientSocket.getInputStream();

            /** reading the request and put it into buffer **/
            int n = bis.read(buffer);
            logger.debug("filled buffer size N=" + n);
            /** Getting the browser request into a string to extract the connection details **/
            String browserRequest = new String(buffer, 0, n);
            messagePrint("Browser request: \n" + browserRequest);
            /** Extract the target host and port if available **/
            Matcher mHost = patternHost.matcher(browserRequest);
            if (mHost.find()) {
                logger.debug("Matched groups: " + mHost.groupCount());
                targetHost = mHost.group(1);
                if (!mHost.group(2).equals(""))
                    targetPort = Integer.parseInt(mHost.group(2));
                else {
                    if (targetHost.startsWith("ssl.")) {
                        targetPort = 443;
                    } else {
                        targetPort = 80;
                    }
                }
                if (targetHost.startsWith("ssl.")) {
                    targetHost = targetHost.replaceFirst("ssl.", "");
                    ssl = true;
                } else {
                    ssl = false;
                }
                logger.debug("Target host: [" + targetHost + "]");
                logger.debug("Target port: [" + targetPort + "]");
            } else {
                messagePrint("No host description found in the request, aborting thread!");
                //TODO ssl or not, is it a CONNECT command? This proxy should only be registered for the http protocol if possible as this case is not yet handled, take a look at http://www.javaworld.com/javatips/jw-javatip111.html
                return;
            }
            messagePrint("Connecting socket to host " + targetHost + " on port " + targetPort);

            /** connect to the target host **/
            if (debugSSL) {
                System.setProperty("javax.net.debug", "all");
            }
            if (ssl) {
                /** Set system variables according to config **/
                if (trustStoreFile != null)
                    System.setProperty("javax.net.ssl.trustStore", trustStoreFile.getAbsolutePath());
                if (trustStorePassword != null)
                    System.setProperty("javax.net.ssl.trustStorePassword", trustStorePassword);
                if (keyStoreFile != null)
                    System.setProperty("javax.net.ssl.keyStore", keyStoreFile.getAbsolutePath());
                if (keyStorePassword != null)
                    System.setProperty("javax.net.ssl.keyStorePassword", keyStorePassword);
                if (keyStoreType != null)
                    System.setProperty("javax.net.ssl.keyStoreType", keyStoreType);
                
                /** create KeyStore **/
                KeyStore keyStore;
                char[] keyStorePasswd = null;
                if(keyStoreFile == null) {
                    /** Decide if file based certificate or entrust/windows system store is used (not implemented openly) **/
                    throw new Exception("Client certificate file not set, aborting thread");
                } else {
                    InputStream keyInput = new FileInputStream(getKeyStoreFile());
                    keyStore = KeyStore.getInstance(getKeyStoreType());
                    keyStorePasswd = getKeyStorePassword().toCharArray();
                    keyStore.load(keyInput, keyStorePasswd);
                    keyInput.close();
                }
                
                /** Create keymanager **/
                KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
                kmf.init(keyStore, keyStorePasswd);
                KeyManager[] keyManagers = kmf.getKeyManagers();

                /** create trust manager **/
                TrustManager[] trustManagers;
                if (trustStoreFile == null) {
                    /** set hostname verifyer if no truststorefile is set **/
                    trustManagers = trustAllCerts;
                } else {
                    TrustManagerFactory tmf = TrustManagerFactory.getInstance(("SunX509"));
                    tmf.init(keyStore);
                    trustManagers = tmf.getTrustManagers();
                }
                /** initiate the ssl connection **/
                SSLContext sc = SSLContext.getInstance("TLS");
                sc.init(keyManagers, trustManagers, new java.security.SecureRandom());
                SocketFactory socketFactory = sc.getSocketFactory();
                hostSocket = socketFactory.createSocket(targetHost, targetPort);

            } else {
                /** initiate standard http connection **/
                logger.debug(targetHost + ":" + targetPort);
                hostSocket = new Socket(targetHost, targetPort);
            }

            /** Pipe the request to the target host **/
            OutputStream sos = hostSocket.getOutputStream();
            messagePrint("Forwarding request to server");
            //sos.write(buffer, 0, n);
            //browserRequest = browserRequest.replace("http://ssl.", "https://");
            //debugInfo("Modified request:\n"+browserRequest);
            sos.write(browserRequest.getBytes());
            sos.flush();

            /** Pipe the response back to the requestor **/
            InputStream sis = hostSocket.getInputStream();

            BufferedInputStream sisb = new BufferedInputStream(sis);
            OutputStream bos = clientSocket.getOutputStream();
            messagePrint("Forwarding response from server");
            BufferedReader buff = new BufferedReader(new InputStreamReader(sis));
            int totaln = 0;
            int contentlenght = -1;
            int contentlenghtreceived = 0;
            String header = "";
            //TODO Extract header, this should be in the first received chunk
            //TODO continue with content, header decides if fixed lenght or chunked stream
            do {
                n = sisb.read(buffer);
                contentlenghtreceived += n;
                logger.debug("Receiving " + n + " bytes");
                    if (n > 0) {
                        //TODO rewrite links
                        String tmps = new String(buffer, 0, n, Charset.defaultCharset());
                        if(tmps.matches("https://(ssl.)?")) {
                            tmps = tmps.replaceAll("https://(ssl.)?", "http://ssl.");
                            bos.write(tmps.getBytes());
                        } else {
                            bos.write(buffer, 0, n);
                        }

                        Matcher m304 = pattern304.matcher(tmps);
                        if(m304.find()) {
                            messagePrint("found 304");
                            n = -1;
                        }
                        Matcher mcl = patternContentLength.matcher(tmps);
                        if (mcl.find()) {
                            logger.debug("content-lenght detected = "+mcl.group(1));
                            contentlenght = Integer.parseInt(mcl.group(1));
                            int headerend = tmps.indexOf("\r\n\r\n");
                            header = tmps.substring(0,headerend);
                            contentlenghtreceived -= header.getBytes().length;
                        } else if(contentlenght != -1) {
                            Matcher mcs = patternChunckSize.matcher(tmps);
                            if(mcs.find()) {
                                for(int i = 1; i<mcs.groupCount();i++) {
                                    logger.debug("chunk-size detected: "+mcs.group(i));
                                    if(Integer.parseInt(mcs.group(i), 16) == 0) {
                                        logger.debug("chunk zero reached");
                                        n = -1;
                                    }
                                }
                            }
                        }
                        if(n == 8) {
                            String hexvalue = stringToHex(tmps);
                            //debugInfo("hex: "+hexvalue);
                            /** Special handling because of a weird error in regex matching **/
                            if(hexvalue.equals("30 30 30 30 8d a 8d a "))
                                n = -1;
                        }
                        if(contentlenght <= contentlenghtreceived && contentlenght != -1) {
                            n = -1;
                            logger.debug("Content-lenght reached");
                        }

                    }
                } while (n > 0);
            bos.flush();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        } finally {
            /** close the socket again **/
            try{
                if(hostSocket != null)
                    hostSocket.close();
                if(clientSocket != null)
                    clientSocket.close();
            } catch(Exception e) {
                e.printStackTrace();
            }
            logger.debug("End of communication thread");
            CAPServer.getInstance().ptStopNotification();
        }
    }

    /**
     * Helper function to debug proxy stuff that shows a string in its hex values
     * @param base Text to be converted
     * @return Hex representation of the String in ASCII to be displayed on screens
     */
    public static String stringToHex(String base)
    {
        StringBuffer buffer = new StringBuffer();
        int intValue;
        for(int x = 0; x < base.length(); x++)
        {
            int cursor = 0;
            intValue = base.charAt(x);
            String binaryChar = new String(Integer.toBinaryString(base.charAt(x)));
            for(int i = 0; i < binaryChar.length(); i++)
            {
                if(binaryChar.charAt(i) == '1')
                {
                    cursor += 1;
                }
            }
            if((cursor % 2) > 0)
            {
                intValue += 128;
            }
            buffer.append(Integer.toHexString(intValue) + " ");
        }
        return buffer.toString();
    }

    private synchronized void messagePrint(String msg) {
        if (!silent)
            logger.info(msg);
        else
            logger.debug(msg);
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
        if(debug) {
            logger.setLevel(Level.DEBUG);
        }
    }

    public boolean isDebugSSL() {
        return debugSSL;
    }

    public void setDebugSSL(boolean debugSSL) {
        this.debugSSL = debugSSL;
    }

    public boolean isSilent() {
        return silent;
    }

    public void setSilent(boolean silent) {
        this.silent = silent;
    }

    public File getTrustStoreFile() {
        return trustStoreFile;
    }

    public void setTrustStoreFile(File trustStoreFile) {
        this.trustStoreFile = trustStoreFile;
    }

    public String getTrustStorePassword() {
        return trustStorePassword;
    }

    public void setTrustStorePassword(String trustStorePassword) {
        this.trustStorePassword = trustStorePassword;
    }

    public File getKeyStoreFile() {
        return keyStoreFile;
    }

    public void setKeyStoreFile(File keyStoreFile) {
        this.keyStoreFile = keyStoreFile;
    }

    public String getKeyStorePassword() {
        return keyStorePassword;
    }

    public void setKeyStorePassword(String keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
    }

    public String getKeyStoreType() {
        return keyStoreType;
    }

    public void setKeyStoreType(String keyStoreType) {
        this.keyStoreType = keyStoreType;
    }

}

