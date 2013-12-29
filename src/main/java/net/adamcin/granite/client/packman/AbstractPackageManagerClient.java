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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The AbstractPackageManagerClient provides constants and concrete implementations for generic method logic and response
 * handling.
 */
public abstract class AbstractPackageManagerClient implements PackageManagerClient {
    public static final ResponseProgressListener DEFAULT_LISTENER = new DefaultResponseProgressListener();

    public static final String CONSOLE_UI_BASE_PATH = "/crx/packmgr/index.jsp";
    public static final String CONSOLE_UI_LIST_PATH = "/crx/packmgr/list.jsp";
    public static final String CONSOLE_UI_DOWNLOAD_PATH = "/crx/packmgr/download.jsp";
    public static final String SERVICE_BASE_PATH = "/crx/packmgr/service";
    public static final String HTML_SERVICE_PATH = SERVICE_BASE_PATH + "/console.html";
    public static final String JSON_SERVICE_PATH = SERVICE_BASE_PATH + "/exec.json";
    public static final String DEFAULT_BASE_URL = "http://localhost:4502";
    public static final String DEFAULT_USERNAME = "admin";
    public static final String DEFAULT_PASSWORD = "admin";
    public static final int MIN_AUTOSAVE = 1024;

    public static final String MIME_ZIP = "application/zip";

    public static final String KEY_CMD = "cmd";
    public static final String KEY_FORCE = "force";
    public static final String KEY_PACKAGE = "package";
    public static final String KEY_SUCCESS = "success";
    public static final String KEY_MESSAGE = "msg";
    public static final String KEY_PATH = "path";
    public static final String KEY_RECURSIVE = "recursive";
    public static final String KEY_AUTOSAVE = "autosave";
    public static final String KEY_ACHANDLING = "acHandling";
    public static final String KEY_RESULTS = "results";
    public static final String KEY_TOTAL = "total";
    public static final String KEY_GROUP = "group";
    public static final String KEY_NAME = "name";
    public static final String KEY_VERSION = "version";
    public static final String KEY_HAS_SNAPSHOT = "hasSnapshot";
    public static final String KEY_NEEDS_REWRAP = "needsRewrap";
    public static final String KEY_INCLUDE_VERSIONS = "includeVersions";
    public static final String KEY_QUERY = "q";

    public static final String CMD_CONTENTS = "contents";
    public static final String CMD_INSTALL = "install";
    public static final String CMD_UNINSTALL = "uninstall";
    public static final String CMD_UPLOAD = "upload";
    public static final String CMD_BUILD = "build";
    public static final String CMD_REWRAP = "rewrap";
    public static final String CMD_DRY_RUN = "dryrun";
    public static final String CMD_DELETE = "delete";
    public static final String CMD_REPLICATE = "replicate";

    private static final Pattern PATTERN_TITLE = Pattern.compile("^<body><h2>([^<]*)</h2>");
    private static final Pattern PATTERN_LOG = Pattern.compile("^([^<]*<br>)+");
    //private static final Pattern PATTERN_MESSAGE = Pattern.compile("<span class=\"([^\"]*)\"><b>([^<]*)</b>&nbsp;([^<(]*)(\\([^)]*\\))?</span>");
    private static final Pattern PATTERN_MESSAGE = Pattern.compile("<span class=\"([^\"]*)\"><b>([^<]*)</b>&nbsp;([^<(]*)(.*)$");
    private static final Pattern PATTERN_SUCCESS = Pattern.compile("^</div><br>(.*) in (\\d+)ms\\.<br>");

    public static final String LOGIN_PATH = "/crx/j_security_check";
    public static final String LOGIN_PARAM_USERNAME = "j_username";
    public static final String LOGIN_PARAM_PASSWORD = "j_password";
    public static final String LOGIN_PARAM_VALIDATE = "j_validate";
    public static final String LOGIN_VALUE_VALIDATE = "true";
    public static final String LOGIN_PARAM_CHARSET = "_charset_";
    public static final String LOGIN_VALUE_CHARSET = "utf-8";

