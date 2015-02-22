/*
 * Copyleft (c) 2015. This code is for learning purposes only.
 * Do whatever you like with it but don't take it as perfect code.
 * //Michel Racic (http://rac.su/+)//
 */

package ch.racic.caps;

import ch.racic.caps.exceptions.NotStartedException;
import org.apache.log4j.Logger;

import javax.naming.ConfigurationException;
import java.io.IOException;
import java.net.ServerSocket;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

/**
 * Created by rac on 08.02.15.
 */
public class CAPServer extends Thread {

    /**
     * Logging helper
     */
    private static final Logger logger = Logger.getLogger(CAPServer.class);
    // TODO get those values from the pom file while deploying
    public static String PROJECT_NAME = "CAPS";
    public static String PROJECT_VERSION = "0.1-SNAPSHOT";
    private static volatile long connectionCounter = 0;
    private final ICapsConfiguration configuration;
    private volatile Boolean running = false;
    private ServerSocket serverSocket;
    private volatile int proxyListenerPort;
    private volatile ThreadGroup connectionHandlerGroup = new ThreadGroup("CAPS connection handlers");


    /**
     * CAPS main thread constructor Initialize the server with a configuration object and use the start() method to
     * start listening. After using it, you can use the shutdown() method to initiate the shutdown of the server which
     * interrupts all connection handler threads and closes the sockets.
     * <p/>
     * <pre>
     *     capsConf = new CapsConfiguration()
     *               .setTargetKeyStorePath("certs/test.p12")
     *               .setTargetKeyStorePassword("testPassword");
     *     CAPServer cs = new CAPServer(capsConf);
     *     cs.start();
     *     ... do whatever tests you want to do ...
     *     // Getting a proxy object to use in selenium
     *     String proxyString = "localhost:" + cs.getProxyListenerPort();
     *     org.openqa.selenium.Proxy proxy = new Proxy().setSslProxy(proxyString).setHttpProxy(proxyString)
     *     DesiredCapabilities caps = DesiredCapabilities.htmlUnit();
     *     caps.setCapability(CapabilityType.ACCEPT_SSL_CERTS, true);
     *     caps.setCapability(CapabilityType.PROXY, proxy);
     *     HtmlUnitDriver d = new HtmlUnitDriver(caps);
     *     ...
     *     cs.shutdown();
     * </pre>
     *
     * @param configuration
     */
    public CAPServer(final ICapsConfiguration configuration) {
        super("CAPServer-" + ++connectionCounter);
        this.configuration = configuration;
    }

    /**
     * Factory to instantiate the proxy, start it and wait until it is listening prior to returning with the CAPS
     * instance.
     *
     * @param conf
     * @return CAPServer instance which is already listening
     * @throws InterruptedException
     * @throws CertificateException
     * @throws UnrecoverableKeyException
     * @throws NoSuchAlgorithmException
     * @throws IOException
     * @throws KeyManagementException
     * @throws KeyStoreException
     * @throws ConfigurationException
     */
    public static synchronized CAPServer bringItUpRunning(CapsConfiguration conf) throws InterruptedException, CertificateException, UnrecoverableKeyException, NoSuchAlgorithmException, IOException, KeyManagementException, KeyStoreException, ConfigurationException {
        final CAPServer cs = new CAPServer(conf);
        cs.startProxyServer();
        cs.start();
        if (cs.getProxyListenerPort() == null)
            throw new NotStartedException("The starting sequence of the proxy did not work, socket is not listening");
        return cs;
    }

    public synchronized void run() {
        try {
            startProxyServer();
            running = true;
            while (running) {
                // handle accepts
                new ConnectionHandler(serverSocket.accept(), configuration, connectionHandlerGroup).start();

            }

        } catch (Exception e) {
            running = false;
            logger.error("Fatal exception in CAPServer", e);
        } finally {
            try {
                stopProxyServer();
            } catch (IOException e) {
                connectionHandlerGroup.uncaughtException(this, e);
            }
        }
    }

    /**
     * Create a server socket to be used for incoming connections from clients.
     *
     * @throws IOException
     * @throws CertificateException
     * @throws UnrecoverableKeyException
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     * @throws KeyManagementException
     * @throws ConfigurationException
     */
    private synchronized void startProxyServer() throws IOException, CertificateException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, ConfigurationException {
        if (serverSocket != null) {
            return;
        }
        // create the server socket based on the configuration
        serverSocket = new ServerSocket(configuration.getProxyListenerPort());
        serverSocket.setReuseAddress(true);
        // Setting the port allocated by the socket, in case the configuration was default on 0, it will use any free port
        proxyListenerPort = serverSocket.getLocalPort();
    }

    /**
     * Send a interrupt to all connection handlers and close the server socket.
     *
     * @throws IOException
     */
    private synchronized void stopProxyServer() throws IOException {
        // Interrupt all Connection threads
        connectionHandlerGroup.interrupt();
        if (!serverSocket.isClosed())
            serverSocket.close();
        serverSocket = null;
        Thread.currentThread().interrupt();
    }

    /**
     * Initiate shutdown of the proxy server. This will send interrupts to all processing threads and closes the
     * sockets.
     */
    public void shutdown() {
        running = false;
    }

    /**
     * @return port or null if proxy is not yet started
     */
    public Integer getProxyListenerPort() {
        if (proxyListenerPort == 0)
            return null;
        else
            return proxyListenerPort;
    }

    /**
     * The proxy string has the form {hostname}:{port} and can be used to create a selenium proxy object.
     * <pre>
     *     String proxyString = cs.getProxyString();
     *     org.openqa.selenium.Proxy proxy = new Proxy().setSslProxy(proxyString).setHttpProxy(proxyString);
     *     DesiredCapabilities caps = DesiredCapabilities.htmlUnit();
     *     caps.setCapability(CapabilityType.ACCEPT_SSL_CERTS, true);
     *     caps.setCapability(CapabilityType.PROXY, proxy);
     * </pre>
     *
     * @return
     */
    public String getProxyString() {
        if (proxyListenerPort == 0)
            return null;
        else
            return serverSocket.getInetAddress().getHostName() + ":" + getProxyListenerPort();
    }

}
