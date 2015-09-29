/*
 * Copyleft (c) 2015. This code is for learning purposes only.
 * Do whatever you like with it but don't take it as perfect code.
 * //Michel Racic (http://rac.su/+)//
 */

package ch.racic.caps;

import ch.racic.caps.utils.FileUtils;
import ch.racic.caps.utils.IOUtils;
import com.google.common.io.Files;
import org.apache.log4j.Logger;

import javax.naming.ConfigurationException;
import javax.net.ssl.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Proxy;
import java.nio.charset.Charset;
import java.security.*;
import java.security.cert.CertificateException;

/**
 * Created by rac on 08.02.15.
 */
public class CapsConfiguration implements ICapsConfiguration {

    /**
     * SSL statics *
     */
    private final static TrustManager[] trustAllCerts = new TrustManager[]{
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
    private static final String DEFAULT_STORE_TYPE = "pkcs12";
    private static Logger logger = Logger.getLogger(CapsConfiguration.class);
    /**
     * Variables used for target socket in CAPS *
     */
    private volatile SSLContext targetSslContext;
    private String targetTrustStorePath;
    private String targetTrustStorePassword = "";
    private String targetTrustStoreType;
    private volatile TrustManager[] targetTrustManager;
    private String targetKeyStorePath;
    private String targetKeyStorePassword = "";
    private String targetKeyStoreType;
    private volatile KeyManager[] targetKeyManager;
    private Proxy targetProxy;
    private long connectionTimeout;
    /**
     * Variables used for caps listener socket *
     */
    private String proxyKeyStorePath;
    private String proxyKeyStorePassword;
    private String proxyKeyStoreType;
    private KeyManager[] proxyKeyManager;
    private SSLContext proxySslContext;
    private int proxyListenerPort;

    private int threadPoolSize;

    /**
     * Constructor for the reference implementation of the configuration object. To use a custom configuration object,
     * you can extend this class and override the public methods or implement ch.racic.caps.ICapsConfiguration.
     * <p/>
     * This constructor will set default values in case they don't get set: targetTrustStoreType = "pkcs12";
     * targetKeyStoreType = "pkcs12"; proxyKeyStorePath = "ch.racic.caps.resources/certs/server.p12"; self signed
     * certificate inside this artifact proxyKeyStoreType = "pkcs12"; proxyListenerPort = 0; if port 0 is used, the
     * proxy server takes any free port @see getProxyListenerPort() connectionTimeout = 5000;
     */
    public CapsConfiguration() {
        ClassLoader cl = getClass().getClassLoader();
        // Setting default values
        targetTrustStoreType = DEFAULT_STORE_TYPE;
        targetKeyStoreType = DEFAULT_STORE_TYPE;
        proxyKeyStorePath = "ch.racic.caps.resources/certs/server.p12";
        try {
            proxyKeyStorePassword = IOUtils.resourceAsString(proxyKeyStorePath + ".pwd");
        } catch (IOException e) {
            logger.warn("Could not read default server certificate password, you will have to set your own certificate with corresponding password", e);
        }
        proxyKeyStoreType = DEFAULT_STORE_TYPE;
        proxyListenerPort = 0; // Use a random free port
        connectionTimeout = 5000;
        threadPoolSize = 30; // Limit the pool size to 30 by default, projects can use a bigger pool if they know the limits
    }

    /**
     * Used by CAPS to retrieve the SSLContext for the target connection (proxy => target)
     *
     * @return SSLContext to create target socket
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     * @throws KeyStoreException
     * @throws IOException
     * @throws UnrecoverableKeyException
     * @throws ConfigurationException
     * @throws KeyManagementException
     */
    public synchronized SSLContext getTargetSslContext() throws NoSuchAlgorithmException, CertificateException, KeyStoreException, IOException, UnrecoverableKeyException, ConfigurationException, KeyManagementException {
        if (targetSslContext != null) {
            return targetSslContext;
        } else {
            targetSslContext = SSLContext.getInstance("TLS");
            targetSslContext.init(getTargetKeyManager(), getTargetTrustManager(), new java.security.SecureRandom("CAPS".getBytes()));
            return targetSslContext;
        }
    }

    /**
     * Set the target SSLContext directly, no need to set target TrustStore or KeyStore after this.
     *
     * @param targetSslContext
     * @return itself for the dot-notation
     */
    public CapsConfiguration setTargetSslContext(SSLContext targetSslContext) {
        this.targetSslContext = targetSslContext;
        return this;
    }

    /**
     * Set the path to the desired TrustStore file. Don't forget to set the target TrustStore type if it's not PKCS12.
     * The file will be taken as stream from the current class loader. It will try to find a the password in default
     * files but only log to debug if this fails. First it will load {targetTrustStorePath}.pwd including the extension
     * (normal append). If first fails, it will try to load a file with .pwd extension instead of the current
     * extension.
     *
     * @param targetTrustStorePath
     * @return itself for the dot-notation
     */
    public CapsConfiguration setTargetTrustStorePath(String targetTrustStorePath) {
        this.targetTrustStorePath = targetTrustStorePath;
        // Try if there is a file with the same name that ends with .pwd
        try {
            if ((new File(targetTrustStorePath + ".pwd")).exists()) {
                // read password from file on file system
                Files.toString(new File(targetTrustStorePath + ".pwd"), Charset.defaultCharset());
            } else {
                this.targetTrustStorePassword = IOUtils.resourceAsString(targetTrustStorePath + ".pwd");
            }
        } catch (IOException ignoredException) {
            // Ignore this exception, user will need to set password or password file on his own
            logger.debug("Try to get password from default location, failed to load the file " + targetTrustStorePath + ".pwd", ignoredException);
            // Try it without the original extension
            final String alternatePwdPath = FileUtils.replaceExtension(targetTrustStorePath, "pwd");
            try {
                if ((new File(alternatePwdPath)).exists()) {
                    // read password from file on file system
                    Files.toString(new File(alternatePwdPath), Charset.defaultCharset());
                } else {
                    this.targetTrustStorePassword = IOUtils.resourceAsString(alternatePwdPath);
                }
            } catch (IOException ignoredException2) {
                // Ignore this exception, user will need to set password or password file on his own
                logger.debug("Try to get password from second default location, failed to load the file " + alternatePwdPath, ignoredException2);
            }
        }
        return this;
    }

    /**
     * Password needed to unlock the TrustStore.
     *
     * @param targetTrustStorePassword
     * @return itself for the dot-notation
     */
    public CapsConfiguration setTargetTrustStorePassword(String targetTrustStorePassword) {
        this.targetTrustStorePassword = targetTrustStorePassword;
        return this;
    }

    /**
     * The type of the target TrustStore. Defaults to PKCS12 but can take anything that the current runtime system
     * supports.
     *
     * @param targetTrustStoreType
     * @return itself for the dot-notation
     */
    public CapsConfiguration setTargetTrustStoreType(String targetTrustStoreType) {
        this.targetTrustStoreType = targetTrustStoreType;
        return this;
    }

    /**
     * Initialization of the TrustManager used to get the target SSLContext.
     *
     * @return TrustManager based on the files set
     * @throws NoSuchAlgorithmException
     * @throws IOException
     * @throws KeyStoreException
     * @throws CertificateException
     */
    private synchronized TrustManager[] getTargetTrustManager() throws NoSuchAlgorithmException, IOException, KeyStoreException, CertificateException {
        if (targetTrustStorePath == null) {
            // set insecure trustAll verifier if no trustStorePath is set
            return trustAllCerts;
        } else if (targetTrustManager != null) {
            // use the cached instance
            return targetTrustManager;
        } else {
            InputStream keyInput = null;
            if ((new File(targetTrustStorePath)).exists()) {
                new FileInputStream(new File(targetTrustStorePath));
            } else {
                keyInput = getClass().getClassLoader().getResourceAsStream(targetTrustStorePath);
            }
            KeyStore keyStore = KeyStore.getInstance(targetTrustStoreType);
            keyStore.load(keyInput, targetTrustStorePassword.toCharArray());
            keyInput.close();
            TrustManagerFactory tmf;
            try {
                tmf = TrustManagerFactory.getInstance("SunX509");
            } catch (NoSuchAlgorithmException e) {
                tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            }
            tmf.init(keyStore);
            targetTrustManager = tmf.getTrustManagers();
            return targetTrustManager;
        }
    }

    /**
     * Set the path to the desired KeyStore file. Don't forget to set the target KeyStore type if it's not PKCS12. The
     * file will be taken as stream from the current class loader.
     *
     * @param targetKeyStorePath
     * @return itself for the dot-notation
     */
    public CapsConfiguration setTargetKeyStorePath(String targetKeyStorePath) {
        this.targetKeyStorePath = targetKeyStorePath;
        // Try if there is a file with the same name that ends with .pwd
        try {
            if ((new File(targetKeyStorePassword + ".pwd")).exists()) {
                Files.toString(new File(targetKeyStorePassword + ".pwd"), Charset.defaultCharset());
            } else {
                this.targetKeyStorePassword = IOUtils.resourceAsString(targetKeyStorePassword + ".pwd");
            }
        } catch (IOException ignoredException) {
            // Ignore this exception, user will need to set password or password file on his own
            logger.debug("Try to get password from default location, failed to load the file " + targetKeyStorePath + ".pwd", ignoredException);
            // Try it without the original extension
            final String alternatePwdPath = FileUtils.replaceExtension(targetKeyStorePath, "pwd");
            try {
                if ((new File(alternatePwdPath)).exists()) {
                    Files.toString(new File(alternatePwdPath), Charset.defaultCharset());
                } else {
                    this.targetKeyStorePassword = IOUtils.resourceAsString(alternatePwdPath);
                }
            } catch (IOException ignoredException2) {
                // Ignore this exception, user will need to set password or password file on his own
                logger.debug("Try to get password from second default location, failed to load the file " + alternatePwdPath, ignoredException2);
            }
        }
        return this;
    }

    /**
     * Password needed to unlock the KeyStore
     *
     * @param targetKeyStorePassword
     * @return itself for the dot-notation
     */
    public CapsConfiguration setTargetKeyStorePassword(String targetKeyStorePassword) {
        this.targetKeyStorePassword = targetKeyStorePassword;
        return this;
    }

    /**
     * The type of the target KeyStore. Defaults to PKCS12 but can take anything that the current runtime system
     * supports.
     *
     * @param targetKeyStoreType
     * @return itself for the dot-notation
     */
    public CapsConfiguration setTargetKeyStoreType(String targetKeyStoreType) {
        this.targetKeyStoreType = targetKeyStoreType;
        return this;
    }

    /**
     * Initializes the target KeyManager with the set options.
     *
     * @return KeyManagers based on the options set
     * @throws ConfigurationException
     * @throws IOException
     * @throws KeyStoreException
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws UnrecoverableKeyException
     */
    private synchronized KeyManager[] getTargetKeyManager() throws ConfigurationException, IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException {
        if (targetKeyStorePath == null) {
            throw new ConfigurationException("No target KeyStore path has been set");
        } else if (targetKeyManager != null) {
            return targetKeyManager;
        } else {
            InputStream keyInput = null;
            if ((new File(targetKeyStorePath)).exists()) {
                new FileInputStream(new File(targetKeyStorePath));
            } else {
                keyInput = getClass().getClassLoader().getResourceAsStream(targetKeyStorePath);
            }
            KeyStore keyStore = KeyStore.getInstance(targetKeyStoreType);
            keyStore.load(keyInput, targetKeyStorePassword.toCharArray());
            keyInput.close();
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509"); //TODO better use getDefault?
            kmf.init(keyStore, targetKeyStorePassword.toCharArray());
            targetKeyManager = kmf.getKeyManagers();
            return targetKeyManager;
        }
    }

    /**
     * Get the proxy which is required for the connection to the target server.
     *
     * @return Proxy object or null
     */
    public Proxy getTargetProxy() {
        return targetProxy;
    }

    /**
     * Set the proxy which is required for the connection to the target server.
     *
     * @param targetProxy
     * @return itself for the dot-notation
     */
    public CapsConfiguration setTargetProxy(Proxy targetProxy) {
        this.targetProxy = targetProxy;
        return this;
    }

    /**
     * Set the server certificate which the proxy uses for the handshake with the source browser. If not set, it will
     * default to a self signed certificate which has been used in the unit test of CAPS.
     *
     * @param proxyKeyStorePath
     */
    public CapsConfiguration setProxyKeyStorePath(String proxyKeyStorePath) {
        this.proxyKeyStorePath = proxyKeyStorePath;
        return this;
    }

    /**
     * Password needed to unlock the proxy KeyStore
     *
     * @param proxyKeyStorePassword
     * @return itself for the dot-notation
     */
    public CapsConfiguration setProxyKeyStorePassword(String proxyKeyStorePassword) {
        this.proxyKeyStorePassword = proxyKeyStorePassword;
        return this;
    }

    /**
     * The type of the proxy KeyStore. Defaults to PKCS12 but can take anything that the current runtime system
     * supports.
     *
     * @param proxyKeyStoreType
     * @return itself for the dot-notation
     */
    public CapsConfiguration setProxyKeyStoreType(String proxyKeyStoreType) {
        this.proxyKeyStoreType = proxyKeyStoreType;
        return this;
    }

    /**
     * Used by CAPS to retrieve the SSLContext for the client connection (proxy => client). This is needed for the MITM
     * interception to inject the client certificate.
     *
     * @return SSLContext to create the proxy socket for the SSL Upgrade of the connection
     * @throws CertificateException
     * @throws UnrecoverableKeyException
     * @throws NoSuchAlgorithmException
     * @throws IOException
     * @throws KeyStoreException
     * @throws ConfigurationException
     * @throws KeyManagementException
     */
    public SSLContext getProxySslContext() throws CertificateException, UnrecoverableKeyException, NoSuchAlgorithmException, IOException, KeyStoreException, ConfigurationException, KeyManagementException {
        if (proxySslContext != null) {
            return proxySslContext;
        } else {
            proxySslContext = SSLContext.getInstance("TLS");
            proxySslContext.init(getProxyKeyManager(), null, new java.security.SecureRandom());
            return proxySslContext;
        }
    }

    /**
     * Set the proxy SSLContext directly, no need to set proxy KeyStore after this.
     *
     * @param proxySslContext
     * @return itself for the dot-notation
     */
    public CapsConfiguration setProxySslContext(SSLContext proxySslContext) {
        this.proxySslContext = proxySslContext;
        return this;
    }

    /**
     * Initialization of the KeyManager used to get the proxy SSLContext.
     *
     * @return KeyManager based on the options set
     * @throws ConfigurationException
     * @throws IOException
     * @throws KeyStoreException
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws UnrecoverableKeyException
     */
    private KeyManager[] getProxyKeyManager() throws ConfigurationException, IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException {
        if (proxyKeyStorePath == null) {
            throw new ConfigurationException("No proxy KeyStore path has been set");
        } else if (proxyKeyManager != null) {
            return proxyKeyManager;
        } else {
            InputStream keyInput = getClass().getClassLoader().getResourceAsStream(proxyKeyStorePath);
            KeyStore keyStore = KeyStore.getInstance(proxyKeyStoreType);
            keyStore.load(keyInput, proxyKeyStorePassword.toCharArray());
            keyInput.close();
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509"); //TODO better use getDefault?
            kmf.init(keyStore, proxyKeyStorePassword.toCharArray());
            proxyKeyManager = kmf.getKeyManagers();
            return proxyKeyManager;
        }
    }

    /**
     * Get the current setting of the proxy listener port.
     *
     * @return port number
     */
    public int getProxyListenerPort() {
        return proxyListenerPort;
    }

    /**
     * Set the proxy listener port to be used. Default value is 0 which means it will use any free port and you can
     * retrieve it from the proxy after its started.
     *
     * @param proxyListenerPort
     * @return
     */
    public CapsConfiguration setProxyListenerPort(int proxyListenerPort) {
        this.proxyListenerPort = proxyListenerPort;
        return this;
    }

    @Override
    public int getThreadPoolSize() {
        return threadPoolSize;
    }

    /**
     * Set the thread pool size to limit how many concurrent open connections can be handled, remaining will be queued.
     *
     * @param threadPoolSize
     */
    public void setThreadPoolSize(int threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
    }

    /**
     * Get the connection timeout set for the sockets.
     *
     * @return timeout in ms
     */
    public long getConnectionTimeout() {
        return connectionTimeout;
    }

    /**
     * Set the connection timeout used in the sockets.
     *
     * @param connectionTimeout
     * @return
     */
    public CapsConfiguration setConnectionTimeout(long connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
        return this;
    }
}
