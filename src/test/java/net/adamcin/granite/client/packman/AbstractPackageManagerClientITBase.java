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

import net.adamcin.commons.testing.junit.TestBody;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.junit.Assert.*;

public abstract class AbstractPackageManagerClientITBase {
    public final Logger LOGGER = LoggerFactory.getLogger(getClass());

    protected abstract AbstractPackageManagerClient getClientImplementation();

    public void generateTestPackage(File packageFile) throws IOException {
        InputStream testPack = null;
        OutputStream identOs = null;
        try {
            testPack = getClass().getResourceAsStream("/test-packmgr-client-1.0.zip");
            if (packageFile.getParentFile().isDirectory()
                    || packageFile.getParentFile().mkdirs()) {

                identOs = new FileOutputStream(packageFile);
                IOUtils.copy(testPack, identOs);
            } else {
                LOGGER.error("[generateTestPackage] failed to generate test package: {}", packageFile);
            }
        } finally {
            IOUtils.closeQuietly(testPack);
            IOUtils.closeQuietly(identOs);
        }
    }

    @Test
    public void testLoginUsernamePassword() {

        TestBody.test(new PackmgrClientTestBody() {
            @Override
            protected void execute() throws Exception {
                assertTrue("Login using default credentials", client.login("admin", "admin"));

            }
        });
    }

    @Test
    public void testLoginSigner() {
        TestBody.test(new PackmgrClientTestBody() {
            @Override protected void execute() throws Exception {
            }
        });
    }

    @Test
    public void testIdentifyPackage() {
        TestBody.test(new PackmgrClientTestBody() {
            @Override protected void execute() throws Exception {
                client.login("admin", "admin");
                File nonExist = new File("target/non-exist-package.zip");
                boolean ioExceptionThrown = false;

                try {
                    client.identify(nonExist);
                } catch (IOException e) {
                    ioExceptionThrown = true;
                }

                assertTrue("identify throws correct exception for non-existent file", ioExceptionThrown);

                File identifiable = new File("target/identifiable-package.zip");
                generateTestPackage(identifiable);

                PackId id = client.identify(identifiable);

                assertNotNull("id should not be null", id);

                assertEquals("group should be test-packmgr", "test-packmgr", id.getGroup());
                assertEquals("name should be test-packmgr-client", "test-packmgr-client", id.getName());
                assertEquals("version should be 1.0", "1.0", id.getVersion());
                assertEquals("installationPath should be /etc/packages/test-packmgr/test-packmgr-client-1.0", "/etc/packages/test-packmgr/test-packmgr-client-1.0", id.getInstallationPath());
            }
        });
    }

    @Test
    public void testWaitForService() {
        TestBody.test(new PackmgrClientTestBody() {
            @Override protected void execute() throws Exception {
                client.login("admin", "admin");
                client.setServiceTimeout(5000L);
                boolean ex = false;
                try {
                    client.waitForService();
                } catch (Exception e) {
                    LOGGER.debug("Exception: " + e.getMessage());
                    ex = true;
                }

                assertFalse("Exception should not be thrown for baseUrl: " + client.getBaseUrl(), ex);

                ex = false;
                client.setBaseUrl("http://www.google.com");

                long stop = System.currentTimeMillis() + 5000L;
                try {
                    client.waitForService();
                } catch (Exception e) {
                    LOGGER.debug("Exception: " + e.getMessage());
                    ex = true;
                    assertTrue("Waited long enough", System.currentTimeMillis() > stop);
                }

                assertTrue("Exception should be thrown for baseUrl: " + client.getBaseUrl(), ex);
            }
        });
    }

    @Test
    public void testExistsOnServer() {
        TestBody.test(new PackmgrClientTestBody() {
            @Override protected void execute() throws Exception {
                client.login("admin", "admin");
                File file = new File("target/test-packmgr-client-1.0.zip");

                generateTestPackage(file);

                PackId id = client.identify(file);
                if (client.existsOnServer(id)) {
                    LOGGER.info("deleting: {}", client.delete(id));
                }

                assertFalse("package should not exist on server", client.existsOnServer(id));

                LOGGER.info("uploading: {}", client.upload(file, true, id));

                assertTrue("package should exist on server", client.existsOnServer(id));

                LOGGER.info("deleting: {}", client.delete(id));

                assertFalse("package should not exist on server", client.existsOnServer(id));
            }
        });
    }

    abstract class PackmgrClientTestBody extends TestBody {
        AbstractPackageManagerClient client = getClientImplementation();
    }
}
