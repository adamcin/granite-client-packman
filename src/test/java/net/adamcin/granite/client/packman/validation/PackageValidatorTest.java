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

package net.adamcin.granite.client.packman.validation;

import net.adamcin.commons.testing.junit.FailUtil;
import net.adamcin.granite.client.packman.WspFilter;
import org.apache.jackrabbit.vault.fs.api.WorkspaceFilter;
import org.apache.jackrabbit.vault.fs.config.DefaultWorkspaceFilter;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;

/**
 * Created by madamcin on 3/14/14.
 */
public class PackageValidatorTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(PackageValidatorTest.class);

    @Test
    public void testCheckFilter() {
        WspFilter filter = loadFromResource("testCheckFilter/check1.filter.xml");
        ValidationOptions options =
                new DefaultValidationOptions(filter, false);
        assertEquals("a filter validated against itself should always return VALID",
                ValidationResult.VALID,
                PackageValidator.checkFilter(options, filter));

        WspFilter valid = loadFromResource("testCheckFilter/check1.valid.xml");
        assertEquals("a matching filter set must contain all rules from the covering validation filter set",
                ValidationResult.VALID,
                PackageValidator.checkFilter(options, valid));

        WspFilter omission = loadFromResource("testCheckFilter/check1.omission.xml");
        assertEquals("a matching filter set must contain all rules from the covering validation filter set",
                ValidationResult.Reason.ROOT_MISSING_RULES,
                PackageValidator.checkFilter(options, omission).getReason());

        WspFilter wrongOrder = loadFromResource("testCheckFilter/check1.wrongOrder.xml");
        assertEquals("a matching filter set must contain all validation rules in the correct order, after all other rules",
                ValidationResult.Reason.ROOT_MISSING_RULES,
                PackageValidator.checkFilter(options, wrongOrder).getReason());
    }

    private WspFilter loadFromResource(String path) {
        return WspFilter.adaptWorkspaceFilter(loadFullFromResource(path));
    }

    private WorkspaceFilter loadFullFromResource(String path) {
        DefaultWorkspaceFilter filter = new DefaultWorkspaceFilter();
        try {
            filter.load(getClass().getClassLoader().getResourceAsStream(path));
        } catch (Exception e) {
            FailUtil.sprintFail(e);
        }
        return filter;
    }

}
