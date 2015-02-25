package ch.racic.caps.utils;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * StreamCopyThread Tester.
 *
 * @author Michel Racic
 * @version 1.0
 * @since <pre>Feb 22, 2015</pre>
 */
public class StreamCopyThreadTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testSimpleString() throws Exception {
        String testString = RandomStringUtils.randomAscii(5000000);
        ByteArrayInputStream in = new ByteArrayInputStream(testString.getBytes("UTF-8"));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        StreamCopyThread sct = new StreamCopyThread(in, out, 40960, false);
        sct.start();
        sct.join();
        in.close();
        out.close();
        String transportedResult = out.toString("UTF-8");
        assertThat("Output String is the same as InputString", testString.equals(transportedResult));
    }

} 
