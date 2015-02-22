package ch.racic.caps.utils;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToIgnoringWhiteSpace;

/**
 * StringUtils Tester.
 *
 * @author Michel Racic
 * @version 1.0
 * @since <pre>Feb 22, 2015</pre>
 */
public class StringUtilsTest {

    /**
     * Method: replaceLast(final String text, final String regex, final String replacement)
     */
    @Test
    public void testReplaceLastSimpleStringSingle() throws Exception {
        String testString = "ch.racic.caps.resources/certs/server.p12";
        String testRegex = "p12";
        String testReplacement = "pwd";
        String expectedString = "ch.racic.caps.resources/certs/server.pwd";
        String modifiedString = StringUtils.replaceLast(testString, testRegex, testReplacement);
        assertThat(modifiedString, equalToIgnoringWhiteSpace(expectedString));
    }

    /**
     * Method: replaceLast(final String text, final String regex, final String replacement)
     */
    @Test
    public void testReplaceLastSimpleStringMultiple() throws Exception {
        String testString = "ch.racic.caps.resources.p12/certs/p12/server.p12";
        String testRegex = "p12";
        String testReplacement = "pwd";
        String expectedString = "ch.racic.caps.resources.p12/certs/p12/server.pwd";
        String modifiedString = StringUtils.replaceLast(testString, testRegex, testReplacement);
        assertThat(modifiedString, equalToIgnoringWhiteSpace(expectedString));
    }

} 
