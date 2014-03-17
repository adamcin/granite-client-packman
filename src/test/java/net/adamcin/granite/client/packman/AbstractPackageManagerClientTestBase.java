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

public abstract class AbstractPackageManagerClientTestBase {
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
    public void testIdentifyPackage() {
        TestBody.test(new PackmgrClientTestBody() {
            @Override protected void execute() throws Exception {
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
    public void testGetHtmlUrl() {
    	TestBody.test(new PackmgrClientTestBody() {
			@Override protected void execute() throws Exception {
				PackId goodId = PackId.createPackId("group", "name", "1.0");
				assertEquals("packIds with no special chars should work", 
						client.getHtmlUrl() + "/etc/packages/group/name-1.0.zip",
						client.getHtmlUrl(goodId));
				
				// escape space (  %20) and percent (% %25)
				PackId illegalId = PackId.createPackId("percents%", "and spaces", "1.0");
				assertEquals("packIds with illegal path chars should work", 
						client.getHtmlUrl() + "/etc/packages/percents%25/and%20spaces-1.0.zip",
						client.getHtmlUrl(illegalId));
				
				// escape question mark (? %3F) and octothorpe (# %23), but not ampersand (& %26)
				PackId reservedId = PackId.createPackId("amps&", "quests?octothorpes#", "1.0");
				assertEquals("packIds with reserved URL chars should work", 
						client.getHtmlUrl() + "/etc/packages/amps&/quests%3Foctothorpes%23-1.0.zip",
						client.getHtmlUrl(reservedId));
				
				// escape non-ascii?
				/*
				PackId utf8Id = PackId.createPackId("eighty\u20ACs", "name", "1.0");
				assertEquals("packIds with UTF-8 should have ascii-only urls", 
						client.getHtmlUrl() + "/etc/packages/eighty%E2%82%ACs/name.zip",
						client.getHtmlUrl(utf8Id));
						*/
			}
    	});
    }
    
    @Test
    public void testGetJsonUrl() {
    	TestBody.test(new PackmgrClientTestBody() {
			@Override protected void execute() throws Exception {
				PackId goodId = PackId.createPackId("group", "name", "1.0");
				assertEquals("packIds with no special chars should work", 
						client.getJsonUrl() + "/etc/packages/group/name-1.0.zip",
						client.getJsonUrl(goodId));
				
				// escape space (  %20) and percent (% %25)
				PackId illegalId = PackId.createPackId("percents%", "and spaces", "1.0");
				assertEquals("packIds with illegal path chars should work", 
						client.getJsonUrl() + "/etc/packages/percents%25/and%20spaces-1.0.zip",
						client.getJsonUrl(illegalId));
				
				// escape question mark (? %3F) and octothorpe (# %23), but not ampersand (& %26)
				PackId reservedId = PackId.createPackId("amps&", "quests?octothorpes#", "1.0");
				assertEquals("packIds with reserved URL chars should work", 
						client.getJsonUrl() + "/etc/packages/amps&/quests%3Foctothorpes%23-1.0.zip",
						client.getJsonUrl(reservedId));
			
			}
    	});
    }
    
    @Test
    public void testGetConsoleUiUrl() {
    	TestBody.test(new PackmgrClientTestBody() {
			@Override protected void execute() throws Exception {
				PackId goodId = PackId.createPackId("group", "name", "1.0");
				assertEquals("packIds with no special chars should work", 
						client.getConsoleUiUrl() + "#/etc/packages/group/name-1.0.zip",
						client.getConsoleUiUrl(goodId));
				
				// escape space (  %20) and percent (% %25)
				PackId illegalId = PackId.createPackId("percents%", "and spaces", "1.0");
				assertEquals("packIds with illegal path chars should work", 
						client.getConsoleUiUrl() + "#/etc/packages/percents%25/and%20spaces-1.0.zip",
						client.getConsoleUiUrl(illegalId));
				
				// escape octothorpe (# %23), but do not escape question mark (? %3F) or ampersand (& %26) 
				PackId reservedId = PackId.createPackId("amps&", "quests?octothorpes#", "1.0");
				assertEquals("packIds with reserved URL chars should work", 
						client.getConsoleUiUrl() + "#/etc/packages/amps&/quests?octothorpes%23-1.0.zip",
						client.getConsoleUiUrl(reservedId));
			
			}
    	});
    }
    
    @Test
    public void testGetListUrl() {
    	TestBody.test(new PackmgrClientTestBody() {
			@Override protected void execute() throws Exception {
				String noSlash = "http://localhost:4502";
				assertEquals("don't screw up the URL handling, you klutz. [default]", 
						noSlash + AbstractPackageManagerClient.CONSOLE_UI_LIST_PATH,
						client.getListUrl());
			
				client.setBaseUrl(noSlash);
				assertEquals("don't screw up the URL handling, you klutz. [noSlash]", 
						noSlash + AbstractPackageManagerClient.CONSOLE_UI_LIST_PATH,
						client.getListUrl());
			
				String slash = noSlash + "/";
				client.setBaseUrl(slash);
				assertEquals("don't screw up the URL handling, you klutz. [slash]", 
						noSlash + AbstractPackageManagerClient.CONSOLE_UI_LIST_PATH,
						client.getListUrl());
				
				String withContextNoSlash = noSlash + "/context";
				client.setBaseUrl(withContextNoSlash);
				assertEquals("don't screw up the URL handling, you klutz. [withContextNoSlash]", 
						withContextNoSlash + AbstractPackageManagerClient.CONSOLE_UI_LIST_PATH,
						client.getListUrl());
				
				String withContextSlash = withContextNoSlash + "/";
				client.setBaseUrl(withContextSlash);
				assertEquals("don't screw up the URL handling, you klutz. [withContextSlash]", 
						withContextNoSlash + AbstractPackageManagerClient.CONSOLE_UI_LIST_PATH,
						client.getListUrl());
			}
    	});
    }
    
    @Test
    public void testGetDownloadUrl() {
    	TestBody.test(new PackmgrClientTestBody() {
			@Override protected void execute() throws Exception {
				String noSlash = "http://localhost:4502";
				assertEquals("don't screw up the URL handling, you klutz. [default]", 
						noSlash + AbstractPackageManagerClient.CONSOLE_UI_DOWNLOAD_PATH,
						client.getDownloadUrl());
			
				client.setBaseUrl(noSlash);
				assertEquals("don't screw up the URL handling, you klutz. [noSlash]", 
						noSlash + AbstractPackageManagerClient.CONSOLE_UI_DOWNLOAD_PATH,
						client.getDownloadUrl());
			
				String slash = noSlash + "/";
				client.setBaseUrl(slash);
				assertEquals("don't screw up the URL handling, you klutz. [slash]", 
						noSlash + AbstractPackageManagerClient.CONSOLE_UI_DOWNLOAD_PATH,
						client.getDownloadUrl());
				
				String withContextNoSlash = noSlash + "/context";
				client.setBaseUrl(withContextNoSlash);
				assertEquals("don't screw up the URL handling, you klutz. [withContextNoSlash]", 
						withContextNoSlash + AbstractPackageManagerClient.CONSOLE_UI_DOWNLOAD_PATH,
						client.getDownloadUrl());
				
				String withContextSlash = withContextNoSlash + "/";
				client.setBaseUrl(withContextSlash);
				assertEquals("don't screw up the URL handling, you klutz. [withContextSlash]", 
						withContextNoSlash + AbstractPackageManagerClient.CONSOLE_UI_DOWNLOAD_PATH,
						client.getDownloadUrl());
			}
    	});
    }

    abstract class PackmgrClientTestBody extends TestBody {
        AbstractPackageManagerClient client = getClientImplementation();
    }
}