    public static final String LEGACY_PATH = "/crx/de/login.jsp";
    public static final String LEGACY_PARAM_USERID = "UserId";
    public static final String LEGACY_PARAM_PASSWORD = "Password";
    public static final String LEGACY_PARAM_WORKSPACE = "Workspace";
    public static final String LEGACY_VALUE_WORKSPACE = "crx.default";
    public static final String LEGACY_PARAM_TOKEN = ".token";
    public static final String LEGACY_VALUE_TOKEN = "";

    private String baseUrl = DEFAULT_BASE_URL;
    private long requestTimeout = -1L;
    private long serviceTimeout = -1L;

    public void setBaseUrl(String baseUrl) {
        if (baseUrl == null) {
            throw new NullPointerException("baseUrl");
        }

        String _baseUrl = baseUrl;
        while (_baseUrl.endsWith("/")) {
            _baseUrl = _baseUrl.substring(0, _baseUrl.length() - 1);
        }
        this.baseUrl = _baseUrl;
    }

    public final String getBaseUrl() {
        return this.baseUrl;
    }

    public long getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(long requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    public long getServiceTimeout() {
        return serviceTimeout;
    }

    public void setServiceTimeout(long serviceTimeout) {
        this.serviceTimeout = serviceTimeout;
    }

    protected final String getHtmlUrl() {
        return getBaseUrl() + HTML_SERVICE_PATH;
    }

    protected final String getHtmlUrl(PackId packageId) {
        if (packageId == null) {
            throw new NullPointerException("packageId");
        }
        return getHtmlUrl() + packageId.getInstallationPath() + ".zip";
    }

    public String getLoginUrl() {
        return getJsonUrl();
    }

    protected final String getJsonUrl() {
        return getBaseUrl() + JSON_SERVICE_PATH;
    }

    protected final String getJsonUrl(PackId packageId) {
        if (packageId == null) {
            throw new NullPointerException("packageId");
        }
        return getJsonUrl() + packageId.getInstallationPath() + ".zip";
    }

    protected final String getListUrl() {
        return getBaseUrl() + CONSOLE_UI_LIST_PATH;
    }

    protected final String getDownloadUrl() {
        return getBaseUrl() + CONSOLE_UI_DOWNLOAD_PATH;
    }

    public final String getConsoleUiUrl() {
        return getBaseUrl() + CONSOLE_UI_BASE_PATH;
    }

    public final String getConsoleUiUrl(PackId packageId) {
        if (packageId == null) {
            throw new NullPointerException("packageId");
        }

        return getConsoleUiUrl() + "#" + packageId.getInstallationPath() + ".zip";
    }

    public abstract boolean login(String username, String password) throws IOException;

    /**
     * The CRX PackageManagerServlet does not support GET requests. The only use for GET is to check service
     * availability. If anything other than 401 or 405 is returned, the service should be considered unavailable.
     * @param checkTimeout set to true to enforce a timeout
     * @param timeoutRemaining remaining timeout in milliseconds
     * @return either a throwable or a boolean
     */
    protected abstract Either<? extends Exception, Boolean> checkServiceAvailability(boolean checkTimeout, long timeoutRemaining);

    protected abstract ResponseBuilder getResponseBuilder();

    private static boolean handleStart(String line, ResponseProgressListener listener) {
        if (line.startsWith("<body>")) {
            Matcher titleMatcher = PATTERN_TITLE.matcher(line);
            if (titleMatcher.find()) {
                listener.onStart(titleMatcher.group(1));
                return true;
            }
        }
        return false;
    }

    private static DetailedResponse handleSuccess(String line, List<String> progressErrors) {
        if (line.startsWith("</div>")) {
            String message = "";
            long duration = -1L;
            Matcher successMatcher = PATTERN_SUCCESS.matcher(line);
            if (successMatcher.find()) {
                message = successMatcher.group(1);
                try {
                    duration = Long.valueOf(successMatcher.group(2));
                } catch (Exception e) { }
                return new DetailedResponseImpl(true, message, duration, progressErrors, null);
            }
        }
        return null;
    }

    private static DetailedResponse handleFailure(String line, List<String> failureBuilder, List<String> progressErrors) {
        if (line.startsWith("</pre>")) {
            String msg = failureBuilder != null && !failureBuilder.isEmpty() ? failureBuilder.remove(0) : "";
            return new DetailedResponseImpl(false, msg, -1, progressErrors, failureBuilder);
        } else {
            // assume line is part of stack trace
            failureBuilder.add(line.trim());
        }
        return null;
    }

    private static void handleLogs(String line, ResponseProgressListener listener) {
        if (!line.startsWith("<span")) {
            Matcher logMatcher = PATTERN_LOG.matcher(line);
            if (logMatcher.find()) {
                String logs = logMatcher.group(1);
                for (String log : logs.split("<br>")) {
                    if (!log.isEmpty()) {
                        listener.onLog(log);
                    }
                }
            }
        }
    }

    private static void handleMessage(String line, List<String> progressErrors, ResponseProgressListener listener, BufferedReader reader) {
        Matcher messageMatcher = PATTERN_MESSAGE.matcher(line);
        if (messageMatcher.find()) {
            String action = messageMatcher.group(1);
            String path = messageMatcher.group(3);
            String error = messageMatcher.group(4);
            boolean isEndOfMessage = error.endsWith("</span><br>");

            try {
                while (!isEndOfMessage) {
                    String addtLine = reader.readLine();
                    if (addtLine.endsWith("</span><br>")) {
                        isEndOfMessage = true;
                    }
                    error = error + "\r\n" + addtLine;
                }
            } catch (IOException e) {
                e.printStackTrace(System.err);
            }

            if (error.lastIndexOf("</span><br>") >= 0) {
                error = error.substring(0, error.lastIndexOf("</span><br>"));
            }

            if ("E".equals(action)) {
                progressErrors.add(path + " " + error);
                listener.onError(path.trim(), error.substring(1, error.length()-1));
            } else if (action.length() == 1) {
                listener.onProgress(action, path.trim());
            } else {
                listener.onMessage(action);
            }
        }
    }

    private static boolean handleBeginFailure(String line) {
        return line.endsWith("<span class=\"error\">Error during processing.</span><br><code><pre>");
    }

    protected static DetailedResponse parseDetailedResponse(final int statusCode,
                                                                  final String statusText,
                                                                  final InputStream stream,
                                                                  final String charset,
                                                                  final ResponseProgressListener listener)
        throws IOException {

        if (statusCode == 400) {
            throw new IOException("Command not supported by service");
        } else if (statusCode / 100 != 2) {
            throw new IOException(Integer.toString(statusCode) + " " + statusText);
        } else {
            final ResponseProgressListener _listener = listener == null ? DEFAULT_LISTENER : listener;

            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new InputStreamReader(stream, charset));
                boolean isFailure = false;
                boolean isStarted = false;
                final List<String> failureBuilder = new ArrayList<String>();
                final List<String> progressErrors = new ArrayList<String>();

                String line;
                while ((line = reader.readLine()) != null) {
                    if (isFailure) {

                        // handle failure end line
                        DetailedResponse response = handleFailure(line, failureBuilder, progressErrors);
                        if (response != null) {
                            return response;
                        }

                    } else {

                        if (!isStarted) {
                            // handle title line
                            if (handleStart(line, _listener)) {
                                isStarted = true;
                            }
                        }

                        if (isStarted) {
                            // handle success line
                            DetailedResponse response = handleSuccess(line, progressErrors);
                            if (response != null) {
                                return response;
                            }

                            // handle log statements
                            handleLogs(line, _listener);

                            // handle progress message
                            handleMessage(line, progressErrors, _listener, reader);

                            if (handleBeginFailure(line)) {
                                isFailure = true;
                            }
                        }
                    }
                }

                // throw an exception if neither success or failure was returned
                throw new IOException("Failed to parse service response");

            } finally {
                if (reader != null) {
                    reader.close();
                }
            }
        }
    }

    protected static SimpleResponse parseSimpleResponse(final int statusCode,
                                                       final String statusText,
                                                       final InputStream stream,
                                                       final String charset)
            throws IOException {
        if (statusCode == 400) {
            throw new IOException("Command not supported by service");
        } else if (statusCode / 100 != 2) {
            throw new IOException(Integer.toString(statusCode) + " " + statusText);
        } else {
            try {
                JSONTokener tokener = new JSONTokener(new InputStreamReader(stream, charset));
                final JSONObject json = new JSONObject(tokener);

                final boolean success = json.has(KEY_SUCCESS) && json.getBoolean(KEY_SUCCESS);
                final String message = json.has(KEY_MESSAGE) ? json.getString(KEY_MESSAGE) : "";
                final String path = json.has(KEY_PATH) ? json.getString(KEY_PATH) : "";

                return new SimpleResponseImpl(success, message, path);
            } catch (JSONException e) {
                throw new IOException("Exception encountered while parsing response.", e);
            }
        }
    }

    protected static ListResponse parseListResponse(final int statusCode,
                                                    final String statusText,
                                                    final InputStream stream,
                                                    final String charset)
        throws IOException {

        if (statusCode == 200) {
            try {
                ArrayList<ListResult> results = new ArrayList<ListResult>();
                JSONTokener tokener = new JSONTokener(new InputStreamReader(stream, charset));

                final JSONObject resultsObj = new JSONObject(tokener);
                final JSONArray resultsArr = resultsObj.getJSONArray(KEY_RESULTS);

                final int total = resultsObj.getInt(KEY_TOTAL);

                for (int i = 0; i < resultsArr.length(); i++) {
                    JSONObject result = resultsArr.getJSONObject(i);

                    results.add(ListResultImpl.fromJSONObject(result));
                }

                return new ListResponseImpl(results, total);
            } catch (JSONException e) {
                throw new IOException("Exception encountered while parsing response.", e);
            }
        } else {
            throw new IOException("Invalid status code: " + statusCode);
        }
    }

    protected static DownloadResponse parseDownloadResponse(final int statusCode,
                                                    final String statusText,
                                                    final InputStream stream,
                                                    final File outputFile)
            throws IOException {

        if (outputFile.isDirectory()) {
            throw new IOException("Cannot download to a directory. outputFile=" + outputFile.getAbsolutePath());
        }

        if (statusCode == 200) {
            InputStream in = new BufferedInputStream(stream);
            OutputStream out = null;
            try {

                out = new BufferedOutputStream(new FileOutputStream(outputFile));

                byte[] buffer = new byte[16384];
                long totalLength = 0;
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                    totalLength += len;
                }

                return new DownloadResponseImpl(totalLength, outputFile);
            } finally {
                try { in.close(); } catch (IOException ignored) {}
                if (out != null) {
                    try { out.close(); } catch (IOException ignored) {}
                }
            }
        } else {
            throw new IOException("Invalid status code: " + statusCode);
        }
    }

    protected static abstract class Either<T, U> {
        abstract boolean isLeft();
        T getLeft() { return null; }
        U getRight() { return null; }
    }

    static final class Left<T, U> extends Either<T, U> {
        final T left;

        private Left(final T left) {
            if (left == null) {
                throw new NullPointerException("left");
            }
            this.left = left;
        }

        @Override boolean isLeft() { return true; }
        @Override T getLeft() { return left; }
    }

    static final class Right<T, U> extends Either<T, U> {
        final U right;

        private Right(final U right) {
            if (right == null) {
                throw new NullPointerException("right");
            }
            this.right = right;
        }

        @Override boolean isLeft() { return false; }
        @Override U getRight() { return right; }
    }

    protected static <T, U> Either<T, U> left(T left, Class<U> right) {
        return new Left<T, U>(left);
    }

    protected static <T, U> Either<T, U> right(Class<T> left, U right) {
        return new Right<T, U>(right);
    }

    static class DetailedResponseImpl implements DetailedResponse {
        final boolean success;
        final String message;
        final long duration;
        final List<String> progressErrors;
        final List<String> stackTrace;

        DetailedResponseImpl(boolean success, String message, long duration, List<String> progressErrors) {
            this(success, message, duration, progressErrors, null);
        }

        DetailedResponseImpl(boolean success, String message, long duration, List<String> progressErrors, List<String> stackTrace) {
            this.success = success;
            this.message = message;
            this.duration = duration;
            List<String> _progressErrors = progressErrors == null ? new ArrayList<String>() : progressErrors;
            this.progressErrors = Collections.unmodifiableList(_progressErrors);
            List<String> _stackTrace = stackTrace == null ? new ArrayList<String>() : stackTrace;
            this.stackTrace = Collections.unmodifiableList(_stackTrace);
        }

        public long getDuration() {
            return duration;
        }

        public boolean hasErrors() {
            return !success || (progressErrors != null && !progressErrors.isEmpty());
        }

        public List<String> getProgressErrors() {
            return progressErrors;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public List<String> getStackTrace() {
            return this.stackTrace;
        }

        public String toString() {
            return "{success:" + success +
                    ", msg:\"" + message +
                    "\", duration:\"" + duration +
                    "\", hasErrors:" + !progressErrors.isEmpty() + "}";
        }
    }

    static class SimpleResponseImpl implements SimpleResponse {
        final boolean success;
        final String message;
        final String path;

        SimpleResponseImpl(boolean success, String message, String path) {
            this.success = success;
            this.message = message;
            this.path = path == null ? "" : path;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public String getPath() {
            return path;
        }

        public String toString() {
            return "{success:" + success + ", msg:\"" + message + "\", path:\"" + path + "\"}";
        }
    }

    static class ListResponseImpl implements ListResponse {
        final List<ListResult> results;
        final int total;

        ListResponseImpl(List<ListResult> results, int total) {
            this.results = results;
            this.total = total;
        }

        public List<ListResult> getResults() {
            return Collections.unmodifiableList(results);
        }

        public int getTotal() {
            return total;
        }

        public boolean isSuccess() {
            return true;
        }

        public String getMessage() {
            throw new UnsupportedOperationException("getMessage not implemented");
        }
    }

    static class ListResultImpl implements ListResult {
        private final PackId packId;
        private final boolean hasSnapshot;
        private final boolean needsRewrap;

        ListResultImpl(PackId packId, boolean hasSnapshot, boolean needsRewrap) {
            this.packId = packId;
            this.hasSnapshot = hasSnapshot;
            this.needsRewrap = needsRewrap;
        }

        public PackId getPackId() { return this.packId; }
        public boolean isHasSnapshot() { return this.hasSnapshot; }
        public boolean isNeedsRewrap() { return this.needsRewrap; }

        static ListResultImpl fromJSONObject(JSONObject result) throws JSONException {
            PackId packId = PackId.createPackId(
                    result.getString(KEY_GROUP),
                    result.getString(KEY_NAME),
                    result.optString(KEY_VERSION)
            );

            boolean hasSnapshot = result.has(KEY_HAS_SNAPSHOT) && result.getBoolean(KEY_HAS_SNAPSHOT);
            boolean needsRewrap = result.has(KEY_NEEDS_REWRAP) && result.getBoolean(KEY_NEEDS_REWRAP);
            return new ListResultImpl(packId, hasSnapshot, needsRewrap);
        }
    }

    static class DownloadResponseImpl implements DownloadResponse {
        private final Long length;
        private final File content;

        DownloadResponseImpl(Long length, File content) {
            this.length = length;
            this.content = content;
        }

        public Long getLength() {
            return length;
        }

        public File getContent() {
            return content;
        }
    }


    protected static abstract class ResponseBuilder {
        protected abstract ResponseBuilder forPackId(PackId packId);
        protected abstract ResponseBuilder withParam(String name, String value);
        protected abstract ResponseBuilder withParam(String name, boolean value);
        protected abstract ResponseBuilder withParam(String name, int value);
        protected abstract ResponseBuilder withParam(String name, File value, String mimeType) throws IOException;
        protected abstract SimpleResponse getSimpleResponse() throws Exception;
        protected abstract ListResponse getListResponse() throws Exception;
        protected abstract DetailedResponse getDetailedResponse(ResponseProgressListener listener) throws Exception;
        protected abstract DownloadResponse getDownloadResponse(File outputFile) throws Exception;
    }

    //-------------------------------------------------------------------------
    // PackageManagerClient method implementations
    //-------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    public PackId identify(File file) throws IOException {
        return PackId.identifyPackage(file);
    }

    /**
     * {@inheritDoc}
     */
    public final void waitForService() throws Exception {
        boolean checkTimeout = serviceTimeout >= 0L;
        int tries = 0;
        final long stop = System.currentTimeMillis() + serviceTimeout;
        Either<? extends Exception, Boolean> resp;
        do {
            if (checkTimeout && stop <= System.currentTimeMillis()) {
                throw new IOException("Service timeout exceeded.");
            }
            Thread.sleep(Math.min(5, tries) * 1000L);
            resp = checkServiceAvailability(checkTimeout, stop - System.currentTimeMillis());
            if (resp.isLeft()) {
                throw resp.getLeft();
            }
            tries++;
        } while (!resp.isLeft() && !resp.getRight());
    }

    /**
     * {@inheritDoc}
     */
    public final boolean existsOnServer(PackId packageId) throws Exception {
        ListResponse response = list(packageId, false);
        return response.getResults().size() > 0
                && response.getResults().get(0).getPackId().equals(packageId);
    }

    /**
     * {@inheritDoc}
     */
    public final ListResponse list() throws Exception {
        return getResponseBuilder().getListResponse();
    }

    /**
     * {@inheritDoc}
     */
    public final ListResponse list(String query) throws Exception {
        return getResponseBuilder().withParam(KEY_QUERY, query != null ? query : "").getListResponse();
    }

    /**
     * {@inheritDoc}
     */
    public final ListResponse list(PackId packageId, boolean includeVersions) throws Exception {
        if (packageId == null) {
            throw new NullPointerException("packageId");
        }
        return getResponseBuilder().forPackId(packageId)
                .withParam(KEY_INCLUDE_VERSIONS, Boolean.toString(includeVersions)).getListResponse();
    }

    /**
     * {@inheritDoc}
     */
    public final SimpleResponse upload(File file, boolean force, PackId packageId) throws Exception {
        if (file == null) {
            throw new NullPointerException("file");
        }
        return getResponseBuilder().forPackId(packageId == null ? identify(file) : packageId)
                .withParam(KEY_CMD, CMD_UPLOAD)
                .withParam(KEY_PACKAGE, file, MIME_ZIP)
                .withParam(KEY_FORCE, force)
                .getSimpleResponse();
    }

    /**
     * {@inheritDoc}
     */
    public final DownloadResponse download(PackId packageId, File toFile) throws Exception {
        if (packageId == null) {
            throw new NullPointerException("packageId");
        }
        if (toFile == null) {
            throw new NullPointerException("toFile");
        }
        return getResponseBuilder().forPackId(packageId).getDownloadResponse(toFile);
    }

    /**
     * {@inheritDoc}
     */
    public final DownloadResponse downloadToDirectory(PackId packageId, File toDirectory) throws Exception {
        if (packageId == null) {
            throw new NullPointerException("packageId");
        }
        if (toDirectory == null) {
            throw new NullPointerException("toDirectory");
        }

        File toFile = new File(toDirectory, packageId.getInstallationPath().substring(1) + ".zip");
        if (toFile.getParentFile().isDirectory() || toFile.getParentFile().mkdirs()) {
            return download(packageId, toFile);
        } else {
            throw new IOException("Failed to create path: " + toFile.getParentFile().getAbsolutePath());
        }
    }

    /**
     * {@inheritDoc}
     */
    public final SimpleResponse delete(PackId packageId) throws Exception {
        if (packageId == null) {
            throw new NullPointerException("packageId");
        }
        return getResponseBuilder().forPackId(packageId)
                .withParam(KEY_CMD, CMD_DELETE)
                .getSimpleResponse();
    }

    /**
     * {@inheritDoc}
     */
    public final SimpleResponse replicate(PackId packageId) throws Exception {
        if (packageId == null) {
            throw new NullPointerException("packageId");
        }
        return getResponseBuilder().forPackId(packageId)
                .withParam(KEY_CMD, CMD_REPLICATE)
                .getSimpleResponse();
    }

    /**
     * {@inheritDoc}
     */
    public final DetailedResponse contents(PackId packageId) throws Exception {
        return this.contents(packageId, null);
    }

    /**
     * {@inheritDoc}
     */
    public final DetailedResponse contents(PackId packageId, ResponseProgressListener listener) throws Exception {
        if (packageId == null) {
            throw new NullPointerException("packageId");
        }
        return getResponseBuilder().forPackId(packageId)
                .withParam(KEY_CMD, CMD_CONTENTS)
                .getDetailedResponse(listener);
    }

    /**
     * {@inheritDoc}
     */
    public final DetailedResponse install(PackId packageId,
                                          boolean recursive,
                                          int autosave,
                                          ACHandling acHandling) throws Exception {
        return this.install(packageId, recursive, autosave, acHandling, null);
    }

    /**
     * {@inheritDoc}
     */
    public final DetailedResponse install(PackId packageId,
                                          boolean recursive,
                                          int autosave,
                                          ACHandling acHandling,
                                          ResponseProgressListener listener) throws Exception {
        if (packageId == null) {
            throw new NullPointerException("packageId");
        }

        ResponseBuilder rb = getResponseBuilder().forPackId(packageId)
                .withParam(KEY_CMD, CMD_INSTALL)
                .withParam(KEY_RECURSIVE, recursive)
                .withParam(KEY_AUTOSAVE, Math.max(autosave, MIN_AUTOSAVE));

        if (acHandling != null) {
            rb.withParam(KEY_ACHANDLING, acHandling.name().toLowerCase());
        }

        return rb.getDetailedResponse(listener);
    }

    /**
     * {@inheritDoc}
     */
    public final DetailedResponse dryRun(PackId packageId) throws Exception {
        return this.dryRun(packageId, null);
    }

    /**
     * {@inheritDoc}
     */
    public final DetailedResponse dryRun(PackId packageId, ResponseProgressListener listener) throws Exception {
        if (packageId == null) {
            throw new NullPointerException("packageId");
        }

        return getResponseBuilder().forPackId(packageId)
                .withParam(KEY_CMD, CMD_DRY_RUN)
                .getDetailedResponse(listener);
    }

    /**
     * {@inheritDoc}
     */
    public final DetailedResponse build(PackId packageId) throws Exception {
        return this.build(packageId, null);
    }

    /**
     * {@inheritDoc}
     */
    public final DetailedResponse build(PackId packageId, ResponseProgressListener listener) throws Exception {
        if (packageId == null) {
            throw new NullPointerException("packageId");
        }

        return getResponseBuilder().forPackId(packageId)
                .withParam(KEY_CMD, CMD_BUILD)
                .getDetailedResponse(listener);
    }

    /**
     * {@inheritDoc}
     */
    public final DetailedResponse rewrap(PackId packageId) throws Exception {
        return this.rewrap(packageId, null);
    }

    /**
     * {@inheritDoc}
     */
    public final DetailedResponse rewrap(PackId packageId, ResponseProgressListener listener) throws Exception {
        if (packageId == null) {
            throw new NullPointerException("packageId");
        }

        return getResponseBuilder().forPackId(packageId)
                .withParam(KEY_CMD, CMD_REWRAP)
                .getDetailedResponse(listener);
    }

    /**
     * {@inheritDoc}
     */
    public final DetailedResponse uninstall(PackId packageId) throws Exception {
        return this.uninstall(packageId, null);
    }

    /**
     * {@inheritDoc}
     */
    public final DetailedResponse uninstall(PackId packageId, ResponseProgressListener listener) throws Exception {
        if (packageId == null) {
            throw new NullPointerException("packageId");
        }

        return getResponseBuilder().forPackId(packageId)
                .withParam(KEY_CMD, CMD_UNINSTALL)
                .getDetailedResponse(listener);
    }
}
