/*
 * Copyleft (c) 2015. This code is for learning purposes only.
 * Do whatever you like with it but don't take it as perfect code.
 * //Michel Racic (http://rac.su/+)//
 */

package ch.racic.caps;

import ch.racic.caps.utils.IOUtils;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;

import javax.naming.ConfigurationException;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import static org.junit.Assert.assertTrue;

/**
 * @Author Michel Racic (m@rac.su)
 */
public class ProxySeleniumIntegrationTest {

    private static int PORT_TESTSERVER_HTTP, PORT_TESTSERVER_SSL;
    private static String proxyString;
    private static CAPServer cs;
    private static Server testServer;

    private static String CERT_SERVER = "ch.racic.caps.resources/certs/server.p12";
    private static String CERT_SERVER_TYPE = "pkcs12";
    private static String CERT_CLIENT = "certs/client.p12";
    private static String CERT_CLIENT_TYPE = "pkcs12";

    private static CapsConfiguration capsConf;

    @BeforeClass
    public static void tearUp() throws Exception {
        testServer = new Server();
        testServer.setStopAtShutdown(true);
        testServer.setStopTimeout(10);

        SslContextFactory sslContextFactory = new SslContextFactory();
        final String serverCertPassword = IOUtils.resourceAsString(CERT_SERVER + ".pwd");
        final String serverCertPath = testServer.getClass().getClassLoader().getResource(CERT_SERVER).toExternalForm();
        sslContextFactory.setKeyStorePath(serverCertPath);
        sslContextFactory.setKeyStorePassword(serverCertPassword);
        sslContextFactory.setKeyStoreType(CERT_SERVER_TYPE);
        sslContextFactory.setKeyManagerPassword(serverCertPassword);
        sslContextFactory.setTrustStorePath(serverCertPath);
        sslContextFactory.setTrustStorePassword(serverCertPassword);
        sslContextFactory.setTrustStoreType(CERT_SERVER_TYPE);
        sslContextFactory.setWantClientAuth(true);

        HttpConfiguration https_config = new HttpConfiguration();
        https_config.addCustomizer(new SecureRequestCustomizer());

        HttpConfiguration http_config = new HttpConfiguration();

        // SSL Connector
        ServerConnector sslConnector = new ServerConnector(testServer,
                new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()),
                new HttpConnectionFactory(https_config));
        sslConnector.setPort(0);

        ServerConnector httpConnector = new ServerConnector(testServer, new HttpConnectionFactory(http_config));
        httpConnector.setPort(0);
        httpConnector.setIdleTimeout(30000);
        testServer.addConnector(httpConnector);
        testServer.addConnector(sslConnector);

        ServletHandler handler = new ServletHandler();
        testServer.setHandler(handler);
        handler.addServletWithMapping(ClientAuthServlet.class, "/*");

        testServer.start();

        PORT_TESTSERVER_HTTP = httpConnector.getLocalPort();
        PORT_TESTSERVER_SSL = sslConnector.getLocalPort();

        initProxy();

    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (testServer != null) {
            testServer.stop();
        }
        cs.shutdown();
    }

    private static void initProxy() throws InterruptedException, CertificateException, UnrecoverableKeyException, NoSuchAlgorithmException, IOException, KeyManagementException, KeyStoreException, ConfigurationException {
        if (cs == null) {
            capsConf = new CapsConfiguration().setTargetKeyStorePath(CERT_CLIENT)
                    .setTargetKeyStoreType(CERT_CLIENT_TYPE);
            cs = CAPServer.bringItUpRunning(capsConf);
            proxyString = cs.getProxyString();
        }
    }

    private static Proxy getCapsProxy() {
        return new Proxy().setSslProxy(proxyString).setHttpProxy(proxyString);
    }

    @Test
    public void withoutProxyHttp() throws Exception {
        DesiredCapabilities caps = DesiredCapabilities.htmlUnit();
        WebDriver d = new HtmlUnitDriver(caps);
        d.navigate().to("http://localhost:" + PORT_TESTSERVER_HTTP);
        assertTrue("Expected that the cert list will be empty", d.getPageSource().startsWith("Cert: 0"));
    }

    @Test
    public void withoutProxySsl() throws Exception {
        DesiredCapabilities caps = DesiredCapabilities.htmlUnit();
        caps.setCapability(CapabilityType.ACCEPT_SSL_CERTS, true);
        WebDriver d = new HtmlUnitDriver(caps);
        d.navigate().to("https://localhost:" + PORT_TESTSERVER_SSL);
        assertTrue("Expected that the cert list will be empty", d.getPageSource().startsWith("Cert: 0"));
    }

    @Test
    public void withProxySsl() throws Exception {
        DesiredCapabilities caps = DesiredCapabilities.htmlUnit();
        caps.setCapability(CapabilityType.ACCEPT_SSL_CERTS, true);
        caps.setCapability(CapabilityType.PROXY, getCapsProxy());
        HtmlUnitDriver d = new HtmlUnitDriver(caps);
        d.navigate().to("https://localhost:" + PORT_TESTSERVER_SSL);
        String response = d.getPageSource();
        assertTrue("Expected that the cert list will have one", response.startsWith("Cert: 1"));
        assertTrue("Expected the name to be the test certificate",
                response.contains("CN=Tester, O=Testing, L=Zurich, ST=ZH, C=CH"));
    }

    @Test
    public void withProxyHttp() throws Exception {
        DesiredCapabilities caps = DesiredCapabilities.htmlUnit();
        caps.setCapability(CapabilityType.PROXY, getCapsProxy());
        HtmlUnitDriver d = new HtmlUnitDriver(caps);
        d.navigate().to("http://localhost:" + PORT_TESTSERVER_HTTP);
        assertTrue("Expected that the cert list will be empty", d.getPageSource().startsWith("Cert: 0"));
    }
}
