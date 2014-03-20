/*
 * This is free and unencumbered software released into the public domain.
 *
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 *
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * For more information, please refer to <http://unlicense.org/>
 */

package net.adamcin.granite.client.packman;

import net.adamcin.commons.testing.junit.FailUtil;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.adamcin.granite.client.packman.WspFilter.*;
import static org.junit.Assert.*;

/**
 * Created by madamcin on 3/18/14.
 */
public class WspFilterTest {

    public static final Logger LOGGER = LoggerFactory.getLogger(WspFilterTest.class);

    @Test
    public void testEquals() {
        WspFilter exampleFilter = new WspFilter(
                new Root("/etc",
                        new Rule(false, "/etc/packages(/.*)?"),
                        new Rule(false, "/etc/map(/.*)?")
                ));

        WspFilter exampleFilterCopy = new WspFilter(
                new Root("/etc",
                        new Rule(false, "/etc/packages(/.*)?"),
                        new Rule(false, "/etc/map(/.*)?")
                ));

        assertEquals("two WspFilter instances should be deep-equal if all variables and ordering are the same", exampleFilter, exampleFilterCopy);

        WspFilter exampleFilterReorder = new WspFilter(
                new Root("/etc",
                        new Rule(false, "/etc/map(/.*)?"),
                        new Rule(false, "/etc/packages(/.*)?")
                ));

        assertFalse("two WspFilter instances should NOT be deep-equal even if only the rule order has changed", exampleFilter.equals(exampleFilterReorder));

        WspFilter exampleFilterOmission = new WspFilter(
                new Root("/etc",
                        new Rule(false, "/etc/packages(/.*)?")
                ));

        assertFalse("two WspFilter instances should NOT be deep-equal even if a rule has been omitted", exampleFilter.equals(exampleFilterOmission));

        WspFilter exampleFilterAddition = new WspFilter(
                new Root("/etc",
                        new Rule(false, "/etc/packages(/.*)?"),
                        new Rule(false, "/etc/map(/.*)?")
                ),
                new Root("/content",
                        new Rule(false, "/content/dam(/.*)?")
                ));

        assertFalse("two WspFilter instances should NOT be deep-equal even if a root has been added", exampleFilter.equals(exampleFilterAddition));
    }

    @Test
    public void testParseSimpleSpec() {
        assertNull("A null text value should return null", WspFilter.parseSimpleSpec(null));

        assertEquals("An empty text value should return an empty WspFilter", new WspFilter(), WspFilter.parseSimpleSpec(""));

        boolean exThrown = false;
        try {
            WspFilter.parseSimpleSpec("invalid");
        } catch (RuntimeException ex) {
            exThrown = true;
        }

        assertTrue("An invalid text value should throw a RuntimeException of some kind.", exThrown);

        exThrown = false;
        try {
            WspFilter.parseSimpleSpec("# comment");
            WspFilter.parseSimpleSpec(" # comment");
            WspFilter.parseSimpleSpec("/");
            WspFilter.parseSimpleSpec("/#comment");
            WspFilter.parseSimpleSpec("/#comment\n+/etc/packages(/.*)?\n-/etc/packages/jcr:content(/.*)?");
        } catch (RuntimeException e) {
            LOGGER.info("*** parse exception ***: {}", FailUtil.sprintShortStack(e));
            exThrown = true;
        }

        assertFalse("A valid text value should NOT throw a RuntimeException of some kind.", exThrown);

        WspFilter exampleFilter = new WspFilter(
                new Root("/etc",
                        new Rule(false, "/etc/packages(/.*)?"),
                        new Rule(false, "/etc/map(/.*)?")
                ),
                new Root("/content",
                        new Rule(true, "/content/usergenerated(/.*)?")
                ));

        StringBuilder specBuilder = new StringBuilder();
        specBuilder.append("/etc # root for /etc").append("\r\n");
        specBuilder.append("# exclude packages.").append("\r\n");
        specBuilder.append("-/etc/packages(/.*)?").append("\r\n");
        specBuilder.append("-/etc/map(/.*)? # exclude map entries.").append("\r\n");
        specBuilder.append("  /content  ").append("\r\n");
        specBuilder.append("        +/content/usergenerated(/.*)?  ").append("\r\n");

        assertEquals("a WspFilter from spec should match one constructed as pojos",
                exampleFilter, WspFilter.parseSimpleSpec(specBuilder.toString()));
    }

}
