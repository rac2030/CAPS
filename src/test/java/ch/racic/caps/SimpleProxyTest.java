package ch.racic.caps;

import ch.racic.utils.Request;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;

import static org.junit.Assert.assertTrue;

/**
 * @Author Michel Racic (m@rac.su) Date: 14.08.12
 */
public class SimpleProxyTest {

    // Proxy test object
    private static CAPServer cs;
    private static int proxyPort = 8081;
    private static Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("localhost", proxyPort));

    /**
     * Initialize the test by enabling console output to the logger and setting the log level to display some
     * informations. These changes to the logger will be reseted.
     */
    @BeforeClass
    public static void init() {
        cs = new CAPServer();
        cs.setDebug(true);
        cs.setSilent(false);

        cs.setListenerPort(proxyPort);
        cs.startProxy(true);
    }

    /**
     * Resets the changes to the logger after this test has run.
     */
    @AfterClass
    public static void shutdown() {
        cs.stopProxy();
    }

    /**
     * Tests if a simple http call over the proxy returns any value
     *
     * @throws Exception
     */
    @Test
    public void httpTest() throws Exception {
        String test = Request.readUrl(new URL("http://rac.su"), proxy);
        assertTrue("Return value has some content", !test.equals(""));
    }


}
