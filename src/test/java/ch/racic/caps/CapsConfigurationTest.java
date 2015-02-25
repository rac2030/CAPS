package ch.racic.caps;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import javax.naming.ConfigurationException;

/**
 * CapsConfiguration Tester.
 *
 * @author Michel Racic
 * @version 1.0
 * @since <pre>Feb 22, 2015</pre>
 */
public class CapsConfigurationTest {
    private static String CERT_SERVER = "ch.racic.caps.resources/certs/server.p12";
    private static String CERT_SERVER_TYPE = "pkcs12";
    private static String CERT_CLIENT = "certs/client.p12";
    private static String CERT_CLIENT_TYPE = "pkcs12";
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    private CapsConfiguration conf;

    @Before
    public void setUp() throws Exception {
        conf = new CapsConfiguration();
    }

    @After
    public void tearDown() throws Exception {
    }

    /**
     * Method: getTargetSslContext()
     */
    @Test
    public void testGetTargetSslContextNoException() throws Exception {
        conf.setTargetKeyStorePath(CERT_CLIENT).setTargetKeyStoreType(CERT_CLIENT_TYPE);
        conf.getTargetSslContext();
    }

    /**
     * Method: getTargetSslContext()
     */
    @Test
    public void testGetTargetSslContextNoKeyStorePathException() throws Exception {
        thrown.expect(ConfigurationException.class);
        thrown.expectMessage("No target KeyStore path has been set");
        conf.getTargetSslContext();
    }


    /**
     * Method: getProxySslContext()
     */
    @Test
    public void testGetProxySslContextDefaultNoException() throws Exception {
        conf.getProxySslContext();
    }


} 
