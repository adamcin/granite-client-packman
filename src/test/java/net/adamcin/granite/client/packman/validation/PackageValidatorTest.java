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
import net.adamcin.granite.client.packman.ACHandling;
import net.adamcin.granite.client.packman.WspFilter;
import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.vault.fs.api.WorkspaceFilter;
import org.apache.jackrabbit.vault.fs.config.DefaultWorkspaceFilter;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarFile;

import static org.junit.Assert.assertEquals;

/**
 * Created by madamcin on 3/14/14.
 */
public class PackageValidatorTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(PackageValidatorTest.class);

    @Test
    public void testCheckFilter() {
        WspFilter filter = loadWspFilterFromResource("testCheckFilter/check1.filter.xml");
        DefaultValidationOptions options = new DefaultValidationOptions();
        options.setValidationFilter(filter);
        assertEquals("a filter validated against itself should always return success",
                ValidationResult.success(),
                PackageValidator.checkFilter(options, filter));

        WspFilter valid = loadWspFilterFromResource("testCheckFilter/check1.valid.xml");
        assertEquals("a matching filter set must contain all rules from the covering validation filter set",
                ValidationResult.success(),
                PackageValidator.checkFilter(options, valid));

        WspFilter omission = loadWspFilterFromResource("testCheckFilter/check1.omission.xml");
        assertEquals("a matching filter set must contain all rules from the covering validation filter set",
                ValidationResult.Reason.ROOT_MISSING_RULES,
                PackageValidator.checkFilter(options, omission).getReason());

        WspFilter wrongOrder = loadWspFilterFromResource("testCheckFilter/check1.wrongOrder.xml");
        assertEquals("a matching filter set must contain all validation rules in the correct order, after all other rules",
                ValidationResult.Reason.ROOT_MISSING_RULES,
                PackageValidator.checkFilter(options, wrongOrder).getReason());
    }

    @Test
    public void testCheckForbiddenExtensions() {
        try {
            List<String> noJars = Arrays.asList(".jar");
            List<String> noZips = Arrays.asList("zip");

            File noForbidden = new File("target/test-packmgr-client-1.0.zip");
            generatePackageFile("/test-packmgr-client-1.0.zip", noForbidden);

            expectReasonForCheckExtensions(noForbidden, null,
                    ValidationResult.Reason.SUCCESS);

            expectReasonForCheckExtensions(noForbidden, noJars,
                    ValidationResult.Reason.SUCCESS);

            expectReasonForCheckExtensions(noForbidden, noZips,
                    ValidationResult.Reason.SUCCESS);

            File hasJar = new File("target/recap-0.8.0.zip");
            generatePackageFile("/recap-0.8.0.zip", hasJar);

            expectReasonForCheckExtensions(hasJar, null,
                    ValidationResult.Reason.SUCCESS);

            expectReasonForCheckExtensions(hasJar, noJars,
                    ValidationResult.Reason.FORBIDDEN_EXTENSION);

            expectReasonForCheckExtensions(hasJar, noZips,
                    ValidationResult.Reason.SUCCESS);

            File hasZip = new File("target/test-embedded-package-1.0.zip");
            generatePackageFile("/test-embedded-package-1.0.zip", hasZip);

            expectReasonForCheckExtensions(hasZip, null,
                    ValidationResult.Reason.SUCCESS);

            expectReasonForCheckExtensions(hasZip, noJars,
                    ValidationResult.Reason.SUCCESS);

            expectReasonForCheckExtensions(hasZip, noZips,
                    ValidationResult.Reason.FORBIDDEN_EXTENSION);

        } catch (IOException e) {
            FailUtil.sprintFail(e);
        }
    }

    @Test
    public void testPathsDeniedForInclusion() {
        try {
            File hasJar = new File("target/recap-0.8.0.zip");
            generatePackageFile("/recap-0.8.0.zip", hasJar);
            DefaultValidationOptions opts = new DefaultValidationOptions();
            opts.setPathsDeniedForInclusion(Arrays.asList("/libs/recap/install", "/libs/recap/components/addressbook"));
            ValidationResult result = PackageValidator.validate(hasJar, opts);
            assertEquals("Should deny this path", ValidationResult.Reason.DENIED_PATH_INCLUSION,
                    result.getReason());
        } catch (IOException e) {
            FailUtil.sprintFail(e);
        }
    }

    @Test
    public void testForbiddenACHandlingModes() {
        try {
            File recap = new File("target/recap-0.8.0.zip");
            generatePackageFile("/recap-0.8.0.zip", recap);
            DefaultValidationOptions opts = new DefaultValidationOptions();
            opts.setForbiddenACHandlingModes(Arrays.asList(
                    ACHandling.IGNORE, ACHandling.MERGE, ACHandling.MERGE_PRESERVE,
                    ACHandling.OVERWRITE, ACHandling.CLEAR));
            ValidationResult result = PackageValidator.validate(recap, opts);
            assertEquals("Should forbid this handling", ValidationResult.Reason.FORBIDDEN_ACHANDLING,
                    result.getReason());
            assertEquals("Should forbid this handling", ACHandling.IGNORE,
                    result.getForbiddenACHandlingMode());
        } catch (IOException e) {
            FailUtil.sprintFail(e);
        }
    }

    private void expectReasonForCheckExtensions(File file, List<String> forbiddenExtensions, ValidationResult.Reason reason) throws IOException {
        ValidationResult result =
                PackageValidator.checkForbiddenExtensions(
                        new JarFile(file), forbiddenExtensions);
        assertEquals(String.format("expect reason for forbidden extensions {} in file {}",
                forbiddenExtensions != null ? forbiddenExtensions : "",
                file), reason, result.getReason());
    }

    public void generatePackageFile(String resourcePath, File packageFile) throws IOException {
        InputStream testPack = null;
        OutputStream identOs = null;
        try {
            testPack = getClass().getResourceAsStream(resourcePath);
            if (packageFile.getParentFile().isDirectory()
                    || packageFile.getParentFile().mkdirs()) {

                identOs = new FileOutputStream(packageFile);
                IOUtils.copy(testPack, identOs);
            } else {
                LOGGER.error("[generatePackageFile] failed to generate package file: {}", packageFile);
            }
        } finally {
            IOUtils.closeQuietly(testPack);
            IOUtils.closeQuietly(identOs);
        }
    }

    private WspFilter loadWspFilterFromResource(String path) {
        return WspFilter.adaptWorkspaceFilter(loadWorkspaceFilterFromResource(path));
    }

    private WorkspaceFilter loadWorkspaceFilterFromResource(String path) {
        DefaultWorkspaceFilter filter = new DefaultWorkspaceFilter();
        try {
            filter.load(getClass().getClassLoader().getResourceAsStream(path));
        } catch (Exception e) {
            FailUtil.sprintFail(e);
        }
        return filter;
    }

}
