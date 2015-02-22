package ch.racic.caps.utils;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToIgnoringWhiteSpace;

/**
 * FileUtils Tester.
 *
 * @author Michel Racic
 * @version 1.0
 * @since <pre>Feb 22, 2015</pre>
 */
public class FileUtilsTest {

    /**
     * Method: getFileExtension(final String fullPath)
     */
    @Test
    public void testGetFileExtension() throws Exception {
        String testPath = "ch.racic.caps.resources/certs/server.p12";
        String expectedExtension = "p12";
        String result = FileUtils.getFileExtension(testPath);
        assertThat(result, equalToIgnoringWhiteSpace(expectedExtension));
    }

    /**
     * Method: getFileExtension(final String fullPath)
     */
    @Test
    public void testGetFileExtensionMultiple() throws Exception {
        String testPath = "ch.racic.caps.resources/certs/server.xy.p12";
        String expectedExtension = "p12";
        String result = FileUtils.getFileExtension(testPath);
        assertThat(result, equalToIgnoringWhiteSpace(expectedExtension));
    }

    /**
     * Method: replaceExtension(final String fullPath, final String newExtension)
     */
    @Test
    public void testReplaceExtension() throws Exception {
        String testPath = "ch.racic.caps.resources/certs/server.p12";
        String testReplacement = "pwd";
        String expectedString = "ch.racic.caps.resources/certs/server.pwd";
        String modifiedString = FileUtils.replaceExtension(testPath, testReplacement);
        assertThat(modifiedString, equalToIgnoringWhiteSpace(expectedString));
    }

    /**
     * Method: replaceExtension(final String fullPath, final String newExtension)
     */
    @Test
    public void testReplaceExtensionMultiple() throws Exception {
        String testPath = "ch.racic.caps.resources/certs/server.p13.p12";
        String testReplacement = "pwd";
        String expectedString = "ch.racic.caps.resources/certs/server.p13.pwd";
        String modifiedString = FileUtils.replaceExtension(testPath, testReplacement);
        assertThat(modifiedString, equalToIgnoringWhiteSpace(expectedString));
    }

} 
