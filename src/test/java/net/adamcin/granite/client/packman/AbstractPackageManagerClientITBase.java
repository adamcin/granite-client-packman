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
import net.adamcin.commons.testing.junit.TestBody;
import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.vault.fs.api.PathFilterSet;
import org.apache.jackrabbit.vault.fs.api.WorkspaceFilter;
import org.apache.jackrabbit.vault.packaging.PackageManager;
import org.apache.jackrabbit.vault.packaging.PackagingService;
import org.apache.jackrabbit.vault.packaging.VaultPackage;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import static org.junit.Assert.*;

public abstract class AbstractPackageManagerClientITBase {
    public final Logger LOGGER = LoggerFactory.getLogger(getClass());
    public final ResponseProgressListener LISTENER = new LoggingListener(LOGGER, LoggingListener.Level.DEBUG);

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

    @Test
    public void testCreate() {
        TestBody.test(new PackmgrClientTestBody() {
            @Override protected void execute() throws Exception {
                client.login("admin", "admin");

                PackId id = PackId.createPackId("test-packmgr", "test-create-package", "1.0");
                if (client.existsOnServer(id)) {
                    LOGGER.info("deleting: {}", client.delete(id));
                }

                assertFalse("package should not exist on server", client.existsOnServer(id));

                LOGGER.info("creating: {}", client.create(id));

                assertTrue("package should exist on server", client.existsOnServer(id));

                LOGGER.info("deleting: {}", client.delete(id));

                assertFalse("package should not exist on server", client.existsOnServer(id));
            }
        });
    }

    @Test
    public void testMove() {
        TestBody.test(new PackmgrClientTestBody() {
            @Override protected void execute() throws Exception {
                client.login("admin", "admin");

                PackId id = PackId.createPackId("test-packmgr", "test-move", "1.0");
                PackId moveToId = PackId.createPackId("test-packmgr", "move-to", "1.0");
                if (client.existsOnServer(id)) {
                    LOGGER.info("deleting: {}", client.delete(id));
                }
                if (client.existsOnServer(moveToId)) {
                    LOGGER.info("deleting: {}", client.delete(moveToId));
                }

                assertFalse("package should not exist on server", client.existsOnServer(id));
                assertFalse("move-to-location should not exist on server", client.existsOnServer(moveToId));

                LOGGER.info("creating: {}", client.create(id));

                assertTrue("package should exist on server", client.existsOnServer(id));
                assertFalse("move-to-location should not exist on server", client.existsOnServer(moveToId));

                LOGGER.info("moving: {}", client.move(id, moveToId));

                assertFalse("package should not exist on server", client.existsOnServer(id));
                assertTrue("move-to-location SHOULD exist on server", client.existsOnServer(moveToId));

                LOGGER.info("deleting: {}", client.delete(moveToId));
            }
        });
    }

    @Test
    public void testUpdateFilter() {
        TestBody.test(new PackmgrClientTestBody() {
            @Override protected void execute() throws Exception {
                client.login("admin", "admin");

                PackId id = PackId.createPackId("test-packmgr", "test-update-filter", "1.0");
                if (client.existsOnServer(id)) {
                    LOGGER.info("deleting: {}", client.delete(id));
                }

                assertFalse("package should not exist on server", client.existsOnServer(id));

                LOGGER.info("creating: {}", client.create(id));

                assertTrue("package should exist on server", client.existsOnServer(id));

                String theOneRoot = "/tmp/test-update-filter";
                String theOnePattern = theOneRoot + "(/.*)?";

                WspFilter.Root origRoot =new WspFilter.Root(theOneRoot,
                        new WspFilter.Rule(true, theOnePattern));
                WspFilter origWSPFilter = new WspFilter(origRoot);

                LOGGER.info("updating filter: {}", client.updateFilter(id, origWSPFilter));

                LOGGER.info("building: {}", client.build(id, LISTENER));

                File downloaded = new File("target/test-update-filter-1.0.zip");

                LOGGER.info("downloading: {}", client.download(id, downloaded));

                VaultPackage pack = null;
                try {
                    PackageManager manager = PackagingService.getPackageManager();
                    pack = manager.open(downloaded, true);

                    assertTrue("package should be valid after download", pack.isValid());

                    WorkspaceFilter wspFilter = pack.getMetaInf().getFilter();

                    List<PathFilterSet> filterSets = wspFilter.getFilterSets();

                    assertFalse("package filter sets should not be empty", filterSets.isEmpty());

                    PathFilterSet filterSet = filterSets.get(0);
                    WspFilter.Root archiveFilterRoot = WspFilter.adaptFilterSet(filterSet);

                    assertEquals("filterSet root should be the same as before.", theOneRoot, archiveFilterRoot.getPath());

                    assertFalse("package filter set rules should not be empty", archiveFilterRoot.getRules().isEmpty());

                    assertTrue("package filter rule should be an include",
                            archiveFilterRoot.getRules().get(0).isInclude());

                    WspFilter.Rule archiveRule = archiveFilterRoot.getRules().get(0);

                    assertEquals("filter pattern should be the same as before.",
                            theOnePattern, archiveRule.getPattern());

                } catch (IOException e) {
                    FailUtil.sprintFail(e);
                } finally {
                    if (pack != null) {
                        pack.close();
                    }
                }

                LOGGER.info("deleting: {}", client.delete(id));

                assertFalse("package should not exist on server", client.existsOnServer(id));
            }
        });
    }

    abstract class PackmgrClientTestBody extends TestBody {
        AbstractPackageManagerClient client = getClientImplementation();
    }
}
