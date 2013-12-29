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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class AbstractPackageManagerClientTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractPackageManagerClientTest.class);

    final String INSTALL_SUCCESS = "/install_success.html";
    final String INSTALL_SUCCESS_WITH_ERRORS = "/install_success_with_errors.html";
    final String INSTALL_FAILURE = "/install_failure.html";

    @Test
    public void testParseDetailedResponse() {
        TestBody.test(new ResponseTestBody(INSTALL_SUCCESS) {
            @Override protected void execute() throws Exception {
                DetailedResponse response = parse();
                assertTrue(INSTALL_SUCCESS + " parses as success", response.isSuccess());
                assertEquals(INSTALL_SUCCESS + " message is correct", "Package installed", response.getMessage());
                assertEquals(INSTALL_SUCCESS + " duration is correct", 101L, response.getDuration());
                assertTrue(INSTALL_SUCCESS + " progressErrors is empty", response.getProgressErrors().isEmpty());
            }
        });

        TestBody.test(new ResponseTestBody(INSTALL_SUCCESS_WITH_ERRORS) {
            @Override protected void execute() throws Exception {
                DetailedResponse response = parse();
                assertTrue(INSTALL_SUCCESS_WITH_ERRORS + " parses as success", response.isSuccess());
                assertEquals(INSTALL_SUCCESS_WITH_ERRORS + " message is correct", "Package installed", response.getMessage());
                assertEquals(INSTALL_SUCCESS_WITH_ERRORS + " duration is correct", 37L, response.getDuration());
                assertFalse(INSTALL_SUCCESS_WITH_ERRORS + " progressErrors is not empty", response.getProgressErrors().isEmpty());
            }
        });

        TestBody.test(new ResponseTestBody(INSTALL_FAILURE) {
            @Override protected void execute() throws Exception {
                DetailedResponse response = parse();
                assertFalse(INSTALL_FAILURE + " parses as failure", response.isSuccess());
                assertTrue(INSTALL_FAILURE + " message starts with org.apache.jackrabbit.core.data.DataStoreException",
                        response.getMessage().startsWith("org.apache.jackrabbit.core.data.DataStoreException"));
                assertEquals(INSTALL_FAILURE + " duration is correct", -1L, response.getDuration());
                assertTrue(INSTALL_FAILURE + " progressErrors is empty", response.getProgressErrors().isEmpty());
            }
        });
    }


    static abstract class ResponseTestBody extends TestBody {
        final InputStream stream;
        final TestListener listener = new TestListener();

        public ResponseTestBody() {
            super();
            stream = null;
        }

        ResponseTestBody(String resourcePath) {
            super();
            this.stream = getClass().getResourceAsStream(resourcePath);
        }

        protected DetailedResponse parse() throws IOException {
            return AbstractPackageManagerClient.parseDetailedResponse(200, "Ok", stream, "UTF-8", listener);
        }

        @Override
        protected void cleanUp() {
            IOUtils.closeQuietly(stream);
        }
    }

    static class TestListener implements ResponseProgressListener {
        String title = null;
        List<String> logs = new ArrayList<String>();
        List<String> messages = new ArrayList<String>();
        // path x error
        Map<String, String> errors = new HashMap<String, String>();
        // path x action
        Map<String, String> progress = new HashMap<String, String>();

        public void onStart(String title) {
            LOGGER.debug("[TestListener#onStart] title={}", title);
            this.title = title;
        }

        public void onLog(String message) {
            LOGGER.debug("[TestListener#onLog] message={}", message);
            logs.add(message);
        }

        public void onMessage(String message) {
            LOGGER.debug("[TestListener#onMessage] message={}", message);
            messages.add(message);
        }

        public void onProgress(String action, String path) {
            LOGGER.debug("[TestListener#onProgress] action={}, path={}", action, path);
            progress.put(path, action);
        }

        public void onError(String path, String error) {
            LOGGER.info("[TestListener#onError] path={}, error={}", path, error);
            errors.put(path, error);
        }
    }
}
