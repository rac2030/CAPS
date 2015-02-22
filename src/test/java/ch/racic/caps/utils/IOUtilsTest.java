package ch.racic.caps.utils;

import org.junit.Assert;
import org.junit.Test;

import java.io.InputStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToIgnoringWhiteSpace;

/**
 * IOUtils Tester.
 *
 * @author Michel Racic
 * @version 1.0
 * @since <pre>Feb 22, 2015</pre>
 */
public class IOUtilsTest {

    /**
     * Method: toString(InputStream in, String encoding)
     */
    @Test
    public void testToStringForInEncoding() throws Exception {
        //TODO: Test goes here...
        Assert.fail();
    }

    /**
     * Method: toString(InputStream in)
     */
    @Test
    public void testToStringIn() throws Exception {
        InputStream testInputStream = getClass().getClassLoader().getResourceAsStream("certs/client.p12.pwd");
        String expectedString = "test";
        String result = IOUtils.toString(testInputStream);
        assertThat(result, equalToIgnoringWhiteSpace(expectedString));
    }

    /**
     * Method: resourceAsString(String path, String encoding)
     */
    @Test
    public void testResourceAsStringForPathEncoding() throws Exception {
        //TODO: Test goes here...
        Assert.fail();
    }

    /**
     * Method: resourceAsString(String path)
     */
    @Test
    public void testResourceAsStringPath() throws Exception {
        String expectedString = "test";
        String result = IOUtils.resourceAsString("certs/client.p12.pwd");
        assertThat(result, equalToIgnoringWhiteSpace(expectedString));
    }


} 
