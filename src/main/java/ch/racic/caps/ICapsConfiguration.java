/*
 * Copyleft (c) 2015. This code is for learning purposes only.
 * Do whatever you like with it but don't take it as perfect code.
 * //Michel Racic (http://rac.su/+)//
 */

package ch.racic.caps;

import javax.naming.ConfigurationException;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.Proxy;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

/**
 * Interface that can be used to provide your own implementation of the proxy configuration and the way the SSLContext
 * is created.
 */
public interface ICapsConfiguration {
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
    public SSLContext getTargetSslContext() throws NoSuchAlgorithmException, CertificateException, KeyStoreException,
            IOException, UnrecoverableKeyException, ConfigurationException, KeyManagementException;

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
    public SSLContext getProxySslContext() throws CertificateException, UnrecoverableKeyException,
            NoSuchAlgorithmException, IOException, KeyStoreException, ConfigurationException, KeyManagementException;

    /**
     * Get the proxy which is required for the connection to the target server.
     *
     * @return Proxy object or null
     */
    public Proxy getTargetProxy();

    /**
     * Get the connection timeout set for the sockets.
     *
     * @return timeout in ms
     */
    public long getConnectionTimeout();

    /**
     * Get the current setting of the proxy listener port.
     *
     * @return port number
     */
    public int getProxyListenerPort();

    /**
     * Get the desired thread pool size for the ConnectionHandler threads.
     *
     * @return
     */
    public int getThreadPoolSize();

}
