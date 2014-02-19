package ch.racic.caps;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;

/**
 * @Author Michel Racic (m@rac.su) Date: 12.08.12
 */
public class CAPServer extends Thread {
    /**
     * Logging helper
     */
    private static final Logger logger = Logger.getLogger(CAPServer.class);

    /**
     * Singleton variable *
     */
    private static CAPServer instance;
    private static boolean inShutdown        = false;


    /**
     * Singleton accessor *
     */
    public static CAPServer getInstance() {
        if (instance == null)
            instance = new CAPServer();
        return instance;
    }

    /**
     * Config variables *
     */
    private int     listenerPort = 8081; //Defaults to 8081 if no port is set
    private boolean debug        = false, debugSSL = false;
    private boolean silent = true;

    /**
     * SSL config *
     */
    private File   trustStoreFile     = null;
    private String trustStorePassword = null;
    private File   keyStoreFile       = null;
    private String keyStorePassword   = null;
    private String keyStoreType       = null;

    /**
     * State variables *
     */
    private boolean listening         = false;
    private int     connectionThreads = 0;

    /**
     * Objects *
     */
    private ServerSocket serverSocket = null;

    public CAPServer() {
    }

    public static void main(String[] args) throws IOException {
        CAPServer cs = new CAPServer();
        cs.setDebug(true);
        cs.setSilent(false);
        cs.startProxy(true);
    }

    public synchronized int getListenerPort() {
        return getInstance().listenerPort;
    }

    public synchronized void setListenerPort(int listenerPort) {
        getInstance().listenerPort = listenerPort;
    }

    public synchronized boolean isDebug() {
        return getInstance().debug;
    }

    public synchronized void setDebug(boolean debug) {
        getInstance().debug = debug;
        if(debug) {
            logger.setLevel(Level.DEBUG);
        }
    }

    public synchronized boolean isDebugSSL() {
        return getInstance().debugSSL;
    }

    public synchronized void setDebugSSL(boolean debugSSL) {
        getInstance().debugSSL = debugSSL;
    }

    public synchronized boolean isSilent() {
        return getInstance().silent;
    }

    public synchronized void setSilent(boolean silent) {
        getInstance().silent = silent;
    }

    public synchronized File getTrustStoreFile() {
        return getInstance().trustStoreFile;
    }

    public synchronized void setTrustStoreFile(File trustStoreFile) {
        getInstance().trustStoreFile = trustStoreFile;
    }

    public synchronized String getTrustStorePassword() {
        return getInstance().trustStorePassword;
    }

    public synchronized void setTrustStorePassword(String trustStorePassword) {
        getInstance().trustStorePassword = trustStorePassword;
    }

    public synchronized File getKeyStoreFile() {
        return getInstance().keyStoreFile;
    }

    public synchronized void setKeyStoreFile(File keyStoreFile) {
        getInstance().keyStoreFile = keyStoreFile;
    }

    public synchronized String getKeyStorePassword() {
        return getInstance().keyStorePassword;
    }

    public synchronized void setKeyStorePassword(String keyStorePassword) {
        getInstance().keyStorePassword = keyStorePassword;
    }

    public synchronized String getKeyStoreType() {
        return getInstance().keyStoreType;
    }

    public synchronized void setKeyStoreType(String keyStoreType) {
        getInstance().keyStoreType = keyStoreType;
    }

   public synchronized void startProxy(boolean listening) {
        while(inShutdown) {
            try {
                sleep(100);
            } catch (InterruptedException e) {
                logger.warn("Sleep got interupted", e);
            }
        }
        getInstance().start();
        getInstance().listening = listening;
    }

    public synchronized void startProxyListening() {
        stopProxyListening();
        getInstance().listening = true;
    }

    public synchronized void stopProxy() {
        inShutdown = true;
        stopProxyListening();
        while (instance != null) {
            try {
                sleep(1000);
            } catch (InterruptedException e) {
                logger.warn("Sleep got interupted", e);
            }
        }
    }

    public synchronized void stopProxyListening() {
        if (getInstance().listening) {
            getInstance().listening = false;
        }
        boolean forceShutdown = false;
        long waitLimit = 10000;
        long alreadyWaited = 0;
        boolean loopCondition = !forceShutdown?(getInstance().connectionThreads > 0):forceShutdown;
        while (loopCondition) {
            try {
                sleep(100);
                alreadyWaited += 100;
                if (alreadyWaited >= waitLimit)
                    forceShutdown = true;
            } catch (InterruptedException e) {
                logger.warn("Sleep got interupted", e);
            }
            loopCondition = !forceShutdown?(getInstance().connectionThreads > 0):forceShutdown;
        }
        if (getInstance().serverSocket != null) {
            try {
                getInstance().serverSocket.close();
                getInstance().serverSocket = null;

            } catch (IOException e) {
                logger.error("Socket error", e);
            }
        }
    }

    public synchronized void ptStartNotification() {
        getInstance().connectionThreads++;
        if (getInstance().debug) {
            logger.info("OpenConnectionThreads[" + getInstance().connectionThreads + "]");
        }
    }

    public synchronized void ptStopNotification() {
        getInstance().connectionThreads--;
        if (getInstance().debug)
            logger.info("OpenConnectionThreads[" + getInstance().connectionThreads + "]");
    }

    public void run() {
        while (!inShutdown) {
            if (listening) {
                try {
                    try {
                        serverSocket = new ServerSocket(getInstance().listenerPort);
                        serverSocket.setReuseAddress(true);
                        logger.info("ClientCertificate SSL Injecting Proxy Started on port: " + getInstance().listenerPort);
                    } catch (IOException e) {
                        logger.info("Could not listen on port: " + getInstance().listenerPort);
                        return;
                    }

                    while (getInstance().listening) {
                        ProxyThread p = new ProxyThread(serverSocket.accept());
                        p.setDebug(getInstance().isDebug());
                        p.setDebugSSL(getInstance().isDebugSSL());
                        p.setSilent(getInstance().isSilent());
                        p.setTrustStoreFile(getInstance().getTrustStoreFile());
                        p.setTrustStorePassword(getInstance().getTrustStorePassword());
                        p.setKeyStoreFile(getInstance().getKeyStoreFile());
                        p.setKeyStorePassword(getInstance().getKeyStorePassword());
                        p.setKeyStoreType((getInstance().getKeyStoreType() != null) ? getInstance().getKeyStoreType() : "pkcs12");
                        p.start();
                    }

                } catch (IOException e) {
                    logger.error("IOException in socket", e);
                } finally {
                    if (getInstance().serverSocket != null) {
                        try {
                            getInstance().serverSocket.close();
                            getInstance().serverSocket = null;
                        } catch (IOException e) {
                            e.printStackTrace();// IGNORE
                        }
                    }
                }
            }
            try {
                sleep(100);
            } catch (InterruptedException e) {
                logger.warn("Sleep got interupted", e);
            }
        }
        instance = null;
        inShutdown = false;
    }
}
